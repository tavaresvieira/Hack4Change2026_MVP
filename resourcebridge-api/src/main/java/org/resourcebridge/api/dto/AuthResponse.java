package org.resourcebridge.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.resourcebridge.api.enums.Role;

@Data
@AllArgsConstructor
public class AuthResponse {
    private Long id;
    private String token;
    private String name;
    private String email;
    private Role role;
    private Long organizationId;
    private String organizationName;
}
