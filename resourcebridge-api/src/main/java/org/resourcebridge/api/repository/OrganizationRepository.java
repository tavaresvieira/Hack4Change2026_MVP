package org.resourcebridge.api.repository;

import org.resourcebridge.api.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findByName(String name);

    List<Organization> findByType(String type);

    // Restores a soft-deleted organization
    @Modifying
    @Transactional
    @Query(value = "UPDATE organizations SET deleted_at = NULL WHERE id = :id", nativeQuery = true)
    void restoreById(@Param("id") Long id);

    // All orgs including archived — for admin list view
    @Query(value = "SELECT * FROM organizations", nativeQuery = true)
    List<Organization> findAllIncludingDeleted();
}
