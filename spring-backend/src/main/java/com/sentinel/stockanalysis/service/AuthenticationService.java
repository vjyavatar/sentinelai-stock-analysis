package com.sentinel.stockanalysis.service;

import com.sentinel.stockanalysis.dto.*;
import com.sentinel.stockanalysis.entity.User;
import com.sentinel.stockanalysis.repository.UserRepository;
import com.sentinel.stockanalysis.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication Service
 * 
 * Handles user registration, login, and authentication
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Register new user
     */
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        // Check if user exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Create user
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .isActive(true)
                .isVerified(true) // Auto-verify for now
                .role(User.UserRole.USER)
                .build();

        User saved = userRepository.save(user);
        log.info("✅ New user registered: {}", saved.getEmail());

        // Generate JWT token
        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .user(mapToUserDTO(saved))
                .build();
    }

    /**
     * User login
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Authenticate
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Get user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update last login
        user.updateLastLogin();
        userRepository.save(user);

        log.info("✅ User logged in: {}", user.getEmail());

        // Generate token
        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .user(mapToUserDTO(user))
                .build();
    }

    /**
     * Load user by email (for Spring Security)
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    /**
     * Get current user profile
     */
    @Transactional(readOnly = true)
    public UserDTO getCurrentUser(User user) {
        return mapToUserDTO(user);
    }

    // Helper method
    private UserDTO mapToUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .isVerified(user.getIsVerified())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }
}
