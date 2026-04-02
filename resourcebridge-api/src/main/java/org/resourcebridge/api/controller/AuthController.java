package org.resourcebridge.api.controller;

import lombok.RequiredArgsConstructor;
import org.resourcebridge.api.dto.AuthResponse;
import org.resourcebridge.api.dto.LoginRequest;
import org.resourcebridge.api.dto.RegisterRequest;
import org.resourcebridge.api.entity.Organization;
import org.resourcebridge.api.entity.User;
import org.resourcebridge.api.enums.Role;
import org.resourcebridge.api.exception.ResourceNotFoundException;
import org.resourcebridge.api.repository.OrganizationRepository;
import org.resourcebridge.api.repository.UserRepository;
import org.resourcebridge.api.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    // POST /api/auth/register — coordinator or staff signs up
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already registered");
        }

        if (request.getRole() == Role.DONOR) {
            return ResponseEntity.badRequest().body("Donors do not need an account");
        }

        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization", request.getOrganizationId()));

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setOrganization(organization);

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return ResponseEntity.ok(new AuthResponse(
                user.getId(),
                token,
                user.getName(),
                user.getEmail(),
                user.getRole(),
                organization.getId(),
                organization.getName()
        ));
    }

    // POST /api/auth/login — coordinator or staff logs in
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        Long orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;

        String orgName = user.getOrganization() != null ? user.getOrganization().getName() : null;

        return ResponseEntity.ok(new AuthResponse(
                user.getId(),
                token,
                user.getName(),
                user.getEmail(),
                user.getRole(),
                orgId,
                orgName
        ));
    }
}
