package com.example.workflow.dto.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class TaskVO {
    private String taskId;
    private String taskName;
    private String assignee;
    private String processInstanceId;
    private String processDefinitionKey;
    private String processDefinitionId;
    private String businessKey;
    private String createTime;
    private String dueDate;
    private String formKey;
    private String tenantId;
    private String currentActivityName;
    private Map<String, Object> variables;
}
