package com.portfolio.helpdesk.auth.application;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.portfolio.helpdesk.auth.infrastructure.UserAccountRepository;

@Service
public class UserAccountDetailsService implements UserDetailsService {
    private final UserAccountRepository userRepository;

    public UserAccountDetailsService(UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmailIgnoreCase(username)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }
}
