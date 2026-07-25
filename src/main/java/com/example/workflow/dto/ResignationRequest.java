package com.example.workflow.dto;

import lombok.Data;

@Data
public class ResignationRequest {
    /** 离职用户ID */
    private String userId;
    /** 任务承接人ID（该用户的待办任务全部转给此人） */
    private String reassignToUserId;
    /** 离职原因 */
    private String reason;
}
