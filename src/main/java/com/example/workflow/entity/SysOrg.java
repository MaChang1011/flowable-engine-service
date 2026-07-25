package com.example.workflow.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysOrg {
    private String id;
    private String orgName;
    private String parentId;
    private Integer orgLevel;
    private String orgType;
    private String orgCode;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
