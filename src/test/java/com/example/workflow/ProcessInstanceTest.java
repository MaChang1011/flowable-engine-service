package com.example.workflow;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 需要 MySQL 才能运行这些集成测试
 */
@SpringBootTest
@ActiveProfiles("test")
class ProcessInstanceTest {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private RepositoryService repositoryService;

    @BeforeEach
    void deployTestData() {
        repositoryService.createDeployment()
                .addClasspathResource("processes/leave.bpmn20.xml")
                .tenantId("ORG_TENANT_001")
                .deploy();
    }

    @Test
    void testStartProcessInstance() {
        Map<String, Object> variables = Map.of(
            "applicant", "zhangsan",
            "manager", "lisi",
            "director", "wangwu",
            "days", 2,
            "reason", "家里有事"
        );

        ProcessInstance instance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("leave_request")
                .businessKey("TEST_ORDER_001")
                .tenantId("ORG_TENANT_001")
                .variables(variables)
                .start();

        assertNotNull(instance.getId());
        assertEquals("leave_request", instance.getProcessDefinitionKey());
        assertEquals("TEST_ORDER_001", instance.getBusinessKey());
        assertEquals("ORG_TENANT_001", instance.getTenantId());
        assertFalse(instance.isSuspended());
    }

    @Test
    void testQueryByBusinessKey() {
        ProcessInstance instance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("leave_request")
                .businessKey("QUERY_TEST_001")
                .tenantId("ORG_TENANT_001")
                .start();

        ProcessInstance found = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey("QUERY_TEST_001")
                .singleResult();

        assertNotNull(found);
        assertEquals(instance.getId(), found.getId());
    }
}
