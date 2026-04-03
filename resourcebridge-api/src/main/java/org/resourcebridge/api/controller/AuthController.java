package org.resourcebridge.api.controller;

import lombok.RequiredArgsConstructor;
import org.resourcebridge.api.dto.AuthResponse;
import org.resourcebridge.api.dto.LoginRequest;
import org.resourcebridge.api.dto.RefreshRequest;
import org.resourcebridge.api.dto.RegisterRequest;
import org.resourcebridge.api.dto.RegisterByInviteRequest;
import org.resourcebridge.api.entity.Invitation;
import org.resourcebridge.api.entity.Organization;
import org.resourcebridge.api.entity.RefreshToken;
import org.resourcebridge.api.entity.User;
import org.resourcebridge.api.enums.Role;
import org.resourcebridge.api.exception.ResourceNotFoundException;
import org.resourcebridge.api.repository.InvitationRepository;
import org.resourcebridge.api.repository.OrganizationRepository;
import org.resourcebridge.api.repository.UserRepository;
import org.resourcebridge.api.security.JwtUtil;
import org.resourcebridge.api.service.RefreshTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final InvitationRepository invitationRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    // POST /api/auth/register — coordinator or staff signs up
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already registered");
        }

        if (request.getRole() == Role.DONOR) {
            return ResponseEntity.badRequest().body("Donors do not need an account");
        }
        if (request.getRole() == Role.STAFF) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Staff accounts require an invitation — ask your admin for an invite link");
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
        String refreshToken = refreshTokenService.generate(user).getToken();

        return ResponseEntity.ok(new AuthResponse(
                user.getId(),
                token,
                refreshToken,
                user.getName(),
                user.getEmail(),
                user.getRole(),
                organization.getId(),
                organization.getName()
        ));
    }

    // POST /api/auth/login — coordinator or staff logs in
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        String refreshToken = refreshTokenService.generate(user).getToken();

        Long orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        String orgName = user.getOrganization() != null ? user.getOrganization().getName() : null;

        return ResponseEntity.ok(new AuthResponse(
                user.getId(),
                token,
                refreshToken,
                user.getName(),
                user.getEmail(),
                user.getRole(),
                orgId,
                orgName
        ));
    }

    // POST /api/auth/register-by-invite — staff registers using an invite token
    @PostMapping("/register-by-invite")
    public ResponseEntity<?> registerByInvite(@Valid @RequestBody RegisterByInviteRequest request) {

        Invitation invitation = invitationRepository.findByToken(request.getToken()).orElse(null);

        if (invitation == null || invitation.isUsed()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Invitation not found or already used");
        }
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.GONE).body("Invitation has expired");
        }
        if (userRepository.existsByEmail(invitation.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already registered");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(invitation.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(invitation.getRole());
        user.setOrganization(invitation.getOrganization());
        userRepository.save(user);

        invitation.setUsed(true);
        invitationRepository.save(invitation);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        String refreshToken = refreshTokenService.generate(user).getToken();
        Organization org = invitation.getOrganization();

        return ResponseEntity.ok(new AuthResponse(
                user.getId(),
                token,
                refreshToken,
                user.getName(),
                user.getEmail(),
                user.getRole(),
                org.getId(),
                org.getName()
        ));
    }

    // POST /api/auth/refresh — exchange a valid refresh token for a new access + refresh token pair
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshToken stored = refreshTokenService.findByToken(request.getRefreshToken()).orElse(null);

        if (stored == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }
        if (refreshTokenService.isExpired(stored)) {
            refreshTokenService.delete(stored.getToken());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token expired — please log in again");
        }

        User user = stored.getUser();

        // Rotate: delete old, issue new refresh token
        refreshTokenService.delete(stored.getToken());
        String newRefreshToken = refreshTokenService.generate(user).getToken();
        String newAccessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        Long orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        String orgName = user.getOrganization() != null ? user.getOrganization().getName() : null;

        return ResponseEntity.ok(new AuthResponse(
                user.getId(),
                newAccessToken,
                newRefreshToken,
                user.getName(),
                user.getEmail(),
                user.getRole(),
                orgId,
                orgName
        ));
    }

    // POST /api/auth/logout — invalidate the refresh token
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        refreshTokenService.delete(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }
}
