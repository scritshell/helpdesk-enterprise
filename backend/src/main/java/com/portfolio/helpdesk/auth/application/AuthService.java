package com.portfolio.helpdesk.auth.application;

import java.util.Comparator;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.helpdesk.auth.api.AuthDtos.LoginRequest;
import com.portfolio.helpdesk.auth.api.AuthDtos.LoginResponse;
import com.portfolio.helpdesk.auth.api.AuthDtos.UserResponse;
import com.portfolio.helpdesk.auth.domain.UserAccount;
import com.portfolio.helpdesk.auth.infrastructure.UserAccountRepository;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserAccountRepository userRepository;
    private final JwtService jwtService;

    public AuthService(
        AuthenticationManager authenticationManager,
        UserAccountRepository userRepository,
        JwtService jwtService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserAccount user = userRepository.findByEmailIgnoreCase(request.email())
            .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));

        return new LoginResponse(jwtService.generateToken(user), "Bearer", toResponse(user));
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(String email) {
        UserAccount user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));
        return toResponse(user);
    }

    private UserResponse toResponse(UserAccount user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getRoles().stream()
                .map(role -> role.getName().name())
                .sorted(Comparator.naturalOrder())
                .toList()
        );
    }
}
