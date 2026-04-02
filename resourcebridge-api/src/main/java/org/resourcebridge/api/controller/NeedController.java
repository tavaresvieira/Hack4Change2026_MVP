package org.resourcebridge.api.controller;

import lombok.RequiredArgsConstructor;
import org.resourcebridge.api.entity.Need;
import org.resourcebridge.api.enums.Urgency;
import org.resourcebridge.api.service.NeedService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/needs")
@RequiredArgsConstructor
public class NeedController {

    private final NeedService needService;

    // GET /api/needs — all unfulfilled needs (donor/coordinator view)
    @GetMapping
    public List<Need> getUnfulfilled() {
        return needService.findUnfulfilledNeeds();
    }

    @GetMapping("/all")
    public List<Need> getAll() {
        return needService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Need> getById(@PathVariable Long id) {
        return ResponseEntity.ok(needService.getById(id));
    }

    @GetMapping("/organization/{organizationId}")
    public List<Need> getByOrganization(@PathVariable Long organizationId) {
        return needService.findUnfulfilledByOrganization(organizationId);
    }

    // GET /api/needs/urgency/CRITICAL
    @GetMapping("/urgency/{urgency}")
    public List<Need> getByUrgency(@PathVariable Urgency urgency) {
        return needService.findByUrgency(urgency);
    }

    @PostMapping
    public ResponseEntity<Need> create(@RequestBody Need need) {
        return ResponseEntity.ok(needService.save(need));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Need> update(@PathVariable Long id, @RequestBody Need need) {
        need.setId(id);
        return ResponseEntity.ok(needService.save(need));
    }

    // PATCH /api/needs/1/fulfill — coordinator/staff marks need as fulfilled
    @PatchMapping("/{id}/fulfill")
    public ResponseEntity<Need> markFulfilled(@PathVariable Long id) {
        return ResponseEntity.ok(needService.markFulfilled(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        needService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
