-- =============================================
-- 多机构多部门流程引擎 - 数据库初始化脚本
-- Flowable 6.8 + Spring Boot 3.x
-- =============================================

-- 1. 机构表
CREATE TABLE IF NOT EXISTS sys_org (
    id VARCHAR(64) PRIMARY KEY COMMENT '机构ID',
    org_name VARCHAR(128) NOT NULL COMMENT '机构名称',
    parent_id VARCHAR(64) DEFAULT '0' COMMENT '父机构ID，0为根',
    org_level INT NOT NULL COMMENT '层级：1=集团,2=分公司,3=部门,4=小组',
    org_type VARCHAR(32) NOT NULL COMMENT '类型: GROUP/COMPANY/DEPT/GROUP',
    org_code VARCHAR(64) NOT NULL UNIQUE COMMENT '机构编码',
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1 COMMENT '1=启用,0=停用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_parent (parent_id),
    INDEX idx_type_level (org_type, org_level),
    INDEX idx_code (org_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机构表';

-- 2. 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id VARCHAR(64) PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(64) NOT NULL UNIQUE COMMENT '登录名',
    real_name VARCHAR(64) NOT NULL COMMENT '真实姓名',
    org_id VARCHAR(64) NOT NULL COMMENT '所属机构ID',
    role_ids VARCHAR(512) COMMENT '角色ID列表,逗号分隔',
    email VARCHAR(128),
    phone VARCHAR(32),
    status TINYINT DEFAULT 1 COMMENT '1=启用,0=停用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (org_id) REFERENCES sys_org(id),
    INDEX idx_org (org_id),
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 3. 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id VARCHAR(64) PRIMARY KEY COMMENT '角色ID',
    role_code VARCHAR(64) NOT NULL UNIQUE COMMENT '角色编码',
    role_name VARCHAR(128) NOT NULL COMMENT '角色名称',
    scope_type VARCHAR(32) NOT NULL DEFAULT 'SELF' COMMENT '权限范围: SELF=本级, DEPT=本部门及下级, ALL=全机构, CROSS=跨机构',
    scope_org_ids VARCHAR(512) COMMENT '跨机构时可指定范围,逗号分隔',
    description VARCHAR(256),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 4. 流程定义表
CREATE TABLE IF NOT EXISTS wf_process_def (
    id VARCHAR(64) PRIMARY KEY COMMENT 'ID',
    process_key VARCHAR(128) NOT NULL COMMENT '流程Key',
    process_name VARCHAR(128) NOT NULL COMMENT '流程名称',
    version INT DEFAULT 1 COMMENT '版本号',
    category VARCHAR(128) COMMENT '分类',
    bpmn_xml LONGTEXT COMMENT 'BPMN XML快照',
    applicable_orgs VARCHAR(512) COMMENT '适用机构ID列表,逗号分隔,空=全适用',
    form_schema_id VARCHAR(64) COMMENT '关联表单schema ID',
    status TINYINT DEFAULT 1 COMMENT '1=启用,0=停用',
    deployed_by VARCHAR(64) COMMENT '部署人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_key_ver (process_key, version),
    INDEX idx_key (process_key),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程定义表';

-- 5. 审批模板表
CREATE TABLE IF NOT EXISTS wf_approval_template (
    id VARCHAR(64) PRIMARY KEY COMMENT 'ID',
    process_key VARCHAR(128) NOT NULL COMMENT '流程Key',
    template_name VARCHAR(128) NOT NULL COMMENT '模板名称',
    rule_type VARCHAR(32) NOT NULL COMMENT '审批规则类型: HIERARCHY=逐级,FIXED=固定节点,DYNAMIC=动态',
    rule_config JSON NOT NULL COMMENT '审批规则配置JSON',
    status TINYINT DEFAULT 1,
    created_by VARCHAR(64),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_process (process_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批模板表';

-- 6. 表单Schema设计表
CREATE TABLE IF NOT EXISTS wf_form_schema (
    id VARCHAR(64) PRIMARY KEY COMMENT 'ID',
    schema_name VARCHAR(128) NOT NULL COMMENT '表单名称',
    schema_key VARCHAR(128) NOT NULL UNIQUE COMMENT '表单Key',
    schema_version INT DEFAULT 1 COMMENT '版本号',
    json_schema TEXT NOT NULL COMMENT 'JSON Schema定义',
    ui_schema TEXT COMMENT 'UI渲染配置(JSON)',
    fields_config TEXT COMMENT '字段级配置(显示/隐藏/必填/只读规则)',
    applicable_orgs VARCHAR(512) COMMENT '适用机构ID列表',
    status TINYINT DEFAULT 1 COMMENT '1=启用,0=停用',
    created_by VARCHAR(64),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_key (schema_key),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单Schema设计表';

-- 7. 流程实例业务扩展表（存业务数据）
CREATE TABLE IF NOT EXISTS wf_business_data (
    id VARCHAR(64) PRIMARY KEY COMMENT 'ID',
    process_instance_id VARCHAR(64) NOT NULL COMMENT 'Flowable流程实例ID',
    business_key VARCHAR(128) NOT NULL COMMENT '业务单号',
    process_key VARCHAR(128) NOT NULL COMMENT '流程Key',
    tenant_id VARCHAR(64) COMMENT '租户ID',
    applicant_id VARCHAR(64) COMMENT '申请人ID',
    applicant_org_id VARCHAR(64) COMMENT '申请人机构ID',
    business_data JSON COMMENT '业务数据JSON',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_proc_inst (process_instance_id),
    INDEX idx_business_key (business_key),
    INDEX idx_tenant (tenant_id),
    INDEX idx_applicant (applicant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程业务数据扩展表';

-- =============================================
-- 初始化示例数据
-- =============================================

-- 插入机构树
INSERT INTO sys_org (id, org_name, parent_id, org_level, org_type, org_code) VALUES
('ORG_ROOT', '集团总部', '0', 1, 'GROUP', 'GROUP_A'),
('ORG_HUADONG', '华东分公司', 'ORG_ROOT', 2, 'COMPANY', 'COMPANY_EAST'),
('ORG_HUANAN', '华南分公司', 'ORG_ROOT', 2, 'COMPANY', 'COMPANY_SOUTH'),
('ORG_HUADONG_TECH', '华东技术部', 'ORG_HUADONG', 3, 'DEPT', 'DEPT_EAST_TECH'),
('ORG_HUADONG_FINANCE', '华东财务部', 'ORG_HUADONG', 3, 'DEPT', 'DEPT_EAST_FINANCE'),
('ORG_HUADONG_TECH_FRONT', '前端组', 'ORG_HUADONG_TECH', 4, 'GROUP', 'GROUP_EAST_TECH_FRONT'),
('ORG_HUADONG_TECH_BACK', '后端组', 'ORG_HUADONG_TECH', 4, 'GROUP', 'GROUP_EAST_TECH_BACK'),
('ORG_HUANAN_TECH', '华南技术部', 'ORG_HUANAN', 3, 'DEPT', 'DEPT_SOUTH_TECH'),
('ORG_HUANAN_HR', '华南人事部', 'ORG_HUANAN', 3, 'DEPT', 'DEPT_SOUTH_HR');

-- 插入角色
INSERT INTO sys_role (id, role_code, role_name, scope_type) VALUES
('ROLE_001', 'EMPLOYEE', '普通员工', 'SELF'),
('ROLE_002', 'TEAM_LEADER', '组长', 'DEPT'),
('ROLE_003', 'DEPT_MANAGER', '部门经理', 'ALL'),
('ROLE_004', 'COMPANY_LEADER', '分公司总经理', 'ALL'),
('ROLE_005', 'GROUP_LEADER', '集团董事长', 'CROSS');

-- 插入示例用户
INSERT INTO sys_user (id, username, real_name, org_id, role_ids) VALUES
('USER_001', 'zhangsan', '张三', 'ORG_HUADONG_TECH_FRONT', 'ROLE_001'),
('USER_002', 'wangwu', '王五', 'ORG_HUADONG_TECH_FRONT', 'ROLE_002'),
('USER_003', 'zhaoliu', '赵六', 'ORG_HUADONG_TECH', 'ROLE_003'),
('USER_004', 'sunqi', '孙七', 'ORG_HUADONG', 'ROLE_004'),
('USER_005', 'zhouba', '周八', 'ORG_ROOT', 'ROLE_005');

-- 插入示例表单Schema
INSERT INTO wf_form_schema (id, schema_name, schema_key, json_schema) VALUES
('FORM_001', '请假申请单', 'leave_request', '{
  "type": "object",
  "required": ["applicant", "days", "reason"],
  "properties": {
    "applicant": {"type": "string", "title": "申请人", "widget": "text"},
    "days": {"type": "number", "title": "天数", "minimum": 0.5, "maximum": 30},
    "reason": {"type": "string", "title": "事由", "widget": "textarea"},
    "startDate": {"type": "string", "title": "开始日期", "widget": "date"},
    "endDate": {"type": "string", "title": "结束日期", "widget": "date"}
  }
}');

-- 插入示例流程定义
INSERT INTO wf_process_def (id, process_key, process_name, version, form_schema_id) VALUES
('PROC_001', 'leave_request', '请假审批流程', 1, 'FORM_001');
