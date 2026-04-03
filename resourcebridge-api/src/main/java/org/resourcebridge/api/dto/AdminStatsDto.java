package org.resourcebridge.api.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AdminStatsDto {

    // Donations
    private long totalDonations;
    private Map<String, Long> donationsByStatus;

    // Needs
    private long totalNeeds;
    private long openNeeds;
    private long fulfilledNeeds;
    private Map<String, Long> openNeedsByUrgency;

    // Transfers
    private long totalTransfers;
    private Map<String, Long> transfersByStatus;

    // Users & Orgs
    private long totalOrganizations;
    private long totalStaff;

    // Top urgent unfulfilled needs
    private List<TopNeedDto> topUrgentNeeds;

    // Expiring inventory across the network
    private List<ExpiringItemDto> expiringItems;

    @Data
    public static class TopNeedDto {
        private String itemName;
        private String organizationName;
        private int quantityNeeded;
        private String urgency;
    }

    @Data
    public static class ExpiringItemDto {
        private String itemName;
        private String unit;
        private String organizationName;
        private int quantity;
        private String expiryDate; // ISO date string
        private int daysUntilExpiry;
    }
}
