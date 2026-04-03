package org.resourcebridge.api.controller;

import lombok.RequiredArgsConstructor;
import org.resourcebridge.api.entity.Announcement;
import org.resourcebridge.api.enums.AnnouncementType;
import org.resourcebridge.api.service.AnnouncementService;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    // GET /api/announcements — latest first, all types
    @GetMapping
    public List<Announcement> getLatest() {
        return announcementService.findLatest();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Announcement> getById(@PathVariable Long id) {
        return ResponseEntity.ok(announcementService.getById(id));
    }

    @GetMapping("/organization/{organizationId}")
    public List<Announcement> getByOrganization(@PathVariable Long organizationId) {
        return announcementService.findByOrganizationId(organizationId);
    }

    // GET /api/announcements/type/EXPIRY
    @GetMapping("/type/{type}")
    public List<Announcement> getByType(@PathVariable AnnouncementType type) {
        return announcementService.findByType(type);
    }

    // POST /api/announcements — shelter staff posts expiry/surplus/urgent announcement
    @PostMapping
    public ResponseEntity<Announcement> create(@Valid @RequestBody Announcement announcement) {
        return ResponseEntity.ok(announcementService.save(announcement));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        announcementService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
