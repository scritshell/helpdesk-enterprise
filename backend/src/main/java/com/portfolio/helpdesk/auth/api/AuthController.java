package com.portfolio.helpdesk.auth.api;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.helpdesk.auth.api.AuthDtos.LoginRequest;
import com.portfolio.helpdesk.auth.api.AuthDtos.LoginResponse;
import com.portfolio.helpdesk.auth.api.AuthDtos.UserResponse;
import com.portfolio.helpdesk.auth.application.AuthService;
import com.portfolio.helpdesk.auth.application.LoginAttemptService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {
    private static final String GENERIC_LOGIN_ERROR = "No se pudo iniciar sesión con esas credenciales.";

    private final AuthService authService;
    private final LoginAttemptService loginAttemptService;

    public AuthController(AuthService authService, LoginAttemptService loginAttemptService) {
        this.authService = authService;
        this.loginAttemptService = loginAttemptService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest servletRequest
    ) {
        String clientIp = servletRequest.getRemoteAddr();
        if (!loginAttemptService.tryBeginAttempt(request.email(), clientIp)) {
            long retryAfter = Math.max(1, loginAttemptService.retryAfterSeconds(request.email(), clientIp));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter))
                .body(Map.of("message", "Demasiados intentos. Inténtalo de nuevo más tarde."));
        }

        try {
            LoginResponse response = authService.login(request);
            loginAttemptService.recordSuccess(request.email(), clientIp);
            return ResponseEntity.ok(response);
        } catch (AuthenticationException exception) {
            loginAttemptService.recordFailure(request.email(), clientIp);
            // Keep the message identical for unknown users and incorrect passwords.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", GENERIC_LOGIN_ERROR));
        } catch (RuntimeException exception) {
            loginAttemptService.cancelAttempt(request.email(), clientIp);
            throw exception;
        }
    }

    @GetMapping("/me")
    public UserResponse currentUser(Authentication authentication) {
        return authService.currentUser(authentication.getName());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Void> handleAuthenticationFailure() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
