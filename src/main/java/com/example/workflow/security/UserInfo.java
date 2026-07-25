package com.example.workflow.security;

import lombok.Data;

@Data
public class UserInfo {
    private String userId;
    private String username;
    private String realName;
    private String orgId;
    private String roleIds;
    private String scopeType;
    private String scopeOrgIds;
}
