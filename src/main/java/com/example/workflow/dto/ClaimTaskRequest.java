package com.example.workflow.dto;

import lombok.Data;

@Data
public class ClaimTaskRequest {
    private String taskId;
    private String assignee;
}
