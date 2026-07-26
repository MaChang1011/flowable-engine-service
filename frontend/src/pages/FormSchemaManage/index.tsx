import { useState, useEffect } from 'react';
import {
  Table, Button, Modal, Form, Input, Space, message, Tag, Select,
  Descriptions, Popconfirm, Typography,
} from 'antd';
import {
  PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import {
  listForms, createForm, updateForm, deleteForm, getFormFields,
} from '@/api/formSchema';
import type { FormSchema } from '@/api/formSchema';

const { Title } = Typography;
const { Option } = Select;

const FormSchemaManage = () => {
  const [data, setData] = useState<FormSchema[]>([]);
  const [loading, setLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState<number | undefined>(undefined);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);

  // Create modal
  const [createVisible, setCreateVisible] = useState(false);
  const [createForm] = Form.useForm();

  // Edit modal
  const [editVisible, setEditVisible] = useState(false);
  const [editForm] = Form.useForm();
  const [editingRecord, setEditingRecord] = useState<FormSchema | null>(null);

  // Fields modal
  const [fieldsVisible, setFieldsVisible] = useState(false);
  const [fieldsData, setFieldsData] = useState<any[]>([]);
  const [fieldsLoading, setFieldsLoading] = useState(false);

  // Delete confirm
  const [deleteConfirm, setDeleteConfirm] = useState<string | null>(null);

  useEffect(() => {
    loadData();
  }, [statusFilter, page]);

  const loadData = async () => {
    setLoading(true);
    try {
      const res = await listForms(statusFilter);
      const list = res.data || [];
      setData(list);
      setTotal(list.length);
    } catch (error: any) {
      console.error(error);
      message.error('加载表单列表失败');
    } finally {
      setLoading(false);
    }
  };

  // ===== Create =====
  const openCreate = () => {
    createForm.resetFields();
    setCreateVisible(true);
  };

  const handleCreate = async () => {
    try {
      const values = await createForm.validateFields();
      const payload: FormSchema = {
        name: values.name,
        schemaKey: values.key,
        schema: JSON.parse(values.jsonSchema),
        status: values.status,
      };
      await createForm(payload);
      message.success('创建成功');
      setCreateVisible(false);
      loadData();
    } catch (error: any) {
      if (error.name !== 'Error') { /* form validation */ }
      else { message.error(error.message || '创建失败'); }
    }
  };

  // ===== Edit =====
  const openEdit = async (record: FormSchema) => {
    setEditingRecord(record);
    try {
      const res = await listForms();
      const full = res.data?.find((f: FormSchema) => f.id === record.id) || record;
      editForm.setFieldsValue({
        name: full.name,
        key: full.schemaKey,
        jsonSchema: JSON.stringify(full.schema, null, 2),
        uiSchema: '{}',
        fieldsConfig: JSON.stringify(full.schema?.properties || {}, null, 2),
        status: full.status,
      });
      setEditVisible(true);
    } catch (e) {
      editForm.setFieldsValue({
        name: record.name,
        key: record.schemaKey,
        jsonSchema: JSON.stringify(record.schema, null, 2),
        uiSchema: '{}',
        fieldsConfig: JSON.stringify(record.schema?.properties || {}, null, 2),
        status: record.status,
      });
      setEditVisible(true);
    }
  };

  const handleEdit = async () => {
    try {
      const values = await editForm.validateFields();
      if (!editingRecord) return;
      const payload: FormSchema = {
        id: editingRecord.id,
        name: values.name,
        schemaKey: values.key,
        schema: JSON.parse(values.jsonSchema),
        status: values.status,
      };
      await updateForm(editingRecord.id, payload);
      message.success('更新成功');
      setEditVisible(false);
      loadData();
    } catch (error: any) {
      if (error.name !== 'Error') { /* form validation */ }
      else { message.error(error.message || '更新失败'); }
    }
  };

  // ===== Delete =====
  const handleDelete = async () => {
    if (!deleteConfirm) return;
    try {
      await deleteForm(deleteConfirm);
      message.success('删除成功');
      setDeleteConfirm(null);
      loadData();
    } catch (error: any) {
      message.error(error.message || '删除失败');
    }
  };

  // ===== View Fields =====
  const openFields = async (record: FormSchema) => {
    setFieldsLoading(true);
    setFieldsVisible(true);
    try {
      const res = await getFormFields(record.id);
      setFieldsData(res.data || []);
    } catch (error: any) {
      console.error(error);
      message.warning('获取字段信息失败');
      setFieldsData([]);
    } finally {
      setFieldsLoading(false);
    }
  };

  const statusMap: Record<number, { text: string; color: string }> = {
    1: { text: '启用', color: 'green' },
    0: { text: '停用', color: 'red' },
  };

  const columns = [
    {
      title: '表单名称',
      dataIndex: 'name',
      key: 'name',
      width: 200,
      ellipsis: true,
    },
    {
      title: '表单 Key',
      dataIndex: 'schemaKey',
      key: 'schemaKey',
      width: 180,
      render: (text: string) => <Tag color="blue">{text || '-'}</Tag>,
    },
    {
      title: '版本号',
      dataIndex: 'version',
      key: 'version',
      width: 100,
      render: (v: any) => v ? `v${v}` : '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (s: number) => {
        const info = statusMap[s];
        return info ? <Tag color={info.color}>{info.text}</Tag> : '-';
      },
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
      width: 180,
      render: (t: string) => t ? new Date(t).toLocaleString('zh-CN') : '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 280,
      fixed: 'right' as const,
      render: (_: any, record: FormSchema) => (
        <Space size="small">
          <Button
            size="small"
            icon={<EditOutlined />}
            onClick={() => openEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确认删除该表单？"
            onConfirm={handleDelete}
            onCancel={() => setDeleteConfirm(null)}
            okText="确认"
            cancelText="取消"
          >
            <Button
              size="small"
              danger
              icon={<DeleteOutlined />}
              onClick={() => setDeleteConfirm(record.id)}
            >
              删除
            </Button>
          </Popconfirm>
          <Button
            size="small"
            icon={<EyeOutlined />}
            onClick={() => openFields(record)}
          >
            查看字段
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>表单设计</Title>

      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Space>
          <span>状态筛选：</span>
          <Select
            value={statusFilter}
            placeholder="全部"
            allowClear
            style={{ width: 120 }}
            onChange={(val) => { setStatusFilter(val); setPage(1); }}
          >
            <Option value={1}>启用</Option>
            <Option value={0}>停用</Option>
          </Select>
          <Button icon={<ReloadOutlined />} onClick={loadData}>刷新</Button>
        </Space>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          创建表单
        </Button>
      </div>

      <Table
        columns={columns}
        dataSource={data}
        loading={loading}
        rowKey="id"
        scroll={{ x: 1000 }}
        pagination={{
          current: page,
          total,
          pageSize: 20,
          onChange: (p) => setPage(p),
          showSizeChanger: false,
        }}
      />

      {/* ===== Create Modal ===== */}
      <Modal
        title="创建表单"
        open={createVisible}
        onOk={handleCreate}
        onCancel={() => setCreateVisible(false)}
        okText="创建"
        cancelText="取消"
        width={700}
      >
        <Form form={createForm} layout="vertical">
          <Form.Item
            label="表单名称"
            name="name"
            rules={[{ required: true, message: '请输入表单名称' }]}
          >
            <Input placeholder="例如：请假申请表" />
          </Form.Item>
          <Form.Item
            label="表单 Key"
            name="key"
            rules={[{ required: true, message: '请输入表单 Key' }]}
          >
            <Input placeholder="例如：leave_application" />
          </Form.Item>
          <Form.Item
            label="JSON Schema"
            name="jsonSchema"
            rules={[
              { required: true, message: '请输入 JSON Schema' },
              {
                validator: (_, value) => {
                  if (value) {
                    try { JSON.parse(value); return Promise.resolve(); }
                    catch { return Promise.reject(new Error('无效的 JSON')); }
                  }
                  return Promise.resolve();
                },
              },
            ]}
          >
            <Input.TextArea rows={8} placeholder='{"type": "object", "properties": {...}}' />
          </Form.Item>
          <Form.Item label="UI Schema" name="uiSchema">
            <Input.TextArea rows={4} placeholder='{"ui:order": ["name", "date"]}' />
          </Form.Item>
          <Form.Item label="状态" name="status" initialValue={1}>
            <Select>
              <Option value={1}>启用</Option>
              <Option value={0}>停用</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      {/* ===== Edit Modal ===== */}
      <Modal
        title="编辑表单"
        open={editVisible}
        onOk={handleEdit}
        onCancel={() => setEditVisible(false)}
        okText="保存"
        cancelText="取消"
        width={700}
      >
        <Form form={editForm} layout="vertical">
          <Form.Item
            label="表单名称"
            name="name"
            rules={[{ required: true, message: '请输入表单名称' }]}
          >
            <Input placeholder="例如：请假申请表" />
          </Form.Item>
          <Form.Item
            label="表单 Key"
            name="key"
            rules={[{ required: true, message: '请输入表单 Key' }]}
          >
            <Input placeholder="例如：leave_application" />
          </Form.Item>
          <Form.Item
            label="JSON Schema"
            name="jsonSchema"
            rules={[
              { required: true, message: '请输入 JSON Schema' },
              {
                validator: (_, value) => {
                  if (value) {
                    try { JSON.parse(value); return Promise.resolve(); }
                    catch { return Promise.reject(new Error('无效的 JSON')); }
                  }
                  return Promise.resolve();
                },
              },
            ]}
          >
            <Input.TextArea rows={8} placeholder='{"type": "object", "properties": {...}}' />
          </Form.Item>
          <Form.Item label="UI Schema" name="uiSchema">
            <Input.TextArea rows={4} placeholder='{"ui:order": ["name", "date"]}' />
          </Form.Item>
          <Form.Item label="字段配置" name="fieldsConfig">
            <Input.TextArea rows={6} placeholder='提取的字段配置（JSON）' />
          </Form.Item>
          <Form.Item label="状态" name="status">
            <Select>
              <Option value={1}>启用</Option>
              <Option value={0}>停用</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      {/* ===== Fields Modal ===== */}
      <Modal
        title="表单字段详情"
        open={fieldsVisible}
        onCancel={() => setFieldsVisible(false)}
        footer={null}
        width={600}
      >
        {fieldsLoading && <p>加载中...</p>}
        {!fieldsLoading && fieldsData.length === 0 && <p>暂无字段信息</p>}
        {!fieldsLoading && fieldsData.length > 0 && (
          <Descriptions bordered column={1} size="small">
            {fieldsData.map((field: any, idx: number) => (
              <Descriptions.Item key={idx} label={`字段 ${idx + 1}`}>
                <Space direction="vertical" size={2}>
                  <span><strong>名称：</strong>{field.name || '-'}</span>
                  <span><strong>类型：</strong>{field.type || '-'}</span>
                  <span><strong>标签：</strong>{field.label || '-'}</span>
                  <span><strong>必填：</strong>{field.required ? '是' : '否'}</span>
                  {field.description && <span><strong>说明：</strong>{field.description}</span>}
                </Space>
              </Descriptions.Item>
            ))}
          </Descriptions>
        )}
      </Modal>
    </div>
  );
};

export default FormSchemaManage;
