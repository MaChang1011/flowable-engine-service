package com.example.workflow.dto.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ProcessInstanceVO {
    private String processInstanceId;
    private String processDefinitionKey;
    private String processDefinitionName;
    private String businessKey;
    private String tenantId;
    private String applicantId;
    private String applicantOrgId;
    private Boolean suspended;
    private String startTime;
    private String endTime;
    private Map<String, Object> variables;
    private List<Map<String, Object>> activities;
}
