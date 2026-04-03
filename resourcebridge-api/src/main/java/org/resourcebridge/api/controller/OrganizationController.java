package org.resourcebridge.api.controller;

import lombok.RequiredArgsConstructor;
import org.resourcebridge.api.entity.Organization;
import org.resourcebridge.api.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    public List<Organization> getAll() {
        return organizationService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Organization> getById(@PathVariable Long id) {
        return ResponseEntity.ok(organizationService.getById(id));
    }

    @GetMapping("/type/{type}")
    public List<Organization> getByType(@PathVariable String type) {
        return organizationService.findByType(type);
    }

    @PostMapping
    public ResponseEntity<Organization> create(@Valid @RequestBody Organization organization) {
        return ResponseEntity.ok(organizationService.save(organization));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Organization> update(@PathVariable Long id, @Valid @RequestBody Organization updated) {
        Organization existing = organizationService.getById(id);
        existing.setName(updated.getName());
        existing.setType(updated.getType());
        existing.setAddress(updated.getAddress());
        existing.setPopulationServed(updated.getPopulationServed());
        existing.setContactEmail(updated.getContactEmail());
        existing.setContactPhone(updated.getContactPhone());
        return ResponseEntity.ok(organizationService.save(existing));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        organizationService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
