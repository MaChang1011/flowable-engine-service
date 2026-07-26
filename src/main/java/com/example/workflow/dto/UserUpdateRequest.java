package com.example.workflow.dto;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String id;
    private String realName;
    private String orgId;
    private String roleIds;
    private String email;
    private String phone;
    private Integer status;
}
