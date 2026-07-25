package com.example.workflow.dto;

import lombok.Data;
import java.util.Map;

@Data
public class CreateFormSchemaRequest {
    private String schemaName;
    private String schemaKey;
    private Integer schemaVersion;
    private String jsonSchema;
    private String uiSchema;
    private String fieldsConfig;
    private String applicableOrgs;
}
