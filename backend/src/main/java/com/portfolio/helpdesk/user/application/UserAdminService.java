package com.portfolio.helpdesk.user.application;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.portfolio.helpdesk.auth.domain.Role;
import com.portfolio.helpdesk.auth.domain.RoleName;
import com.portfolio.helpdesk.auth.domain.UserAccount;
import com.portfolio.helpdesk.auth.infrastructure.RoleRepository;
import com.portfolio.helpdesk.auth.infrastructure.UserAccountRepository;
import com.portfolio.helpdesk.user.api.UserDtos;

@Service
public class UserAdminService {
    private final UserAccountRepository userRepository;
    private final RoleRepository roleRepository;

    public UserAdminService(UserAccountRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<UserDtos.UserResponse> list() {
        return userRepository.findAllByOrderByLastNameAscFirstNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public UserDtos.UserResponse update(Long id, UserDtos.UpdateUserRequest request, Authentication authentication) {
        UserAccount target = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        Set<RoleName> requestedRoles = request.roles();
        boolean remainsAdmin = requestedRoles.contains(RoleName.ADMIN);
        boolean removesLastActiveAdmin = target.isEnabled() && hasRole(target, RoleName.ADMIN)
            && (!request.enabled() || !remainsAdmin)
            && userRepository.countActiveUsersWithRole(RoleName.ADMIN) <= 1;
        if (removesLastActiveAdmin) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede dejar el sistema sin un administrador activo");
        }
        if (target.getEmail().equalsIgnoreCase(authentication.getName()) && !request.enabled() && remainsAdmin
            && userRepository.countActiveUsersWithRole(RoleName.ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No puedes desactivar tu propia cuenta como último administrador");
        }

        Set<Role> roles = requestedRoles.stream().map(role -> roleRepository.findByName(role)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol no válido: " + role))).collect(java.util.stream.Collectors.toSet());
        target.replaceRoles(roles);
        target.setEnabled(request.enabled());
        return toResponse(target);
    }

    private boolean hasRole(UserAccount user, RoleName role) {
        return user.getRoles().stream().anyMatch(item -> item.getName() == role);
    }

    private UserDtos.UserResponse toResponse(UserAccount user) {
        return new UserDtos.UserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
            user.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toUnmodifiableSet()),
            user.isEnabled(), user.getCreatedAt());
    }
}
