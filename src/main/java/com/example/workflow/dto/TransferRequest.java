package com.example.workflow.dto;

import lombok.Data;

@Data
public class TransferRequest {
    /** 被调岗的用户ID */
    private String userId;
    /** 目标机构ID */
    private String targetOrgId;
    /** 调岗原因 */
    private String reason;
}
