package com.portfolio.helpdesk.auth.infrastructure;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.portfolio.helpdesk.auth.domain.UserAccount;
import com.portfolio.helpdesk.auth.domain.RoleName;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<UserAccount> findDistinctByRolesNameOrderByLastNameAscFirstNameAsc(RoleName roleName);

    List<UserAccount> findAllByOrderByLastNameAscFirstNameAsc();

    @Query("select count(distinct user.id) from UserAccount user join user.roles role where user.enabled = true and role.name = :role")
    long countActiveUsersWithRole(RoleName role);
}
