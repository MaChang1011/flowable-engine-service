package com.example.workflow.dto;

import lombok.Data;

@Data
public class OrgCreateRequest {
    private String id;
    private String orgName;
    private String parentId;
    private String orgType;
    private String orgCode;
    private Integer sortOrder;
}
