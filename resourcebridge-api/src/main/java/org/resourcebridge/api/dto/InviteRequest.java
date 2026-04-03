package org.resourcebridge.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.resourcebridge.api.enums.Role;

@Data
public class InviteRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotNull(message = "Organization is required")
    private Long organizationId;

    // Optional — defaults to STAFF if omitted
    private Role role;
}
