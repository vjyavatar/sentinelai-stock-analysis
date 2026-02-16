package com.sentinel.stockanalysis.controller;

import com.sentinel.stockanalysis.dto.*;
import com.sentinel.stockanalysis.entity.User;
import com.sentinel.stockanalysis.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller
 * 
 * Endpoints for user authentication
 * 
 * @author Sentinel AI Team
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthenticationController {

    private final AuthenticationService authService;

    /**
     * User signup
     * 
     * POST /api/auth/signup
     */
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        log.info("📝 Signup request for: {}", request.getEmail());
        
        try {
            AuthResponse response = authService.signup(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Signup failed: {}", e.getMessage());
            throw new RuntimeException("Signup failed: " + e.getMessage());
        }
    }

    /**
     * User login
     * 
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("🔐 Login request for: {}", request.getEmail());
        
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Login failed: {}", e.getMessage());
            throw new RuntimeException("Invalid credentials");
        }
    }

    /**
     * Get current user profile
     * 
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(@AuthenticationPrincipal User user) {
        UserDTO userDTO = authService.getCurrentUser(user);
        return ResponseEntity.ok(userDTO);
    }

    /**
     * Health check for auth service
     * 
     * GET /api/auth/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "authentication"
        ));
    }
}
