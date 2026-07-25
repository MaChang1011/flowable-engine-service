package com.example.workflow.service;

import com.example.workflow.security.PermissionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Flowable 查询助手 — 按机构权限自动注入 org 过滤条件
 * 
 * 核心原理：每个流程实例启动时写入 applicantOrgId 变量，
 * 查询时按当前用户的 accessibleOrgIds 过滤，实现数据隔离。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FlowableQueryHelper {

    private static final String ORG_VAR = "applicantOrgId";

    /**
     * 给 TaskQuery 加上 org 权限过滤
     * 只返回 applicantOrgId 在用户可访问机构列表中的任务
     */
    public void applyOrgFilter(TaskQuery query) {
        List<String> orgIds = getAccessibleOrgs();
        if (orgIds == null || orgIds.isEmpty()) return;

        if (orgIds.size() == 1) {
            query.processVariableValueEquals(ORG_VAR, orgIds.get(0));
            return;
        }
        query.or();
        for (String orgId : orgIds) {
            query.processVariableValueEquals(ORG_VAR, orgId);
        }
        query.endOr();
    }

    /**
     * 给 ProcessInstanceQuery 加上 org 权限过滤
     */
    public void applyOrgFilter(ProcessInstanceQuery query) {
        List<String> orgIds = getAccessibleOrgs();
        if (orgIds == null || orgIds.isEmpty()) return;

        if (orgIds.size() == 1) {
            query.variableValueEquals(ORG_VAR, orgIds.get(0));
            return;
        }
        query.or();
        for (String orgId : orgIds) {
            query.variableValueEquals(ORG_VAR, orgId);
        }
        query.endOr();
    }

    /**
     * 给 HistoricProcessInstanceQuery 加上 org 权限过滤
     */
    public void applyOrgFilter(HistoricProcessInstanceQuery query) {
        List<String> orgIds = getAccessibleOrgs();
        if (orgIds == null || orgIds.isEmpty()) return;

        if (orgIds.size() == 1) {
            query.variableValueEquals(ORG_VAR, orgIds.get(0));
            return;
        }
        query.or();
        for (String orgId : orgIds) {
            query.variableValueEquals(ORG_VAR, orgId);
        }
        query.endOr();
    }

    /**
     * 给 HistoricTaskInstanceQuery 加上 org 权限过滤
     */
    public void applyOrgFilter(HistoricTaskInstanceQuery query) {
        List<String> orgIds = getAccessibleOrgs();
        if (orgIds == null || orgIds.isEmpty()) return;

        if (orgIds.size() == 1) {
            query.processVariableValueEquals(ORG_VAR, orgIds.get(0));
            return;
        }
        query.or();
        for (String orgId : orgIds) {
            query.processVariableValueEquals(ORG_VAR, orgId);
        }
        query.endOr();
    }

    // ===== 兼容旧方法名 =====
    /** @deprecated 使用 applyOrgFilter */
    @Deprecated
    public void applyTenantFilter(TaskQuery query) { applyOrgFilter(query); }
    /** @deprecated 使用 applyOrgFilter */
    @Deprecated
    public void applyTenantFilter(ProcessInstanceQuery query) { applyOrgFilter(query); }
    /** @deprecated 使用 applyOrgFilter */
    @Deprecated
    public void applyTenantFilter(HistoricProcessInstanceQuery query) { applyOrgFilter(query); }
    /** @deprecated 使用 applyOrgFilter */
    @Deprecated
    public void applyTenantFilter(HistoricTaskInstanceQuery query) { applyOrgFilter(query); }

    private List<String> getAccessibleOrgs() {
        List<String> orgIds = PermissionContext.getAccessibleOrgIds();
        if (orgIds != null && !orgIds.isEmpty()) {
            log.debug("权限过滤: applicableOrgs={}", orgIds);
        }
        return orgIds;
    }
}
