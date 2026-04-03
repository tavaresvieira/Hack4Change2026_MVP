package org.resourcebridge.api.repository;

import org.resourcebridge.api.entity.Need;
import org.resourcebridge.api.enums.Urgency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NeedRepository extends JpaRepository<Need, Long> {

    List<Need> findByOrganizationId(Long organizationId);

    List<Need> findByFulfilled(boolean fulfilled);

    Page<Need> findByFulfilled(boolean fulfilled, Pageable pageable);

    List<Need> findByOrganizationIdAndFulfilled(Long organizationId, boolean fulfilled);

    List<Need> findByUrgencyAndFulfilled(Urgency urgency, boolean fulfilled);

    // Auto-matching: find unfulfilled needs for an item, ordered by urgency (CRITICAL first)
    @Query("SELECT n FROM Need n WHERE n.item.id = :itemId AND n.fulfilled = false " +
           "ORDER BY CASE n.urgency WHEN 'CRITICAL' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 WHEN 'LOW' THEN 4 END ASC")
    List<Need> findUnfulfilledByItemOrderByUrgency(@Param("itemId") Long itemId);

    // Preferred-org matching: same item, specific org
    @Query("SELECT n FROM Need n WHERE n.item.id = :itemId AND n.organization.id = :orgId AND n.fulfilled = false " +
           "ORDER BY CASE n.urgency WHEN 'CRITICAL' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 WHEN 'LOW' THEN 4 END ASC")
    List<Need> findUnfulfilledByItemAndOrgOrderByUrgency(@Param("itemId") Long itemId, @Param("orgId") Long orgId);
}
