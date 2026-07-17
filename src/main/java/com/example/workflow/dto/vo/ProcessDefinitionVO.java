package com.example.workflow.dto.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ProcessDefinitionVO {
    private String id;
    private String key;
    private String name;
    private int version;
    private String deploymentId;
    private String resourceName;
    private String category;
    private boolean suspended;
    private String tenantId;
}
