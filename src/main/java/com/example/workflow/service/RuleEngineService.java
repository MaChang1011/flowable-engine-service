package com.example.workflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 规则引擎 — 根据业务变量动态匹配审批链
 * 
 * 支持的操作符:
 *   eq / neq   — 等于/不等于
 *   gt / gte   — 大于/大于等于
 *   lt / lte   — 小于/小于等于
 *   in / notIn — 包含/不包含
 *   between    — 介于
 */
@Slf4j
@Service
public class RuleEngineService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 根据规则配置匹配审批链
     *
     * @param ruleConfigJson 规则配置 JSON
     * @param variables      流程变量
     * @return 匹配到的审批链节点列表，未匹配返回 null
     */
    public List<Map<String, Object>> matchChain(String ruleConfigJson, Map<String, Object> variables) {
        try {
            Map<String, Object> config = objectMapper.readValue(ruleConfigJson,
                    new TypeReference<Map<String, Object>>() {});

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> conditions = (List<Map<String, Object>>) config.get("conditions");
            if (conditions == null || conditions.isEmpty()) {
                return getDefaultChain(config);
            }

            // 按条件顺序匹配，命中第一个即返回
            for (Map<String, Object> condition : conditions) {
                if (evaluateCondition(condition, variables)) {
                    log.info("规则命中: field={}, operator={}, value={}",
                            condition.get("field"), condition.get("operator"), condition.get("value"));
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> chain = (List<Map<String, Object>>) condition.get("chain");
                    return chain;
                }
            }

            // 未命中任何条件，返回默认链
            return getDefaultChain(config);

        } catch (Exception e) {
            log.error("规则匹配失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 评估单个条件
     */
    private boolean evaluateCondition(Map<String, Object> condition, Map<String, Object> variables) {
        String field = (String) condition.get("field");
        String operator = (String) condition.get("operator");
        Object ruleValue = condition.get("value");

        Object actualValue = variables.get(field);
        if (actualValue == null) {
            return false;
        }

        try {
            switch (operator) {
                case "eq":  return compareTo(actualValue, ruleValue) == 0;
                case "neq": return compareTo(actualValue, ruleValue) != 0;
                case "gt":  return compareTo(actualValue, ruleValue) > 0;
                case "gte": return compareTo(actualValue, ruleValue) >= 0;
                case "lt":  return compareTo(actualValue, ruleValue) < 0;
                case "lte": return compareTo(actualValue, ruleValue) <= 0;
                case "in":  return evaluateIn(actualValue, ruleValue);
                case "notIn": return !evaluateIn(actualValue, ruleValue);
                case "between": return evaluateBetween(actualValue, ruleValue);
                default:
                    log.warn("未知操作符: {}", operator);
                    return false;
            }
        } catch (Exception e) {
            log.warn("条件评估失败: field={}, operator={}, error={}", field, operator, e.getMessage());
            return false;
        }
    }

    /**
     * 数值比较（自动处理 Number 和 String）
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private int compareTo(Object a, Object b) {
        double da = toDouble(a);
        double db = toDouble(b);
        return Double.compare(da, db);
    }

    private double toDouble(Object val) {
        if (val instanceof Number n) return n.doubleValue();
        return Double.parseDouble(val.toString());
    }

    private boolean evaluateIn(Object actual, Object ruleValue) {
        if (ruleValue instanceof List<?> list) {
            return list.stream().anyMatch(v -> v.toString().equals(actual.toString()));
        }
        return actual.toString().equals(ruleValue.toString());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean evaluateBetween(Object actual, Object ruleValue) {
        if (ruleValue instanceof List<?> list && list.size() == 2) {
            double min = toDouble(list.get(0));
            double max = toDouble(list.get(1));
            double val = toDouble(actual);
            return val >= min && val <= max;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getDefaultChain(Map<String, Object> config) {
        Object defaultChain = config.get("defaultChain");
        if (defaultChain instanceof List) {
            return (List<Map<String, Object>>) defaultChain;
        }
        return Collections.emptyList();
    }
}
