package com.example.workflow.config;

import org.flowable.engine.*;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.spring.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Flowable 6.8 + Spring Boot 3.x 手动配置
 * （Flowable 6.8 的 spring-boot-starter 缺少 AutoConfiguration.imports，
 *  与 Spring Boot 3.x 不兼容，必须手动创建 beans）
 */
@Configuration
public class FlowableConfig {

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public SpringProcessEngineConfiguration springProcessEngineConfiguration(
            DataSource dataSource, PlatformTransactionManager transactionManager) {
        SpringProcessEngineConfiguration config = new SpringProcessEngineConfiguration();
        config.setDataSource(dataSource);
        config.setTransactionManager(transactionManager);
        config.setHistory(HistoryLevel.FULL.getKey());
        config.setDatabaseSchemaUpdate("true");
        config.setAsyncExecutorActivate(false);
        return config;
    }

    @Bean
    public ProcessEngine processEngine(SpringProcessEngineConfiguration config) {
        ProcessEngineFactoryBean factory = new ProcessEngineFactoryBean();
        factory.setProcessEngineConfiguration(config);
        try {
            return factory.getObject();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ProcessEngine", e);
        }
    }

    // 自动暴露所有 Flowable Services
    @Bean
    public RuntimeService runtimeService(ProcessEngine processEngine) {
        return processEngine.getRuntimeService();
    }

    @Bean
    public TaskService taskService(ProcessEngine processEngine) {
        return processEngine.getTaskService();
    }

    @Bean
    public HistoryService historyService(ProcessEngine processEngine) {
        return processEngine.getHistoryService();
    }

    @Bean
    public RepositoryService repositoryService(ProcessEngine processEngine) {
        return processEngine.getRepositoryService();
    }

    @Bean
    public ManagementService managementService(ProcessEngine processEngine) {
        return processEngine.getManagementService();
    }

    @Bean
    public IdentityService identityService(ProcessEngine processEngine) {
        return processEngine.getIdentityService();
    }

    @Bean
    public FormService formService(ProcessEngine processEngine) {
        return processEngine.getFormService();
    }
}
