package org.resourcebridge.api.controller;

import lombok.RequiredArgsConstructor;
import org.resourcebridge.api.entity.FinancialDonation;
import org.resourcebridge.api.repository.FinancialDonationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/financial-donations")
@RequiredArgsConstructor
public class FinancialDonationController {

    private final FinancialDonationRepository repo;

    // POST /api/financial-donations — public, donor submits
    @PostMapping
    public ResponseEntity<FinancialDonation> create(@RequestBody FinancialDonation donation) {
        donation.setId(null); // prevent ID injection
        return ResponseEntity.ok(repo.save(donation));
    }

    // GET /api/financial-donations — admin only (secured in SecurityConfig)
    @GetMapping
    public List<FinancialDonation> getAll() {
        return repo.findAll(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    // GET /api/financial-donations/stats — admin only
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        long count = repo.count();
        BigDecimal total = repo.sumAllAmounts();
        return Map.of("count", count, "total", total);
    }
}
