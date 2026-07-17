-- 机构表（与 Flowable 无关，业务系统自建）
CREATE TABLE IF NOT EXISTS sys_org (
    id VARCHAR(64) PRIMARY KEY COMMENT '机构ID',
    name VARCHAR(128) NOT NULL COMMENT '机构名称',
    parent_id VARCHAR(64) COMMENT '父机构ID',
    tenant_id VARCHAR(64) NOT NULL UNIQUE COMMENT '对应 Flowable 租户ID',
    level INT DEFAULT 1 COMMENT '层级深度',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES sys_org(id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织架构表';

-- 插入示例数据
INSERT INTO sys_org (id, name, parent_id, tenant_id, level, sort_order) VALUES
('ORG_001', '总公司',       NULL,       'ORG_TENANT_001', 1, 1),
('ORG_002', '华东分公司',   'ORG_001',  'ORG_TENANT_002', 2, 1),
('ORG_003', '上海办事处',   'ORG_002',  'ORG_TENANT_003', 3, 1),
('ORG_004', '华南分公司',   'ORG_001',  'ORG_TENANT_004', 2, 2),
('ORG_005', '深圳办事处',   'ORG_004',  'ORG_TENANT_005', 3, 2);
