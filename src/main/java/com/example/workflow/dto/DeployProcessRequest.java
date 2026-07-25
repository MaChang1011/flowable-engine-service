package com.example.workflow.dto;

import lombok.Data;
import java.util.Map;

@Data
public class DeployProcessRequest {
    private String processKey;
    private String processName;
    private String category;
    private String bpmnXml;
    private String applicableOrgs;
    private String formSchemaId;
}
