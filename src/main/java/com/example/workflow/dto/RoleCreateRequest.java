package com.example.workflow.dto;

import lombok.Data;

@Data
public class RoleCreateRequest {
    private String code;
    private String roleName;
    private String scopeType;      // SELF / DEPT / ALL / CROSS
    private String scopeOrgIds;    // 逗号分隔的机构ID（CROSS模式用）
    private String description;
}
