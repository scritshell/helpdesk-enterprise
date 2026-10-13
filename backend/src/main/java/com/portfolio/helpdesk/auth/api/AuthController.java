package com.portfolio.helpdesk.auth.api;

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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
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
