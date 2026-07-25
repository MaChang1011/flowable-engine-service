package com.example.workflow.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WfProcessDef {
    private String id;
    private String processKey;
    private String processName;
    private Integer version;
    private String category;
    private String bpmnXml;
    private String applicableOrgs;
    private String formSchemaId;
    private Integer status;
    private String deployedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
