package com.portfolio.helpdesk.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import com.portfolio.helpdesk.auth.domain.Role;
import com.portfolio.helpdesk.auth.domain.RoleName;
import com.portfolio.helpdesk.auth.domain.UserAccount;
import com.portfolio.helpdesk.auth.infrastructure.RoleRepository;
import com.portfolio.helpdesk.auth.infrastructure.UserAccountRepository;
import com.portfolio.helpdesk.user.api.UserDtos;
import com.portfolio.helpdesk.user.application.UserAdminService;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {
    @Mock UserAccountRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock Authentication authentication;
    private UserAdminService service;
    private Role adminRole;
    private Role userRole;

    @BeforeEach
    void setUp() {
        service = new UserAdminService(userRepository, roleRepository);
        adminRole = new Role(RoleName.ADMIN);
        userRole = new Role(RoleName.USER);
    }

    @Test
    void cannotRemoveTheLastActiveAdministrator() {
        UserAccount admin = user("admin@test.local", adminRole);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countActiveUsersWithRole(RoleName.ADMIN)).thenReturn(1L);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.update(1L,
            new UserDtos.UpdateUserRequest(Set.of(RoleName.USER), true), authentication));

        assertEquals(409, exception.getStatusCode().value());
    }

    @Test
    void canChangeRoleAndDisableNonAdministrator() {
        UserAccount user = user("user@test.local", userRole);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.TECHNICIAN)).thenReturn(Optional.of(new Role(RoleName.TECHNICIAN)));

        UserDtos.UserResponse response = service.update(1L,
            new UserDtos.UpdateUserRequest(Set.of(RoleName.TECHNICIAN), false), authentication);

        assertEquals(Set.of(RoleName.TECHNICIAN), response.roles());
        assertEquals(false, response.enabled());
        assertEquals(false, user.isEnabled());
    }

    private UserAccount user(String email, Role role) {
        UserAccount account = new UserAccount(email, "hash", "Test", "User");
        account.addRole(role);
        return account;
    }
}
