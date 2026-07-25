package com.example.workflow.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysRole {
    private String id;
    private String roleCode;
    private String roleName;
    private String scopeType;
    private String scopeOrgIds;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
