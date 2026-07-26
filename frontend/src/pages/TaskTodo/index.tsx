import { useState, useEffect } from 'react';
import {
  Table, Button, Modal, Form, Input, Space, message, Tag, Select,
  Descriptions, Typography, Divider, Popconfirm, Tooltip, Dropdown,
} from 'antd';
import {
  CheckCircleOutlined, CloseCircleOutlined, UserSwitchOutlined,
  RetweetOutlined, ExclamationCircleOutlined, ClockCircleOutlined,
  FileTextOutlined, DownOutlined,
} from '@ant-design/icons';
import {
  getTodoTasks, completeTask, rejectToNode, delegateTask, claimTask,
} from '@/api/task';
import { useAuthStore } from '@/store/auth';

const { Title } = Typography;
const { Option } = Select;

const TaskTodo = () => {
  const [data, setData] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [currentUser] = useState(() => useAuthStore.getState().user?.name || '');

  // Approve modal
  const [approveVisible, setApproveVisible] = useState(false);
  const [approveForm] = Form.useForm();
  const [approveTask, setApproveTask] = useState<any>(null);

  // Reject modal
  const [rejectVisible, setRejectVisible] = useState(false);
  const [rejectForm] = Form.useForm();
  const [rejectTask, setRejectTask] = useState<any>(null);

  // Delegate modal
  const [delegateVisible, setDelegateVisible] = useState(false);
  const [delegateForm] = Form.useForm();
  const [delegateTask, setDelegateTask] = useState<any>(null);

  // Claim modal
  const [claimVisible, setClaimVisible] = useState(false);
  const [claimTask, setClaimTask] = useState<any>(null);

  // Detail modal
  const [detailVisible, setDetailVisible] = useState(false);
  const [detailTask, setDetailTask] = useState<any>(null);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const res = await getTodoTasks();
      setData(res.data || []);
    } catch (error) {
      console.error(error);
      message.error('加载待办任务失败');
    } finally {
      setLoading(false);
    }
  };

  // ===== Approve =====
  const openApprove = (record: any) => {
    setApproveTask(record);
    approveForm.resetFields();
    setApproveVisible(true);
  };

  const handleApprove = async () => {
    try {
      const values = await approveForm.validateFields();
      await completeTask(approveTask.taskId, {
        approved: true,
        comment: values.comment,
        variables: values.variables ? JSON.parse(values.variables) : {},
      });
      message.success('审批通过');
      setApproveVisible(false);
      loadData();
    } catch (error: any) {
      if (error.name !== 'Error') {
        // form validation error — don't show API error
      } else {
        message.error(error.message || '审批失败');
      }
    }
  };

  // ===== Reject to Node =====
  const openReject = (record: any) => {
    setRejectTask(record);
    rejectForm.resetFields();
    setRejectVisible(true);
  };

  const handleReject = async () => {
    try {
      const values = await rejectForm.validateFields();
      await rejectToNode(rejectTask.taskId, values.targetNodeId, {
        comment: values.comment,
        variables: values.variables ? JSON.parse(values.variables) : {},
      });
      message.success('已驳回到指定节点');
      setRejectVisible(false);
      loadData();
    } catch (error: any) {
      if (error.name !== 'Error') { /* form validation */ }
      else { message.error(error.message || '驳回失败'); }
    }
  };

  // ===== Delegate =====
  const openDelegate = (record: any) => {
    setDelegateTask(record);
    delegateForm.resetFields();
    setDelegateVisible(true);
  };

  const handleDelegate = async () => {
    try {
      const values = await delegateForm.validateFields();
      await delegateTask(delegateTask.taskId, values.newAssignee, values.comment);
      message.success('任务已转交');
      setDelegateVisible(false);
      loadData();
    } catch (error: any) {
      if (error.name !== 'Error') { /* form validation */ }
      else { message.error(error.message || '转交失败'); }
    }
  };

  // ===== Claim =====
  const openClaim = (record: any) => {
    setClaimTask(record);
    setClaimVisible(true);
  };

  const handleClaim = async () => {
    try {
      if (!currentUser) {
        message.warning('请先登录');
        return;
      }
      await claimTask(claimTask.taskId, currentUser);
      message.success('认领成功');
      setClaimVisible(false);
      loadData();
    } catch (error: any) {
      if (error.name !== 'Error') { /* form validation */ }
      else { message.error(error.message || '认领失败'); }
    }
  };

  // ===== Detail =====
  const openDetail = (record: any) => {
    setDetailTask(record);
    setDetailVisible(true);
  };

  const columns = [
    {
      title: '任务名称',
      dataIndex: 'taskName',
      key: 'taskName',
      width: 180,
      ellipsis: true,
    },
    {
      title: '流程定义 Key',
      dataIndex: 'processDefinitionKey',
      key: 'processDefinitionKey',
      width: 160,
      ellipsis: true,
      render: (text: string) => <Tag color="blue">{text || '-'}</Tag>,
    },
    {
      title: '当前节点',
      dataIndex: 'currentActivityName',
      key: 'currentActivityName',
      width: 140,
      ellipsis: true,
      render: (text: string) => <Tag color="orange">{text || '-'}</Tag>,
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
      render: (t: string) => t ? new Date(t).toLocaleString('zh-CN') : '-',
    },
    {
      title: '截止时间',
      dataIndex: 'dueDate',
      key: 'dueDate',
      width: 180,
      render: (t: string) =>
        t ? (
          <Tooltip title={new Date(t).toLocaleString('zh-CN')}>
            <Tag color={new Date(t) < new Date() ? 'red' : 'green'}>
              {new Date(t).toLocaleDateString('zh-CN')}
            </Tag>
          </Tooltip>
        ) : (
          '-'
        ),
    },
    {
      title: '处理人',
      dataIndex: 'assignee',
      key: 'assignee',
      width: 120,
      render: (text: string) => text || <Tag color="default">未分配</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      width: 260,
      fixed: 'right' as const,
      render: (_: any, record: any) => {
        const isUnclaimed = !record.assignee;

        const approveItems = [
          {
            label: '通过',
            icon: <CheckCircleOutlined />,
            onClick: () => openApprove(record),
          },
          {
            label: '拒绝',
            icon: <CloseCircleOutlined />,
            danger: true,
            onClick: () => {
              Modal.confirm({
                title: '确认拒绝？',
                icon: <ExclamationCircleOutlined />,
                content: `确定拒绝「${record.taskName}」吗？`,
                okText: '确认拒绝',
                cancelText: '取消',
                onOk: async () => {
                  try {
                    await completeTask(record.taskId, { approved: false });
                    message.success('已拒绝');
                    loadData();
                  } catch (e: any) {
                    message.error(e.message || '拒绝失败');
                  }
                },
              });
            },
          },
        ];

        return (
          <Space direction="vertical" size={4}>
            <Button
              type="primary"
              size="small"
              icon={<CheckCircleOutlined />}
              onClick={() => openApprove(record)}
            >
              通过
            </Button>
            <Dropdown menu={{ items: approveItems }} trigger={['click']}>
              <Button size="small" icon={<DownOutlined />}>
                更多操作
              </Button>
            </Dropdown>
            <Button
              size="small"
              icon={<RetweetOutlined />}
              onClick={() => openReject(record)}
            >
              驳回
            </Button>
            <Button
              size="small"
              icon={<UserSwitchOutlined />}
              onClick={() => openDelegate(record)}
            >
              转交
            </Button>
            {isUnclaimed && (
              <Button
                size="small"
                type="dashed"
                icon={<FileTextOutlined />}
                onClick={() => openClaim(record)}
              >
                认领
              </Button>
            )}
            <Button
              size="small"
              type="link"
              onClick={() => openDetail(record)}
            >
              详情
            </Button>
          </Space>
        );
      },
    },
  ];

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>待办任务</Title>

      <Table
        columns={columns}
        dataSource={data}
        loading={loading}
        rowKey="taskId"
        scroll={{ x: 1200 }}
        pagination={{ pageSize: 20 }}
      />

      {/* ===== Approve Modal ===== */}
      <Modal
        title="审批通过"
        open={approveVisible}
        onOk={handleApprove}
        onCancel={() => setApproveVisible(false)}
        okText="确认通过"
        cancelText="取消"
      >
        <p>正在审批：<strong>{approveTask?.taskName}</strong></p>
        <Form form={approveForm} layout="vertical">
          <Form.Item label="审批意见" name="comment">
            <Input.TextArea rows={3} placeholder="请输入审批意见（可选）" />
          </Form.Item>
          <Form.Item label="变量 (JSON)" name="variables" tooltip="以 JSON 格式传入业务变量">
            <Input.TextArea
              rows={5}
              placeholder={`{\n  "amount": 1000,\n  "reason": "..." \n}`}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* ===== Reject Modal ===== */}
      <Modal
        title="驳回到指定节点"
        open={rejectVisible}
        onOk={handleReject}
        onCancel={() => setRejectVisible(false)}
        okText="确认驳回"
        cancelText="取消"
      >
        <p>正在驳回：<strong>{rejectTask?.taskName}</strong></p>
        <Form form={rejectForm} layout="vertical">
          <Form.Item
            label="目标节点 ID"
            name="targetNodeId"
            rules={[{ required: true, message: '请输入目标节点 ID' }]}
          >
            <Input placeholder="例如：node_approve_01" />
          </Form.Item>
          <Form.Item label="驳回原因" name="comment">
            <Input.TextArea rows={2} placeholder="请输入驳回原因（可选）" />
          </Form.Item>
          <Form.Item label="变量 (JSON)" name="variables" tooltip="以 JSON 格式传入业务变量">
            <Input.TextArea rows={3} placeholder='{"remark": "..."}' />
          </Form.Item>
        </Form>
      </Modal>

      {/* ===== Delegate Modal ===== */}
      <Modal
        title="转交任务"
        open={delegateVisible}
        onOk={handleDelegate}
        onCancel={() => setDelegateVisible(false)}
        okText="确认转交"
        cancelText="取消"
      >
        <p>正在转交：<strong>{delegateTask?.taskName}</strong></p>
        <Form form={delegateForm} layout="vertical">
          <Form.Item
            label="接收用户"
            name="newAssignee"
            rules={[{ required: true, message: '请输入接收用户' }]}
          >
            <Input placeholder="用户名或用户 ID" />
          </Form.Item>
          <Form.Item label="备注" name="comment">
            <Input.TextArea rows={2} placeholder="请输入转交备注（可选）" />
          </Form.Item>
        </Form>
      </Modal>

      {/* ===== Claim Modal ===== */}
      <Modal
        title="认领任务"
        open={claimVisible}
        onOk={handleClaim}
        onCancel={() => setClaimVisible(false)}
        okText="确认认领"
        cancelText="取消"
      >
        <p>确认认领任务：<strong>{claimTask?.taskName}</strong>？</p>
        <p style={{ color: '#999' }}>认领后该任务将分配给你本人。</p>
      </Modal>

      {/* ===== Detail Modal ===== */}
      <Modal
        title="任务详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={null}
        width={600}
      >
        {detailTask && (
          <Descriptions bordered column={1} size="small">
            <Descriptions.Item label="任务 ID">{detailTask.taskId}</Descriptions.Item>
            <Descriptions.Item label="任务名称">{detailTask.taskName}</Descriptions.Item>
            <Descriptions.Item label="流程定义 Key">
              <Tag color="blue">{detailTask.processDefinitionKey || '-'}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="当前节点">
              <Tag color="orange">{detailTask.currentActivityName || '-'}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="处理人">
              {detailTask.assignee || <Tag color="default">未分配</Tag>}
            </Descriptions.Item>
            <Descriptions.Item label="创建时间">
              {detailTask.createTime
                ? new Date(detailTask.createTime).toLocaleString('zh-CN')
                : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="截止时间">
              {detailTask.dueDate
                ? new Date(detailTask.dueDate).toLocaleString('zh-CN')
                : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="流程实例 ID">{detailTask.processInstanceId}</Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  );
};

export default TaskTodo;
