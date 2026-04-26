package backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Verifies the compensating CSRF control: mutating API requests must carry
 * {@code X-Requested-With: XMLHttpRequest} unless they hit an exempt
 * unauthenticated bootstrap endpoint.
 */
class CustomHeaderCsrfFilterTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final CustomHeaderCsrfFilter filter = new CustomHeaderCsrfFilter(
            mapper, "X-Requested-With", "XMLHttpRequest"
    );

    @Test
    void postWithoutHeaderIsRejected() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/message/send");
        req.setContentType("application/json");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(req, res, chain);

        assertEquals(403, res.getStatus());
        assertTrue(res.getContentAsString().contains("missing required security header"));
        verify(chain, never()).doFilter(req, res);
    }

    @Test
    void postWithHeaderIsAllowed() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/message/send");
        req.addHeader("X-Requested-With", "XMLHttpRequest");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(req, res, chain);

        assertEquals(200, res.getStatus(), "Default mock response status is 200 when chain runs cleanly");
        verify(chain).doFilter(req, res);
    }

    @Test
    void getRequestsAreExempt() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/admin/external-threats");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(req, res, chain);

        verify(chain).doFilter(req, res);
    }

    @Test
    void loginPostIsExemptForBootstrap() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/login");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(req, res, chain);

        verify(chain).doFilter(req, res);
    }
}
