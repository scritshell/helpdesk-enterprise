package com.portfolio.helpdesk.shared.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void keepsSafeClientMessageForExpectedStatus() {
        var response = handler.handleStatus(new ResponseStatusException(HttpStatus.CONFLICT, "La transición no es válida"));

        assertEquals(409, response.getStatusCode().value());
        assertEquals("La transición no es válida", response.getBody().message());
    }

    @Test
    void hidesUnexpectedServerDetails() {
        var response = handler.handleUnexpected();

        assertEquals(500, response.getStatusCode().value());
        assertEquals("No se ha podido completar la operación.", response.getBody().message());
    }

    @Test
    void preservesForbiddenForAuthorizationFailures() {
        var response = handler.handleAccessDenied();

        assertEquals(403, response.getStatusCode().value());
        assertEquals("No tienes permisos para realizar esta acción.", response.getBody().message());
    }
}
