package com.andregattermeyer.api_frota.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

    @Test
    void authenticationFailureReturnsUnauthorizedWithoutLeakingDetails() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");

        var response = handler.handleAuthenticationFailure(
                new BadCredentialsException("secret internal detail"),
                new ServletWebRequest(request));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Usuário ou senha inválidos.", response.getBody().getMessage());
    }
}
