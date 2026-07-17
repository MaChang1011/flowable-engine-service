package com.example.workflow.dto;

import lombok.Data;
import java.util.Map;

@Data
public class CompleteTaskRequest {
    private String taskId;
    private Map<String, Object> variables;
    private String comment;
}
