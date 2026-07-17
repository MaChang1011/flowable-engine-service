package com.example.workflow.dto.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskVO {
    private String id;
    private String name;
    private String description;
    private String assignee;
    private List<String> candidateGroups;
    private List<String> candidateUsers;
    private String processInstanceId;
    private String processDefinitionKey;
    private String tenantId;
    private LocalDateTime createTime;
    private LocalDateTime dueDate;
}
