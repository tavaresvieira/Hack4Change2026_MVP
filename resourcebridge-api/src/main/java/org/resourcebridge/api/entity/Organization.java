package org.resourcebridge.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "organizations")
@SQLDelete(sql = "UPDATE organizations SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Organization name is required")
    @Column(nullable = false)
    private String name;

    private String type; // shelter, food_bank, community_center, etc.

    private String address;

    private String populationServed;

    @Email(message = "Contact email must be a valid email address")
    private String contactEmail;

    private String contactPhone;

    private LocalDateTime deletedAt;
}
