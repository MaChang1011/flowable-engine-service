package com.example.workflow.service;

import com.example.workflow.entity.WfApprovalTemplate;
import com.example.workflow.mapper.ApprovalTemplateMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 动态审批链服务 — 根据业务条件自动匹配审批链
 *
 * 工作流程:
 * 1. 查 wf_approval_template 中该 process_key 的规则
 * 2. 按 rule_type 分组：AMOUNT(金额) → CONDITION(通用条件) → 默认链
 * 3. 用 RuleEngineService 逐个匹配 → 返回命中的审批链节点列表
 * 4. 审批链注入到流程变量 approvalChain，BPMN 中通过多实例或顺序节点消费
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalChainService {

    private final ApprovalTemplateMapper templateMapper;
    private final RuleEngineService ruleEngine;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 根据流程 key 和业务变量生成审批链
     *
     * @return 审批链节点列表，每个节点包含 nodeId, nodeName, assigneeExpr
     */
    public List<Map<String, Object>> buildChain(String processKey, Map<String, Object> variables) {
        List<WfApprovalTemplate> templates = templateMapper.selectByProcessKey(processKey);

        if (templates.isEmpty()) {
            log.debug("流程 {} 未配置审批模板，使用默认单节点审批", processKey);
            return defaultSingleChain("dept_approve", "部门经理审批");
        }

        // 按优先级匹配: AMOUNT > CONDITION > 默认
        for (WfApprovalTemplate tpl : templates) {
            List<Map<String, Object>> chain = ruleEngine.matchChain(tpl.getRuleConfig(), variables);
            if (chain != null && !chain.isEmpty()) {
                log.info("流程 {} 命中规则: template={}, chainSize={}",
                        processKey, tpl.getTemplateName(), chain.size());
                return chain;
            }
        }

        return defaultSingleChain("dept_approve", "部门经理审批");
    }

    /**
     * 创建默认单节点审批链
     */
    private List<Map<String, Object>> defaultSingleChain(String nodeId, String nodeName) {
        List<Map<String, Object>> chain = new ArrayList<>();
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("nodeId", nodeId);
        node.put("nodeName", nodeName);
        node.put("assigneeExpr", "${deptManager}");
        node.put("timeoutHours", 48);
        node.put("escalateTo", "${superior}");
        chain.add(node);
        return chain;
    }

    /**
     * 将审批链转换为流程变量（给 BPMN 多实例用）
     */
    public Map<String, Object> chainToVariables(List<Map<String, Object>> chain) {
        Map<String, Object> vars = new LinkedHashMap<>();
        // 序列化为 JSON 字符串，避免 Flowable 序列化复杂对象失败
        try {
            vars.put("approvalChain", objectMapper.writeValueAsString(chain));
        } catch (Exception e) {
            log.error("审批链序列化失败", e);
            vars.put("approvalChain", "[]");
        }

        // 提取每级审批人表达式
        List<String> assigneeExprs = new ArrayList<>();
        List<String> nodeNames = new ArrayList<>();
        for (Map<String, Object> node : chain) {
            assigneeExprs.add((String) node.getOrDefault("assigneeExpr", ""));
            nodeNames.add((String) node.getOrDefault("nodeName", ""));
        }
        vars.put("approvalAssignees", objectMapper.valueToTree(assigneeExprs).toString());
        vars.put("approvalNodeNames", objectMapper.valueToTree(nodeNames).toString());
        vars.put("approvalLevels", chain.size());
        vars.put("currentApprovalLevel", 0);

        return vars;
    }
}
