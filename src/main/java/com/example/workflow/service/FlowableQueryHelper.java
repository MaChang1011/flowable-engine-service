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
 * Flowable查询助手 — 自动注入租户/机构过滤条件
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FlowableQueryHelper {

    private final OrgService orgService;

    public void applyTenantFilter(ProcessInstanceQuery query) {
        injectOrgFilter(query);
    }

    public void applyTenantFilter(TaskQuery query) {
        injectOrgFilter(query);
    }

    public void applyTenantFilter(HistoricProcessInstanceQuery query) {
        injectOrgFilter(query);
    }

    public void applyTenantFilter(HistoricTaskInstanceQuery query) {
        injectOrgFilter(query);
    }

    @SuppressWarnings("unchecked")
    private void injectOrgFilter(Object query) {
        List<String> accessibleOrgIds = PermissionContext.getAccessibleOrgIds();
        if (accessibleOrgIds == null || accessibleOrgIds.isEmpty()) return;

        if (accessibleOrgIds.size() == 1) {
            String tid = accessibleOrgIds.get(0);
            if (query instanceof ProcessInstanceQuery q) q.processInstanceTenantId(tid);
            else if (query instanceof TaskQuery q) q.taskTenantId(tid);
            else if (query instanceof HistoricProcessInstanceQuery q) q.processInstanceTenantId(tid);
            else if (query instanceof HistoricTaskInstanceQuery q) q.taskTenantId(tid);
            return;
        }

        if (query instanceof ProcessInstanceQuery q) {
            q.or().processInstanceTenantId(accessibleOrgIds.get(0));
            for (int i = 1; i < accessibleOrgIds.size(); i++) q.or().processInstanceTenantId(accessibleOrgIds.get(i));
            q.endOr();
        } else if (query instanceof TaskQuery q) {
            q.or().taskTenantId(accessibleOrgIds.get(0));
            for (int i = 1; i < accessibleOrgIds.size(); i++) q.or().taskTenantId(accessibleOrgIds.get(i));
            q.endOr();
        } else if (query instanceof HistoricProcessInstanceQuery q) {
            q.or().processInstanceTenantId(accessibleOrgIds.get(0));
            for (int i = 1; i < accessibleOrgIds.size(); i++) q.or().processInstanceTenantId(accessibleOrgIds.get(i));
            q.endOr();
        } else if (query instanceof HistoricTaskInstanceQuery q) {
            q.or().taskTenantId(accessibleOrgIds.get(0));
            for (int i = 1; i < accessibleOrgIds.size(); i++) q.or().taskTenantId(accessibleOrgIds.get(i));
            q.endOr();
        }
    }
}
