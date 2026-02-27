package com.yourapp.dto;

import com.yourapp.model.PermissionType;
import lombok.Data;

@Data
public class ShareNoteRequest {
    private String         targetUserId;
    private PermissionType permissionType; // VIEW or COMMENT
}
