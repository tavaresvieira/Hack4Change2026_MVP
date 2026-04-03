package org.resourcebridge.api.controller;

import lombok.RequiredArgsConstructor;
import org.resourcebridge.api.dto.PageResponse;
import org.resourcebridge.api.entity.Need;
import org.resourcebridge.api.enums.Urgency;
import org.resourcebridge.api.repository.NeedRepository;
import org.resourcebridge.api.service.NeedService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/needs")
@RequiredArgsConstructor
public class NeedController {

    private final NeedService needService;
    private final NeedRepository needRepository;

    // GET /api/needs — all unfulfilled needs (donor/coordinator view)
    @GetMapping
    public List<Need> getUnfulfilled() {
        return needService.findUnfulfilledNeeds();
    }

    // GET /api/needs/page?page=0&size=12 — paginated unfulfilled needs (public homepage)
    @GetMapping("/page")
    public PageResponse<Need> getUnfulfilledPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        var pageable = PageRequest.of(page, size,
                Sort.by(
                    Sort.Order.asc("urgency"), // CRITICAL first via DB collation — see urgency order note
                    Sort.Order.desc("createdAt")
                ));
        return PageResponse.of(needRepository.findByFulfilled(false, pageable));
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
    public ResponseEntity<Need> create(@Valid @RequestBody Need need) {
        return ResponseEntity.ok(needService.save(need));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Need> update(@PathVariable Long id, @Valid @RequestBody Need need) {
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
