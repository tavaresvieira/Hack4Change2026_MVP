package org.resourcebridge.api.controller;

import lombok.RequiredArgsConstructor;
import org.resourcebridge.api.dto.AdminStatsDto;
import org.resourcebridge.api.entity.Inventory;
import org.resourcebridge.api.entity.Need;
import org.resourcebridge.api.entity.User;
import org.resourcebridge.api.enums.DonationStatus;
import org.resourcebridge.api.enums.Role;
import org.resourcebridge.api.enums.TransferStatus;
import org.resourcebridge.api.enums.Urgency;
import org.resourcebridge.api.exception.ResourceNotFoundException;
import org.resourcebridge.api.repository.*;
import org.resourcebridge.api.service.RefreshTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DonationRepository donationRepository;
    private final NeedRepository needRepository;
    private final TransferRepository transferRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final RefreshTokenService refreshTokenService;

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDto> getStats() {
        AdminStatsDto stats = new AdminStatsDto();

        // ── Donations ──────────────────────────────────────────────
        stats.setTotalDonations(donationRepository.count());

        Map<String, Long> donationsByStatus = new LinkedHashMap<>();
        for (DonationStatus s : DonationStatus.values()) {
            donationsByStatus.put(s.name(), (long) donationRepository.findByStatus(s).size());
        }
        stats.setDonationsByStatus(donationsByStatus);

        // ── Needs ──────────────────────────────────────────────────
        stats.setTotalNeeds(needRepository.count());

        List<Need> openNeeds = needRepository.findByFulfilled(false);
        List<Need> fulfilledNeeds = needRepository.findByFulfilled(true);
        stats.setOpenNeeds(openNeeds.size());
        stats.setFulfilledNeeds(fulfilledNeeds.size());

        Map<String, Long> openNeedsByUrgency = new LinkedHashMap<>();
        for (Urgency u : new Urgency[]{Urgency.CRITICAL, Urgency.HIGH, Urgency.MEDIUM, Urgency.LOW}) {
            long count = openNeeds.stream().filter(n -> n.getUrgency() == u).count();
            openNeedsByUrgency.put(u.name(), count);
        }
        stats.setOpenNeedsByUrgency(openNeedsByUrgency);

        // ── Transfers ──────────────────────────────────────────────
        stats.setTotalTransfers(transferRepository.count());

        Map<String, Long> transfersByStatus = new LinkedHashMap<>();
        for (TransferStatus s : TransferStatus.values()) {
            transfersByStatus.put(s.name(), (long) transferRepository.findByStatus(s).size());
        }
        stats.setTransfersByStatus(transfersByStatus);

        // ── Orgs & Staff ───────────────────────────────────────────
        stats.setTotalOrganizations(organizationRepository.count());
        stats.setTotalStaff((long) userRepository.findByRole(Role.STAFF).size());

        // ── Top urgent unfulfilled needs (max 5) ───────────────────
        List<AdminStatsDto.TopNeedDto> topNeeds = openNeeds.stream()
                .sorted(Comparator.comparingInt(n -> urgencyOrder(n.getUrgency())))
                .limit(5)
                .map(n -> {
                    AdminStatsDto.TopNeedDto dto = new AdminStatsDto.TopNeedDto();
                    dto.setItemName(n.getItem().getName());
                    dto.setOrganizationName(n.getOrganization().getName());
                    dto.setQuantityNeeded(n.getQuantityNeeded());
                    dto.setUrgency(n.getUrgency().name());
                    return dto;
                })
                .collect(Collectors.toList());
        stats.setTopUrgentNeeds(topNeeds);

        // ── Expiring inventory (within 7 days, including already expired) ──────
        LocalDate threshold = LocalDate.now().plusDays(7);
        List<Inventory> expiringInv = inventoryRepository.findByExpiryDateBefore(threshold);
        List<AdminStatsDto.ExpiringItemDto> expiringItems = expiringInv.stream()
                .sorted(Comparator.comparing(Inventory::getExpiryDate))
                .map(inv -> {
                    AdminStatsDto.ExpiringItemDto dto = new AdminStatsDto.ExpiringItemDto();
                    dto.setItemName(inv.getItem().getName());
                    dto.setUnit(inv.getItem().getUnit());
                    dto.setOrganizationName(inv.getOrganization().getName());
                    dto.setQuantity(inv.getQuantity());
                    dto.setExpiryDate(inv.getExpiryDate().toString());
                    dto.setDaysUntilExpiry((int) ChronoUnit.DAYS.between(LocalDate.now(), inv.getExpiryDate()));
                    return dto;
                })
                .collect(Collectors.toList());
        stats.setExpiringItems(expiringItems);

        return ResponseEntity.ok(stats);
    }

    private int urgencyOrder(Urgency u) {
        return switch (u) {
            case CRITICAL -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
        };
    }

    // ── Staff management ───────────────────────────────────────────────────────

    // GET /api/admin/staff — all staff including deactivated
    @GetMapping("/staff")
    public ResponseEntity<List<User>> getAllStaff() {
        return ResponseEntity.ok(userRepository.findAllStaffIncludingDeactivated());
    }

    // PATCH /api/admin/staff/{id}/deactivate — soft-deletes the user and kills their session
    @PatchMapping("/staff/{id}/deactivate")
    public ResponseEntity<Void> deactivateStaff(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));

        if (user.getRole() == Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        refreshTokenService.deleteAllForUser(user);
        userRepository.deleteById(id); // intercepted by @SQLDelete → sets deleted_at
        return ResponseEntity.noContent().build();
    }

    // PATCH /api/admin/staff/{id}/restore — un-deletes the user
    @PatchMapping("/staff/{id}/restore")
    public ResponseEntity<Void> restoreStaff(@PathVariable Long id) {
        userRepository.findDeactivatedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deactivated staff member not found"));

        userRepository.restoreById(id);
        return ResponseEntity.noContent().build();
    }

    // ── Organization management ────────────────────────────────────────────────

    // PATCH /api/admin/organizations/{id}/restore — un-archives an organization
    @PatchMapping("/organizations/{id}/restore")
    public ResponseEntity<Void> restoreOrganization(@PathVariable Long id) {
        organizationRepository.restoreById(id);
        return ResponseEntity.noContent().build();
    }
}
