package backend.exception;

import backend.audit.AuditEventType;
import backend.audit.AuditService;
import backend.dto.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Verifies clean error contracts and audit emission for outside-threat events.
 * The handler must never leak stack traces or raw exception messages.
 */
class GlobalExceptionHandlerTest {

    @Test
    void malformedJsonReturnsCleanBadRequestAndAudit() {
        AuditService audit = mock(AuditService.class);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(audit);
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/message/send");

        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error: trailing comma",
                new MockHttpInputMessage(new byte[0])
        );

        ResponseEntity<ApiErrorResponse> response = handler.handleUnreadable(ex, req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("Malformed request body", body.getMessage());
        // Never echo the raw parser message back to the client.
        String raw = body.toString();
        assertFalse(raw.contains("trailing comma"));
        verify(audit).record(eq(AuditEventType.VALIDATION_REJECTED), any(), isNull(), any(), any(), isNull(), any());
    }

    @Test
    void accessDeniedRecordsForbiddenAccessAuditAndCleanForbidden() {
        AuditService audit = mock(AuditService.class);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(audit);
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/admin/lock-user");

        ResponseEntity<ApiErrorResponse> response = handler.handleAccessDenied(
                new AccessDeniedException("expected"), req
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        ApiErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("Access denied", body.getMessage());
        assertTrue(body.getDetails() == null || body.getDetails().stream().noneMatch(d -> d.contains("Exception")));
        verify(audit).record(eq(AuditEventType.FORBIDDEN_ACCESS), any(), isNull(), any(), any(), isNull(), any());
    }
}
