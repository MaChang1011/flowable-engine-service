import { useState, useEffect } from 'react';
import {
  Table, Button, Modal, Form, Input, Space, message, Tag, Select,
  Descriptions, Typography, Divider, Popconfirm, Timeline, Collapse,
} from 'antd';
import {
  EyeOutlined, UnorderedListOutlined, StopOutlined,
  PlayCircleOutlined, ReloadOutlined, ExclamationCircleOutlined,
} from '@ant-design/icons';
import {
  getInstanceDetail, getProcessTrace, terminateProcess,
  suspendProcess, activateProcess,
} from '@/api/workflow';

const { Title } = Typography;
const { Panel } = Collapse;

interface InstanceRecord {
  id: string;
  processDefinitionKey: string;
  processDefinitionName?: string;
  businessKey?: string;
  status: string;
  startTime?: string;
  endTime?: string;
  duration?: number;
  suspended?: boolean;
}

const ProcessManage = () => {
  const [instances, setInstances] = useState<InstanceRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [defKeyFilter, setDefKeyFilter] = useState<string>('');
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [pageSize] = useState(20);

  // Detail modal
  const [detailVisible, setDetailVisible] = useState(false);
  const [detailData, setDetailData] = useState<any>(null);

  // Trace modal
  const [traceVisible, setTraceVisible] = useState(false);
  const [traceData, setTraceData] = useState<any[]>([]);

  // Action loading
  const [actionLoading, setActionLoading] = useState(false);

  useEffect(() => {
    loadInstances();
  }, [statusFilter, defKeyFilter, page]);

  const loadInstances = async () => {
    setLoading(true);
    try {
      const params: Record<string, any> = { page, pageSize };
      if (statusFilter !== 'all') params.status = statusFilter;
      if (defKeyFilter) params.processDefinitionKey = defKeyFilter;

      // Use monitor API for instance list
      const res = await fetch('/api/monitor/instances', {
        headers: { Authorization: `Bearer ${localStorage.getItem('token') || ''}` },
      });
      const json = await res.json();
      const list = json.data?.data || json.data || [];
      setInstances(list);
      setTotal(json.data?.total || list.length);
    } catch (error: any) {
      console.error(error);
      message.error('加载流程实例失败');
    } finally {
      setLoading(false);
    }
  };

  // ===== View Detail =====
  const viewDetail = async (record: InstanceRecord) => {
    try {
      const res = await getInstanceDetail(record.id);
      setDetailData(res.data);
      setDetailVisible(true);
    } catch (error: any) {
      console.error(error);
      message.error('获取详情失败');
    }
  };

  // ===== View Trace =====
  const viewTrace = async (record: InstanceRecord) => {
    try {
      const res = await getProcessTrace(record.id);
      setTraceData(res.data || []);
      setTraceVisible(true);
    } catch (error: any) {
      console.error(error);
      message.error('获取轨迹失败');
    }
  };

  // ===== Actions =====
  const handleTerminate = async (record: InstanceRecord) => {
    try {
      await terminateProcess(record.id);
      message.success('流程已终止');
      loadInstances();
    } catch (error: any) {
      message.error(error.message || '终止失败');
    }
  };

  const handleSuspend = async (record: InstanceRecord) => {
    try {
      await suspendProcess(record.id);
      message.success('流程已挂起');
      loadInstances();
    } catch (error: any) {
      message.error(error.message || '挂起失败');
    }
  };

  const handleActivate = async (record: InstanceRecord) => {
    try {
      await activateProcess(record.id);
      message.success('流程已激活');
      loadInstances();
    } catch (error: any) {
      message.error(error.message || '激活失败');
    }
  };

  const getStatusColor = (s: string) => {
    if (s === 'completed' || s === 'finished') return 'green';
    if (s === 'running' || s === 'active') return 'blue';
    return 'default';
  };

  const getStatusText = (s: string) => {
    if (s === 'completed' || s === 'finished') return '已完成';
    if (s === 'running' || s === 'active') return '进行中';
    return s;
  };

  const columns = [
    {
      title: '流程定义 Key',
      dataIndex: 'processDefinitionKey',
      key: 'processDefinitionKey',
      width: 160,
      render: (text: string) => <Tag color="blue">{text || '-'}</Tag>,
    },
    {
      title: '业务单号',
      dataIndex: 'businessKey',
      key: 'businessKey',
      width: 150,
      ellipsis: true,
      render: (text: string) => text || '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (s: string) => <Tag color={getStatusColor(s)}>{getStatusText(s)}</Tag>,
    },
    {
      title: '开始时间',
      dataIndex: 'startTime',
      key: 'startTime',
      width: 180,
      render: (t: string) => t ? new Date(t).toLocaleString('zh-CN') : '-',
    },
    {
      title: '结束时间',
      dataIndex: 'endTime',
      key: 'endTime',
      width: 180,
      render: (t: string) => t ? new Date(t).toLocaleString('zh-CN') : '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 320,
      fixed: 'right' as const,
      render: (_: any, record: InstanceRecord) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => viewDetail(record)}
          >
            查看详情
          </Button>
          <Button
            type="link"
            size="small"
            icon={<UnorderedListOutlined />}
            onClick={() => viewTrace(record)}
          >
            查看轨迹
          </Button>
          {record.status !== 'completed' && record.status !== 'finished' && (
            <>
              <Popconfirm
                title="确定终止该流程？"
                description="终止后流程将不可恢复"
                onConfirm={() => handleTerminate(record)}
                okText="确认"
                cancelText="取消"
              >
                <Button type="link" size="small" danger icon={<StopOutlined />}>
                  终止流程
                </Button>
              </Popconfirm>
              {!record.suspended ? (
                <Popconfirm
                  title="确定挂起该流程？"
                  onConfirm={() => handleSuspend(record)}
                  okText="确认"
                  cancelText="取消"
                >
                  <Button type="link" size="small" icon={<PlayCircleOutlined />}>
                    挂起
                  </Button>
                </Popconfirm>
              ) : (
                <Popconfirm
                  title="确定激活该流程？"
                  onConfirm={() => handleActivate(record)}
                  okText="确认"
                  cancelText="取消"
                >
                  <Button type="link" size="small" icon={<PlayCircleOutlined />}>
                    激活
                  </Button>
                </Popconfirm>
              )}
            </>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>流程实例管理</Title>

      {/* Filters */}
      <div style={{ marginBottom: 16, display: 'flex', gap: 12, alignItems: 'center' }}>
        <Select
          value={statusFilter}
          onChange={(v) => { setStatusFilter(v); setPage(1); }}
          style={{ width: 160 }}
        >
          <Select.Option value="all">全部状态</Select.Option>
          <Select.Option value="running">进行中</Select.Option>
          <Select.Option value="completed">已完成</Select.Option>
        </Select>
        <Input
          placeholder="流程定义 Key 筛选"
          value={defKeyFilter}
          onChange={(e) => { setDefKeyFilter(e.target.value); setPage(1); }}
          style={{ width: 240 }}
          allowClear
        />
        <Button icon={<ReloadOutlined />} onClick={loadInstances}>
          刷新
        </Button>
      </div>

      <Table
        columns={columns}
        dataSource={instances}
        loading={loading}
        rowKey="id"
        scroll={{ x: 1200 }}
        pagination={{
          current: page,
          total,
          pageSize,
          onChange: (p) => setPage(p),
          showSizeChanger: false,
        }}
      />

      {/* ===== Detail Modal ===== */}
      <Modal
        title="流程实例详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={null}
        width={700}
      >
        {detailData && (
          <Descriptions bordered column={1} size="small">
            <Descriptions.Item label="实例 ID">{detailData.id}</Descriptions.Item>
            <Descriptions.Item label="流程定义 Key">
              <Tag color="blue">{detailData.processDefinitionKey || '-'}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="业务单号">{detailData.businessKey || '-'}</Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag color={getStatusColor(detailData.status)}>
                {getStatusText(detailData.status)}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="是否挂起">
              {detailData.suspended ? <Tag color="orange">是</Tag> : <Tag color="green">否</Tag>}
            </Descriptions.Item>
            <Descriptions.Item label="开始时间">
              {detailData.startTime
                ? new Date(detailData.startTime).toLocaleString('zh-CN')
                : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="结束时间">
              {detailData.endTime
                ? new Date(detailData.endTime).toLocaleString('zh-CN')
                : '-'}
            </Descriptions.Item>
            {detailData.variables && Object.keys(detailData.variables).length > 0 && (
              <Descriptions.Item label="变量">
                <pre style={{ margin: 0, fontSize: 12 }}>
                  {JSON.stringify(detailData.variables, null, 2)}
                </pre>
              </Descriptions.Item>
            )}
          </Descriptions>
        )}
      </Modal>

      {/* ===== Trace Modal ===== */}
      <Modal
        title="流程执行轨迹"
        open={traceVisible}
        onCancel={() => setTraceVisible(false)}
        footer={null}
        width={700}
      >
        {traceData.length > 0 ? (
          <Timeline
            items={traceData.map((item: any) => ({
              color: item.status === 'completed' ? 'green' : item.status === 'running' ? 'blue' : 'gray',
              children: (
                <div>
                  <div style={{ fontWeight: 'bold' }}>
                    {item.activityName}
                    <Tag
                      color={item.activityType === 'userTask' ? 'orange' : 'gray'}
                      style={{ marginLeft: 8 }}
                    >
                      {item.activityType === 'userTask'
                        ? '人工节点'
                        : item.activityType === 'startEvent'
                          ? '开始'
                          : item.activityType === 'endEvent'
                            ? '结束'
                            : item.activityType}
                    </Tag>
                  </div>
                  <div style={{ color: '#999', fontSize: 12, marginTop: 4 }}>
                    {item.assignee && `处理人: ${item.assignee} | `}
                    {item.startTime
                      ? new Date(item.startTime).toLocaleString('zh-CN')
                      : '-'}{' '}
                    →{' '}
                    {item.endTime
                      ? new Date(item.endTime).toLocaleString('zh-CN')
                      : '进行中'}
                    {item.duration != null && item.duration > 0 && ` (${(item.duration / 1000).toFixed(1)}s)`}
                  </div>
                  {item.comment && (
                    <div style={{ color: '#666', fontSize: 12, marginTop: 2 }}>
                      备注: {item.comment}
                    </div>
                  )}
                </div>
              ),
            }))}
          />
        ) : (
          <Typography.Text type="secondary">暂无轨迹数据</Typography.Text>
        )}
      </Modal>
    </div>
  );
};

export default ProcessManage;
