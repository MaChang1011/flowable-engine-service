package com.example.workflow.dto.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ProcessInstanceVO {
    private String id;
    private String processDefinitionId;
    private String processDefinitionKey;
    private String businessKey;
    private String tenantId;
    private boolean suspended;
    private boolean active;
    private LocalDateTime startTime;
}
