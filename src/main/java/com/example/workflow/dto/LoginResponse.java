package com.example.workflow.dto;

import com.example.workflow.entity.SysUser;
import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private String userId;
    private String username;
    private String realName;
    private String orgId;
    private String orgName;
    private String roleIds;
    private String scopeType;
    private java.util.List<String> accessibleOrgIds;

    public static LoginResponse of(String token, SysUser user, String orgName,
                                   String scopeType, java.util.List<String> accessibleOrgIds) {
        LoginResponse r = new LoginResponse();
        r.setToken(token);
        r.setUserId(user.getId());
        r.setUsername(user.getUsername());
        r.setRealName(user.getRealName());
        r.setOrgId(user.getOrgId());
        r.setRoleIds(user.getRoleIds());
        r.setOrgName(orgName);
        r.setScopeType(scopeType);
        r.setAccessibleOrgIds(accessibleOrgIds);
        return r;
    }
}