package org.resourcebridge.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InviteResponse {
    private String token;
    private String email;
    private String organizationName;
    private String inviteUrl;
}
