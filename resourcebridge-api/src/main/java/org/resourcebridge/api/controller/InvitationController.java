package org.resourcebridge.api.controller;

import lombok.RequiredArgsConstructor;
import org.resourcebridge.api.dto.InviteRequest;
import org.resourcebridge.api.dto.InviteResponse;
import org.resourcebridge.api.entity.Invitation;
import org.resourcebridge.api.entity.Organization;
import org.resourcebridge.api.enums.Role;
import org.resourcebridge.api.exception.ResourceNotFoundException;
import org.resourcebridge.api.repository.InvitationRepository;
import org.resourcebridge.api.repository.OrganizationRepository;
import org.resourcebridge.api.repository.UserRepository;
import org.resourcebridge.api.service.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationRepository invitationRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    // POST /api/invitations — ADMIN only, generates a staff invite link
    @PostMapping
    public ResponseEntity<?> createInvitation(@Valid @RequestBody InviteRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("A user with this email already exists");
        }

        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization", request.getOrganizationId()));

        String token = UUID.randomUUID().toString();

        Role role = (request.getRole() != null) ? request.getRole() : Role.STAFF;

        Invitation invitation = new Invitation();
        invitation.setToken(token);
        invitation.setEmail(request.getEmail());
        invitation.setOrganization(organization);
        invitation.setRole(role);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));
        invitationRepository.save(invitation);

        String inviteUrl = "http://localhost:5173/register?token=" + token;

        // Send email asynchronously — won't block the response
        emailService.sendInviteEmail(request.getEmail(), token, organization.getName(), role.name());

        return ResponseEntity.ok(new InviteResponse(
                token,
                request.getEmail(),
                organization.getName(),
                inviteUrl
        ));
    }

    // GET /api/invitations/validate/{token} — public, used by register page to prefill info
    @GetMapping("/validate/{token}")
    public ResponseEntity<?> validateToken(@PathVariable String token) {
        Invitation invitation = invitationRepository.findByToken(token).orElse(null);

        if (invitation == null || invitation.isUsed()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Invitation not found or already used");
        }
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.GONE).body("Invitation has expired");
        }

        return ResponseEntity.ok(new InviteResponse(
                invitation.getToken(),
                invitation.getEmail(),
                invitation.getOrganization().getName(),
                null
        ));
    }
}
