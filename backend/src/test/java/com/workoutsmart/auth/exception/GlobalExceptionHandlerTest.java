package com.workoutsmart.auth.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void apiExceptionMapsToItsStatus() {
        ResponseEntity<Map<String, Object>> res =
                handler.handleApiException(new ApiException(HttpStatus.CONFLICT, "Email đã tồn tại"));

        assertEquals(409, res.getStatusCode().value());
        assertEquals("Email đã tồn tại", res.getBody().get("message"));
        assertNotNull(res.getBody().get("timestamp"));
    }

    @Test
    void validationExceptionReturns400WithFields() throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "registerRequest");
        bindingResult.addError(new FieldError("registerRequest", "email", "Email không hợp lệ"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Map<String, Object>> res = handler.handleValidation(ex);

        assertEquals(400, res.getStatusCode().value());
        assertTrue(res.getBody().containsKey("fields"));
        @SuppressWarnings("unchecked")
        Map<String, String> fields = (Map<String, String>) res.getBody().get("fields");
        assertEquals("Email không hợp lệ", fields.get("email"));
    }

    @Test
    void genericExceptionReturns500() {
        ResponseEntity<Map<String, Object>> res =
                handler.handleGeneric(new RuntimeException("boom"));

        assertEquals(500, res.getStatusCode().value());
        assertNotNull(res.getBody().get("message"));
    }
}
