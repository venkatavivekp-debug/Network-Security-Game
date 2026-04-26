package backend.security.ratelimit;

import backend.audit.AuditEventType;
import backend.audit.AuditService;
import backend.util.RequestContextUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Verifies the rate-limit filter's externally visible behavior:
 * a clean 429 body, a {@code Retry-After} header, and an audit event for SOC.
 */
class RateLimitFilterTest {

    @Test
    void exhaustingLoginBucketShouldReturn429AndAuditBlocked() throws Exception {
        RateLimiterService limiter = new RateLimiterService(new InMemoryRateLimiterBackend());
        AuditService audit = mock(AuditService.class);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RateLimitFilter filter = new RateLimitFilter(limiter, new RequestContextUtil(), mapper, audit);
        FilterChain chain = mock(FilterChain.class);

        // 11 requests against the 10/min login bucket.
        MockHttpServletResponse lastResponse = null;
        for (int i = 0; i < 11; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setMethod("POST");
            req.setRequestURI("/auth/login");
            req.setRemoteAddr("203.0.113.10");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilterInternal(req, res, chain);
            lastResponse = res;
        }

        assertNotNull(lastResponse);
        assertEquals(429, lastResponse.getStatus());
        String retryAfter = lastResponse.getHeader("Retry-After");
        assertNotNull(retryAfter, "Retry-After header must be present on 429");
        assertTrue(Long.parseLong(retryAfter) >= 1);
        assertTrue(lastResponse.getContentAsString().contains("retry-after-seconds"),
                "429 body should expose retry-after seconds for the client");
        verify(audit, atLeastOnce()).record(
                eq(AuditEventType.RATE_LIMIT_BLOCKED),
                any(),
                isNull(),
                any(),
                any(),
                isNull(),
                any()
        );
    }

    @Test
    void firstFewRequestsShouldNotBe429() throws Exception {
        RateLimiterService limiter = new RateLimiterService(new InMemoryRateLimiterBackend());
        AuditService audit = mock(AuditService.class);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RateLimitFilter filter = new RateLimitFilter(limiter, new RequestContextUtil(), mapper, audit);
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod("POST");
        req.setRequestURI("/auth/login");
        req.setRemoteAddr("203.0.113.20");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, chain);

        assertNotEquals(429, res.getStatus());
        verify(audit, never()).record(eq(AuditEventType.RATE_LIMIT_BLOCKED),
                any(), any(), any(), any(), any(), any());
    }
}
