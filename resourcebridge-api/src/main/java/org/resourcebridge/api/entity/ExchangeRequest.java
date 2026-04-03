package org.resourcebridge.api.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.resourcebridge.api.enums.ExchangeStatus;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "exchange_requests")
public class ExchangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @ManyToOne
    @JoinColumn(name = "requesting_org_id", nullable = false)
    private Organization requestingOrg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExchangeStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = ExchangeStatus.PENDING;
    }
}
