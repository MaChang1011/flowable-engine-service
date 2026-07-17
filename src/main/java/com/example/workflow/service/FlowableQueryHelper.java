package com.example.workflow.service;

import com.example.workflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.task.api.TaskQuery;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FlowableQueryHelper {

    private final OrgService orgService;

    public void applyTenantFilter(ProcessInstanceQuery query) {
        injectTenantFilter(query, SecurityUtils.getCurrentTenantId());
    }

    public void applyTenantFilter(TaskQuery query) {
        injectTenantFilter(query, SecurityUtils.getCurrentTenantId());
    }

    public void applyTenantFilter(HistoricProcessInstanceQuery query) {
        injectTenantFilter(query, SecurityUtils.getCurrentTenantId());
    }

    public void applyTenantFilter(HistoricTaskInstanceQuery query) {
        injectTenantFilter(query, SecurityUtils.getCurrentTenantId());
    }

    @SuppressWarnings("unchecked")
    private void injectTenantFilter(Object query, String currentTenantId) {
        if (!StringUtils.hasText(currentTenantId)) {
            return;
        }
        List<String> allowedTenants = orgService.getAccessibleTenantIds(currentTenantId);
        if (allowedTenants.isEmpty()) {
            return;
        }

        // Flowable 7.x: single tenant = direct filter, multiple = OR chain
        if (allowedTenants.size() == 1) {
            String tid = allowedTenants.get(0);
            if (query instanceof ProcessInstanceQuery q) q.processInstanceTenantId(tid);
            else if (query instanceof TaskQuery q) q.taskTenantId(tid);
            else if (query instanceof HistoricProcessInstanceQuery q) q.processInstanceTenantId(tid);
            else if (query instanceof HistoricTaskInstanceQuery q) q.taskTenantId(tid);
            return;
        }

        // Multiple tenants: build OR chain
        if (query instanceof ProcessInstanceQuery q) {
            q.or().processInstanceTenantId(allowedTenants.get(0));
            for (int i = 1; i < allowedTenants.size(); i++) q.or().processInstanceTenantId(allowedTenants.get(i));
            q.endOr();
        } else if (query instanceof TaskQuery q) {
            q.or().taskTenantId(allowedTenants.get(0));
            for (int i = 1; i < allowedTenants.size(); i++) q.or().taskTenantId(allowedTenants.get(i));
            q.endOr();
        } else if (query instanceof HistoricProcessInstanceQuery q) {
            q.or().processInstanceTenantId(allowedTenants.get(0));
            for (int i = 1; i < allowedTenants.size(); i++) q.or().processInstanceTenantId(allowedTenants.get(i));
            q.endOr();
        } else if (query instanceof HistoricTaskInstanceQuery q) {
            q.or().taskTenantId(allowedTenants.get(0));
            for (int i = 1; i < allowedTenants.size(); i++) q.or().taskTenantId(allowedTenants.get(i));
            q.endOr();
        }
    }
}
