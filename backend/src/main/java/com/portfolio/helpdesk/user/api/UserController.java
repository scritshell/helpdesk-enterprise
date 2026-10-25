package com.portfolio.helpdesk.user.api;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.helpdesk.auth.domain.RoleName;
import com.portfolio.helpdesk.auth.infrastructure.UserAccountRepository;
import com.portfolio.helpdesk.user.application.UserAdminService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserAccountRepository userRepository;
    private final UserAdminService userAdminService;

    public UserController(UserAccountRepository userRepository, UserAdminService userAdminService) {
        this.userRepository = userRepository;
        this.userAdminService = userAdminService;
    }

    @GetMapping("/technicians")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    public List<UserDtos.UserSummary> technicians() {
        return userRepository.findDistinctByRolesNameOrderByLastNameAscFirstNameAsc(RoleName.TECHNICIAN)
            .stream()
            .map(user -> new UserDtos.UserSummary(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName()))
            .toList();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDtos.UserResponse> list() {
        return userAdminService.list();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDtos.UserResponse update(@PathVariable Long id, @Valid @RequestBody UserDtos.UpdateUserRequest request,
        org.springframework.security.core.Authentication authentication) {
        return userAdminService.update(id, request, authentication);
    }
}
