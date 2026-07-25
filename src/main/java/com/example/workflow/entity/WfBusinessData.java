package com.example.workflow.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WfBusinessData {
    private String id;
    private String processInstanceId;
    private String businessKey;
    private String processKey;
    private String tenantId;
    private String applicantId;
    private String applicantOrgId;
    private String businessData;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
