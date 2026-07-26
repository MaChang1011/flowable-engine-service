import { useState, useEffect } from 'react';
import {
  Table, Button, Modal, Form, Input, Space, message, Tag, Popconfirm,
  Typography, Select, Switch, InputNumber,
} from 'antd';
import {
  PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined,
} from '@ant-design/icons';

const { Title } = Typography;
const { TextArea } = Input;
const { Option } = Select;

interface TemplateRecord {
  id: string;
  templateName?: string;
  name?: string;
  processKey?: string;
  ruleType?: string;
  ruleConfig?: any;
  status?: number;
  createTime?: string;
  updateTime?: string;
}

const ruleTypeOptions = [
  { label: '层级审批', value: 'HIERARCHY' },
  { label: '固定人员', value: 'FIXED' },
  { label: '动态规则', value: 'DYNAMIC' },
];

const ApprovalTemplate = () => {
  const [data, setData] = useState<TemplateRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form] = Form.useForm();

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const res = await fetch('/api/wf/template/list', {
        headers: { Authorization: `Bearer ${localStorage.getItem('token') || ''}` },
      });
      const json = await res.json();
      const list = json.data || [];
      setData(list);
    } catch (error: any) {
      console.error(error);
      message.error('加载审批模板失败');
    } finally {
      setLoading(false);
    }
  };

  // ===== Open Create Modal =====
  const openCreate = () => {
    setEditingId(null);
    form.resetFields();
    form.setFieldsValue({
      status: 1,
      ruleConfig: '{}',
    });
    setModalVisible(true);
  };

  // ===== Open Edit Modal =====
  const openEdit = (record: TemplateRecord) => {
    setEditingId(record.id);
    form.resetFields();
    form.setFieldsValue({
      templateName: record.templateName || record.name,
      processKey: record.processKey,
      ruleType: record.ruleType,
      ruleConfig: typeof record.ruleConfig === 'string'
        ? record.ruleConfig
        : JSON.stringify(record.ruleConfig, null, 2),
      status: record.status,
    });
    setModalVisible(true);
  };

  // ===== Submit =====
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      const payload = {
        name: values.templateName,
        processKey: values.processKey,
        ruleType: values.ruleType,
        ruleConfig: typeof values.ruleConfig === 'string'
          ? values.ruleConfig
          : values.ruleConfig,
        status: values.status ?? 1,
      };

      let res: Response;
      if (editingId) {
        res = await fetch(`/api/wf/template/${editingId}`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${localStorage.getItem('token') || ''}`,
          },
          body: JSON.stringify(payload),
        });
      } else {
        res = await fetch('/api/wf/template', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${localStorage.getItem('token') || ''}`,
          },
          body: JSON.stringify(payload),
        });
      }

      if (!res.ok) throw new Error('操作失败');
      message.success(editingId ? '编辑成功' : '创建成功');
      setModalVisible(false);
      loadData();
    } catch (error: any) {
      if (error.name !== 'Error') { /* form validation */ }
      else { message.error(error.message || '操作失败'); }
    }
  };

  // ===== Toggle Status =====
  const handleToggleStatus = async (record: TemplateRecord) => {
    const newStatus = record.status === 1 ? 0 : 1;
    try {
      const res = await fetch(`/api/wf/template/${record.id}/toggle`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${localStorage.getItem('token') || ''}`,
        },
        body: JSON.stringify({ status: newStatus }),
      });
      if (!res.ok) throw new Error('切换失败');
      message.success(newStatus === 1 ? '已启用' : '已禁用');
      loadData();
    } catch (error: any) {
      message.error(error.message || '切换状态失败');
    }
  };

  // ===== Delete =====
  const handleDelete = async (record: TemplateRecord) => {
    try {
      const res = await fetch(`/api/wf/template/${record.id}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${localStorage.getItem('token') || ''}` },
      });
      if (!res.ok) throw new Error('删除失败');
      message.success('已删除');
      loadData();
    } catch (error: any) {
      message.error(error.message || '删除失败');
    }
  };

  const getStatusTag = (status: number) =>
    status === 1 ? <Tag color="green">启用</Tag> : <Tag color="red">禁用</Tag>;

  const columns = [
    {
      title: '模板名称',
      dataIndex: 'templateName',
      key: 'templateName',
      width: 180,
      ellipsis: true,
      render: (text: string, record: TemplateRecord) => text || record.name || '-',
    },
    {
      title: '流程 Key',
      dataIndex: 'processKey',
      key: 'processKey',
      width: 160,
      render: (text: string) => <Tag color="blue">{text || '-'}</Tag>,
    },
    {
      title: '规则类型',
      dataIndex: 'ruleType',
      key: 'ruleType',
      width: 120,
      render: (text: string) => {
        const opt = ruleTypeOptions.find((o) => o.value === text);
        return opt ? <Tag>{opt.label}</Tag> : <Tag>{text || '-'}</Tag>;
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (s: number) => getStatusTag(s),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
      render: (t: string) => t ? new Date(t).toLocaleString('zh-CN') : '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 280,
      fixed: 'right' as const,
      render: (_: any, record: TemplateRecord) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEdit(record)}>
            编辑
          </Button>
          <Switch
            size="small"
            checked={record.status === 1}
            checkedChildren="启用"
            unCheckedChildren="禁用"
            onChange={() => handleToggleStatus(record)}
          />
          <Popconfirm
            title="确定删除该模板？"
            onConfirm={() => handleDelete(record)}
            okText="确认"
            cancelText="取消"
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>审批模板管理</Title>

      {/* Create Button */}
      <div style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          创建模板
        </Button>
      </div>

      <Table
        columns={columns}
        dataSource={data}
        loading={loading}
        rowKey="id"
        scroll={{ x: 1200 }}
        pagination={{ pageSize: 20 }}
      />

      {/* ===== Create/Edit Modal ===== */}
      <Modal
        title={editingId ? '编辑模板' : '创建模板'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        okText={editingId ? '保存' : '创建'}
        cancelText="取消"
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="模板名称"
            name="templateName"
            rules={[{ required: true, message: '请输入模板名称' }]}
          >
            <Input placeholder="例如：报销审批模板" />
          </Form.Item>
          <Form.Item
            label="流程定义 Key"
            name="processKey"
            rules={[{ required: true, message: '请输入流程定义 Key' }]}
          >
            <Input placeholder="例如：reimbursement_approval" />
          </Form.Item>
          <Form.Item
            label="规则类型"
            name="ruleType"
            rules={[{ required: true, message: '请选择规则类型' }]}
          >
            <Select placeholder="请选择规则类型">
              {ruleTypeOptions.map((opt) => (
                <Option key={opt.value} value={opt.value}>{opt.label}</Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item
            label="规则配置 (JSON)"
            name="ruleConfig"
            tooltip="以 JSON 格式配置审批规则"
          >
            <TextArea rows={6} placeholder={`{\n  "approvers": ["user1", "user2"],\n  "condition": "amount > 1000"\n}`} />
          </Form.Item>
          <Form.Item label="状态" name="status" initialValue={1}>
            <Select>
              <Select.Option value={1}>启用</Select.Option>
              <Select.Option value={0}>禁用</Select.Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default ApprovalTemplate;
