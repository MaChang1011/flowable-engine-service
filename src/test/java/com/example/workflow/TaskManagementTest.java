package com.example.workflow;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 需要 MySQL 才能运行这些集成测试
 * 本地开发时：mvn test -Dtest=TaskManagementTest
 */
@SpringBootTest
@ActiveProfiles("test")
class TaskManagementTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @BeforeEach
    void deployTestData() {
        repositoryService.createDeployment()
                .addClasspathResource("processes/leave.bpmn20.xml")
                .tenantId("ORG_TENANT_001")
                .deploy();
    }

    @Test
    void testTaskQueryAfterStart() {
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("leave_request")
                .businessKey("TASK_TEST_001")
                .tenantId("ORG_TENANT_001")
                .variables(Map.of(
                    "applicant", "zhangsan",
                    "manager", "lisi",
                    "director", "wangwu",
                    "days", 2
                ))
                .start();

        List<Task> tasks = taskService.createTaskQuery()
                .taskTenantId("ORG_TENANT_001")
                .list();

        assertTrue(tasks.size() > 0, "应该至少有一个待办任务");
    }

    @Test
    void testCompleteTask() {
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("leave_request")
                .businessKey("COMPLETE_TEST_001")
                .tenantId("ORG_TENANT_001")
                .variables(Map.of(
                    "applicant", "zhangsan",
                    "manager", "lisi",
                    "days", 2
                ))
                .start();

        Task firstTask = taskService.createTaskQuery().singleResult();
        assertNotNull(firstTask);

        taskService.complete(firstTask.getId(), Map.of("approved", true));

        Task completedTask = taskService.createTaskQuery()
                .taskId(firstTask.getId())
                .singleResult();
        assertNull(completedTask, "完成任务后该任务应被移除");
    }
}
