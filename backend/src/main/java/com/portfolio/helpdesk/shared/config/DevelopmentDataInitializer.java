package com.portfolio.helpdesk.shared.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.portfolio.helpdesk.auth.domain.Role;
import com.portfolio.helpdesk.auth.domain.RoleName;
import com.portfolio.helpdesk.auth.domain.UserAccount;
import com.portfolio.helpdesk.auth.infrastructure.RoleRepository;
import com.portfolio.helpdesk.auth.infrastructure.UserAccountRepository;

@Configuration
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = false)
public class DevelopmentDataInitializer {
    @Bean
    CommandLineRunner seedDevelopmentUsers(
        RoleRepository roleRepository,
        UserAccountRepository userRepository,
        PasswordEncoder passwordEncoder
    ) {
        return args -> {
            Role userRole = roleRepository.findByName(RoleName.USER).orElseThrow();
            Role technicianRole = roleRepository.findByName(RoleName.TECHNICIAN).orElseThrow();
            Role adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();

            createIfMissing(userRepository, passwordEncoder, "user@helpdesk.local", "Usuario", "Demo", "User123!", userRole);
            createIfMissing(userRepository, passwordEncoder, "technician@helpdesk.local", "Técnico", "Demo", "Technician123!", technicianRole);
            createIfMissing(userRepository, passwordEncoder, "admin@helpdesk.local", "Administrador", "Demo", "Admin123!", adminRole);
        };
    }

    private void createIfMissing(
        UserAccountRepository repository,
        PasswordEncoder encoder,
        String email,
        String firstName,
        String lastName,
        String rawPassword,
        Role role
    ) {
        if (repository.existsByEmailIgnoreCase(email)) {
            return;
        }
        UserAccount account = new UserAccount(email, encoder.encode(rawPassword), firstName, lastName);
        account.addRole(role);
        repository.save(account);
    }
}
