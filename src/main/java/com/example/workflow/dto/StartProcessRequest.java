package com.example.workflow.dto;

import lombok.Data;
import java.util.Map;

@Data
public class StartProcessRequest {
    private String processDefinitionKey;
    private String businessKey;
    private String tenantId;
    private String applicantId;
    private String applicantOrgId;
    private Map<String, Object> variables;
}
