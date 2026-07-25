package com.example.workflow.dto;

import lombok.Data;
import java.util.Map;

@Data
public class RejectTaskRequest {
    private String taskId;
    private String targetNodeId;
    private Map<String, Object> variables;
    private String comment;
}
