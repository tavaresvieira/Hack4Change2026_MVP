package org.resourcebridge.api.controller;

import lombok.RequiredArgsConstructor;
import org.resourcebridge.api.entity.ExchangeRequest;
import org.resourcebridge.api.enums.ExchangeStatus;
import org.resourcebridge.api.repository.AnnouncementRepository;
import org.resourcebridge.api.repository.ExchangeRequestRepository;
import org.resourcebridge.api.repository.OrganizationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exchange-requests")
@RequiredArgsConstructor
public class ExchangeController {

    private final ExchangeRequestRepository repo;
    private final AnnouncementRepository announcementRepo;
    private final OrganizationRepository orgRepo;

    // POST /api/exchange-requests — shelter responds to another shelter's announcement
    @PostMapping
    public ResponseEntity<ExchangeRequest> create(@RequestBody Map<String, Long> body) {
        Long announcementId = body.get("announcementId");
        Long requestingOrgId = body.get("requestingOrgId");

        if (repo.existsByAnnouncement_IdAndRequestingOrg_Id(announcementId, requestingOrgId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        ExchangeRequest req = new ExchangeRequest();
        req.setAnnouncement(announcementRepo.findById(announcementId).orElseThrow());
        req.setRequestingOrg(orgRepo.findById(requestingOrgId).orElseThrow());
        return ResponseEntity.ok(repo.save(req));
    }

    // GET /api/exchange-requests/incoming/{orgId} — requests on YOUR announcements
    @GetMapping("/incoming/{orgId}")
    public List<ExchangeRequest> getIncoming(@PathVariable Long orgId) {
        return repo.findByAnnouncement_Organization_Id(orgId);
    }

    // GET /api/exchange-requests/outgoing/{orgId} — requests YOU made
    @GetMapping("/outgoing/{orgId}")
    public List<ExchangeRequest> getOutgoing(@PathVariable Long orgId) {
        return repo.findByRequestingOrg_Id(orgId);
    }

    // PATCH /api/exchange-requests/{id}/status — accept or reject
    @PatchMapping("/{id}/status")
    public ResponseEntity<ExchangeRequest> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        ExchangeRequest req = repo.findById(id).orElseThrow();
        req.setStatus(ExchangeStatus.valueOf(body.get("status")));
        return ResponseEntity.ok(repo.save(req));
    }
}
