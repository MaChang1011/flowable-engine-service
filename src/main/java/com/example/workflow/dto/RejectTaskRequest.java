package com.example.workflow.dto;

import lombok.Data;
import java.util.Map;

/**
 * 驳回任务请求
 */
@Data
public class RejectTaskRequest {
    /** 任务ID */
    private String taskId;
    
    /** 目标节点ID（如 "apply"、"manager_approve"）*/
    private String targetNodeId;
    
    /** 驳回时携带的流程变量 */
    private Map<String, Object> variables;
    
    /** 驳回原因/备注 */
    private String comment;
}
