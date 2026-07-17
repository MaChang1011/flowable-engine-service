package com.example.workflow.dto;

import lombok.Data;

@Data
public class DelegateTaskRequest {
    private String taskId;
    private String newAssignee;
}
