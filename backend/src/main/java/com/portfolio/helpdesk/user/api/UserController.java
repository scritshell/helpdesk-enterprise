package com.portfolio.helpdesk.user.api;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.helpdesk.auth.domain.RoleName;
import com.portfolio.helpdesk.auth.infrastructure.UserAccountRepository;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserAccountRepository userRepository;

    public UserController(UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/technicians")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    public List<UserDtos.UserSummary> technicians() {
        return userRepository.findDistinctByRolesNameOrderByLastNameAscFirstNameAsc(RoleName.TECHNICIAN)
            .stream()
            .map(user -> new UserDtos.UserSummary(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName()))
            .toList();
    }
}
