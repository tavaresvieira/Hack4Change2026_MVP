package org.resourcebridge.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.resourcebridge.api.enums.Urgency;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "needs")
@SQLDelete(sql = "UPDATE needs SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Need {

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
    private int quantityNeeded;

    @NotNull(message = "Urgency is required")
    @Enumerated(EnumType.STRING)
    private Urgency urgency;

    private boolean fulfilled;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
