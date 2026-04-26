package backend.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Validates the OWASP-aligned security header set is applied. */
class SecurityHeadersFilterTest {

    private final SecurityHeadersFilter filter = new SecurityHeadersFilter(
            "default-src 'self'; frame-ancestors 'none'"
    );

    @Test
    void apiResponseIncludesBaselineHeaders() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/admin/external-threats");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(req, res, chain);

        assertEquals("nosniff", res.getHeader("X-Content-Type-Options"));
        assertEquals("DENY", res.getHeader("X-Frame-Options"));
        assertEquals("no-referrer", res.getHeader("Referrer-Policy"));
        assertNotNull(res.getHeader("Permissions-Policy"));
        assertNotNull(res.getHeader("Content-Security-Policy"));
        assertTrue(res.getHeader("Content-Security-Policy").contains("frame-ancestors 'none'"));
        // Sensitive endpoints suppress caching.
        assertNotNull(res.getHeader("Cache-Control"));
        assertTrue(res.getHeader("Cache-Control").contains("no-store"));
        verify(chain).doFilter(req, res);
    }

    @Test
    void unsensitivePathStillGetsBaselineHeadersButNoCacheControl() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/something-else");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(req, res, chain);

        assertEquals("nosniff", res.getHeader("X-Content-Type-Options"));
        assertNull(res.getHeader("Cache-Control"),
                "Non-sensitive endpoints should not set explicit no-store");
    }
}
