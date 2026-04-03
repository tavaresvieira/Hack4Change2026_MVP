package org.resourcebridge.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.resourcebridge.api.enums.AnnouncementType;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "announcements")
@SQLDelete(sql = "UPDATE announcements SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Organization is required")
    @ManyToOne
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @NotNull(message = "Item is required")
    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(nullable = false)
    private int quantity;

    @NotNull(message = "Announcement type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnnouncementType type;

    @NotBlank(message = "Message is required")
    private String message;

    private String expiryDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
