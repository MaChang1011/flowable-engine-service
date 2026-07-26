package com.example.workflow.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserCreateRequest {
    private String username;
    private String password;
    private String realName;
    private String orgId;
    private String roleIds;
    private String email;
    private String phone;
}
