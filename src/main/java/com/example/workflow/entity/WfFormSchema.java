package com.example.workflow.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WfFormSchema {
    private String id;
    private String schemaName;
    private String schemaKey;
    private Integer schemaVersion;
    private String jsonSchema;
    private String uiSchema;
    private String fieldsConfig;
    private String applicableOrgs;
    private Integer status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
