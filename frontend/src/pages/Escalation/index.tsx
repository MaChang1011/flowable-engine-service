import { useState, useEffect } from 'react';
import {
  Table, Button, Space, message, Tag, Typography, Popconfirm, Card,
} from 'antd';
import {
  ExclamationCircleOutlined, ReloadOutlined, ArrowUpOutlined,
} from '@ant-design/icons';
import { getOverdueTasks, escalateTask } from '@/api/escalation';
import type { EscalationTask } from '@/api/escalation';

const { Title } = Typography;

const Escalation = () => {
  const [data, setData] = useState<EscalationTask[]>([]);
  const [loading, setLoading] = useState(false);
  const [autoRefresh, setAutoRefresh] = useState(true);

  const loadData = async () => {
    setLoading(true);
    try {
      const res = await getOverdueTasks();
      setData(res.data || []);
    } catch (error: any) {
      console.error(error);
      message.error('加载超时任务失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  // Auto-refresh every 60 seconds
  useEffect(() => {
    if (!autoRefresh) return;
    const timer = setInterval(() => {
      loadData();
    }, 60000);
    return () => clearInterval(timer);
  }, [autoRefresh]);

  const handleEscalate = async (taskId: string) => {
    try {
      await escalateTask(taskId);
      message.success('升级成功');
      loadData();
    } catch (error: any) {
      message.error(error.message || '升级失败');
    }
  };

  const columns = [
    {
      title: '任务 ID',
      dataIndex: 'taskId',
      key: 'taskId',
      width: 180,
    },
    {
      title: '任务名称',
      dataIndex: 'taskName',
      key: 'taskName',
      width: 200,
      ellipsis: true,
    },
    {
      title: '处理人',
      dataIndex: 'assignee',
      key: 'assignee',
      width: 120,
      render: (text: string) => text || <Tag color="default">未分配</Tag>,
    },
    {
      title: '流程实例 ID',
      dataIndex: 'processInstanceId',
      key: 'processInstanceId',
      width: 180,
    },
    {
      title: '当前节点',
      dataIndex: 'currentActivityName',
      key: 'currentActivityName',
      width: 160,
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
      render: (t: string) => t ? new Date(t).toLocaleString('zh-CN') : '-',
    },
    {
      title: '超时时长',
      dataIndex: 'overdueDuration',
      key: 'overdueDuration',
      width: 140,
      render: (text: string) => (
        <Tag color="red" icon={<ExclamationCircleOutlined />}>
          {text || '-'}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 160,
      fixed: 'right' as const,
      render: (_: any, record: EscalationTask) => (
        <Popconfirm
          title={`确认升级任务「${record.taskName}」？`}
          description="升级后将通知上级处理人"
          onConfirm={() => handleEscalate(record.taskId)}
          okText="确认升级"
          cancelText="取消"
          okButtonProps={{ danger: true }}
        >
          <Button size="small" danger icon={<ArrowUpOutlined />}>
            手动升级
          </Button>
        </Popconfirm>
      ),
    },
  ];

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>超时升级</Title>

      <Card
        extra={
          <Space>
            <span>自动刷新：</span>
            <Tag color={autoRefresh ? 'green' : 'default'}>
              {autoRefresh ? '开启' : '关闭'}
            </Tag>
            <Button
              size="small"
              onClick={() => setAutoRefresh(!autoRefresh)}
            >
              {autoRefresh ? '暂停' : '恢复'}
            </Button>
            <Button icon={<ReloadOutlined />} onClick={loadData}>立即刷新</Button>
          </Space>
        }
      >
        <Table
          columns={columns}
          dataSource={data}
          loading={loading}
          rowKey="taskId"
          scroll={{ x: 1400 }}
          pagination={{ pageSize: 20 }}
        />
      </Card>
    </div>
  );
};

export default Escalation;
