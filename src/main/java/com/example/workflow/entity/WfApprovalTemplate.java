package com.example.workflow.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审批模板 — 定义条件驱动的动态审批链规则
 *
 * rule_config JSON 示例:
 * {
 *   "conditions": [
 *     {
 *       "field": "amount",
 *       "operator": "gt",
 *       "value": 100000,
 *       "chain": [
 *         {"nodeId": "dept_approve", "nodeName": "部门经理", "assigneeExpr": "${deptManager}"},
 *         {"nodeId": "finance_approve", "nodeName": "财务总监", "assigneeExpr": "finance_director"},
 *         {"nodeId": "ceo_approve", "nodeName": "总经理", "assigneeExpr": "ceo"}
 *       ]
 *     },
 *     {
 *       "field": "amount",
 *       "operator": "gt",
 *       "value": 10000,
 *       "chain": [
 *         {"nodeId": "dept_approve", "nodeName": "部门经理"},
 *         {"nodeId": "hr_approve", "nodeName": "人事审批"}
 *       ]
 *     }
 *   ],
 *   "defaultChain": [
 *     {"nodeId": "dept_approve", "nodeName": "部门经理"}
 *   ]
 * }
 */
@Data
public class WfApprovalTemplate {
    private String id;
    private String processKey;
    private String templateName;
    private String ruleType;
    private String ruleConfig;
    private Integer status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
