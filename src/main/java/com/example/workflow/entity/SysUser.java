package com.example.workflow.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysUser {
    private String id;
    private String username;
    private String realName;
    private String orgId;
    private String roleIds;
    private String email;
    private String phone;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
