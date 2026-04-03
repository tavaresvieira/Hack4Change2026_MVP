package org.resourcebridge.api.repository;

import org.resourcebridge.api.entity.ExchangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExchangeRequestRepository extends JpaRepository<ExchangeRequest, Long> {
    List<ExchangeRequest> findByAnnouncement_Organization_Id(Long orgId);
    List<ExchangeRequest> findByRequestingOrg_Id(Long orgId);
    boolean existsByAnnouncement_IdAndRequestingOrg_Id(Long announcementId, Long requestingOrgId);
}
