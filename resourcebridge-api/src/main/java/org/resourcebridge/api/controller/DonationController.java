package org.resourcebridge.api.controller;

import lombok.RequiredArgsConstructor;
import org.resourcebridge.api.dto.PageResponse;
import org.resourcebridge.api.entity.Donation;
import org.resourcebridge.api.enums.DonationStatus;
import org.resourcebridge.api.repository.DonationRepository;
import org.resourcebridge.api.service.DonationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/donations")
@RequiredArgsConstructor
public class DonationController {

    private final DonationService donationService;
    private final DonationRepository donationRepository;

    // GET /api/donations — coordinator sees all OFFERED donations
    @GetMapping
    public List<Donation> getOffered() {
        return donationService.findByStatus(DonationStatus.OFFERED);
    }

    @GetMapping("/all")
    public List<Donation> getAll() {
        return donationService.getAll();
    }

    // GET /api/donations/page?page=0&size=20 — paginated all donations (admin view)
    @GetMapping("/page")
    public PageResponse<Donation> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.of(donationRepository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Donation> getById(@PathVariable Long id) {
        return ResponseEntity.ok(donationService.getById(id));
    }

    // GET /api/donations/status/ASSIGNED
    @GetMapping("/status/{status}")
    public List<Donation> getByStatus(@PathVariable DonationStatus status) {
        return donationService.findByStatus(status);
    }

    // GET /api/donations/donor?email=john@email.com — donor tracks their donations
    @GetMapping("/donor")
    public List<Donation> getByDonorEmail(@RequestParam String email) {
        return donationService.findByDonorEmail(email);
    }

    // GET /api/donations/available/item/3 — find available donations for a specific item
    @GetMapping("/available/item/{itemId}")
    public List<Donation> getAvailableByItem(@PathVariable Long itemId) {
        return donationService.findAvailableByItem(itemId);
    }

    // POST /api/donations — donor submits a donation offer (no token required)
    @PostMapping
    public ResponseEntity<Donation> create(@Valid @RequestBody Donation donation) {
        return ResponseEntity.ok(donationService.save(donation));
    }

    // PATCH /api/donations/1/status?status=ASSIGNED — coordinator updates status
    @PatchMapping("/{id}/status")
    public ResponseEntity<Donation> updateStatus(
            @PathVariable Long id,
            @RequestParam DonationStatus status) {
        return ResponseEntity.ok(donationService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        donationService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
