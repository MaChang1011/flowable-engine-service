import { useState, useEffect } from 'react';
import { Card, Row, Col, Statistic, Table, Tag, Timeline, Typography, Space, Button, Badge, Tooltip } from 'antd';
import { 
  PlayCircleOutlined, 
  CheckCircleOutlined, 
  ClockCircleOutlined, 
  InboxOutlined,
  EyeOutlined,
  ReloadOutlined,
  UnorderedListOutlined,
  DashboardOutlined
} from '@ant-design/icons';
import { getMonitorDashboard, listInstances, getTrace } from '@/api/monitor';
import { useNavigate } from 'react-router-dom';

const Monitor = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [dashboard, setDashboard] = useState<any>(null);
  const [instances, setInstances] = useState<any[]>([]);
  const [traceData, setTraceData] = useState<any[]>([]);
  const [showTrace, setShowTrace] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<'running' | 'completed'>('running');
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);

  const loadDashboard = async () => {
    setLoading(true);
    try {
      const res = await getMonitorDashboard();
      setDashboard(res.data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const loadInstances = async () => {
    try {
      const res = await listInstances({ status: statusFilter, page, pageSize: 20 });
      setInstances(res.data?.data || []);
      setTotal(res.data?.total || 0);
    } catch (e) {
      console.error(e);
    }
  };

  const loadTrace = async (instanceId: string) => {
    try {
      const res = await getTrace(instanceId);
      setTraceData(res.data || []);
      setShowTrace(instanceId);
    } catch (e) {
      console.error(e);
    }
  };

  useEffect(() => {
    loadDashboard();
  }, []);

  useEffect(() => {
    loadInstances();
  }, [statusFilter, page]);

  // 流程图定义 key 映射
  const defNameMap: Record<string, string> = {
    leave_request: '请假审批流程',
  };

  const getStatusColor = (s: string) => s === 'running' ? 'blue' : 'green';
  const getStatusText = (s: string) => s === 'running' ? '进行中' : '已完成';

  const instanceColumns = [
    {
      title: '业务单号',
      dataIndex: 'businessKey',
      key: 'businessKey',
      width: 150,
    },
    {
      title: '流程名称',
      dataIndex: 'processDefinitionName',
      key: 'processDefinitionName',
      render: (name: string, record: any) => (
        <span>{name || defNameMap[record.processDefinitionKey] || record.processDefinitionKey}</span>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (s: string) => <Tag color={getStatusColor(s)}>{getStatusText(s)}</Tag>,
    },
    ...(statusFilter === 'running' ? [{
      title: '开始时间',
      dataIndex: 'startTime',
      key: 'startTime',
      width: 180,
      render: (t: string) => t ? new Date(t).toLocaleString('zh-CN') : '-',
    }] : [{
      title: '耗时',
      dataIndex: 'duration',
      key: 'duration',
      width: 100,
      render: (d: number) => d ? `${(d / 1000).toFixed(1)}s` : '-',
    }, {
      title: '开始时间',
      dataIndex: 'startTime',
      key: 'startTime',
      width: 180,
      render: (t: string) => t ? new Date(t).toLocaleString('zh-CN') : '-',
    }, {
      title: '结束时间',
      dataIndex: 'endTime',
      key: 'endTime',
      width: 180,
      render: (t: string) => t ? new Date(t).toLocaleString('zh-CN') : '-',
    }]),
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: (_: any, record: any) => (
        <Button
          type="link"
          icon={<EyeOutlined />}
          onClick={() => loadTrace(record.id)}
        >
          查看轨迹
        </Button>
      ),
    },
  ];

  return (
    <div>
      <Typography.Title level={4}>流程监控</Typography.Title>

      {/* 统计卡片 */}
      {dashboard && (
        <Row gutter={16} style={{ marginBottom: 24 }}>
          <Col span={6}>
            <Card>
              <Statistic
                title="进行中"
                value={dashboard.running.total}
                prefix={<PlayCircleOutlined />}
                valueStyle={{ color: '#1890ff' }}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="已完成"
                value={dashboard.completed.total}
                prefix={<CheckCircleOutlined />}
                valueStyle={{ color: '#52c41a' }}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="待办任务"
                value={dashboard.todoCount}
                prefix={<InboxOutlined />}
                valueStyle={{ color: '#ff4d4f' }}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="总流程数"
                value={dashboard.running.total + dashboard.completed.total}
                prefix={<UnorderedListOutlined />}
                valueStyle={{ color: '#722ed1' }}
              />
            </Card>
          </Col>
        </Row>
      )}

      {/* 流程定义分布 */}
      {dashboard?.processDefinitions && dashboard.processDefinitions.length > 0 && (
        <Card title="流程类型分布" style={{ marginBottom: 24 }}>
          <Space size="large">
            {dashboard.processDefinitions.map((def: any) => (
              <div key={def.key}>
                <Tooltip title={`${def.key}: ${def.running} 进行中 / ${def.completed} 已完成`}>
                  <Badge
                    count={`${def.running} 进行中`}
                    style={{ backgroundColor: '#1890ff', marginRight: 8 }}
                  />
                  <Badge
                    count={`${def.completed} 已完成`}
                    style={{ backgroundColor: '#52c41a', marginRight: 8 }}
                  />
                </Tooltip>
              </div>
            ))}
          </Space>
        </Card>
      )}

      {/* 实例列表 */}
      <Card
        title={statusFilter === 'running' ? '进行中的流程' : '已完成的流程'}
        extra={
          <Space>
            <Button
              type={statusFilter === 'running' ? 'primary' : 'default'}
              onClick={() => { setStatusFilter('running'); setPage(1); }}
              icon={<PlayCircleOutlined />}
            >
              进行中
            </Button>
            <Button
              type={statusFilter === 'completed' ? 'primary' : 'default'}
              onClick={() => { setStatusFilter('completed'); setPage(1); }}
              icon={<CheckCircleOutlined />}
            >
              已完成
            </Button>
            <Button icon={<ReloadOutlined />} onClick={loadDashboard}>刷新</Button>
          </Space>
        }
      >
        <Table
          columns={instanceColumns}
          dataSource={instances}
          rowKey="id"
          loading={loading}
          pagination={{
            current: page,
            total,
            pageSize: 20,
            onChange: (p) => setPage(p),
          }}
          scroll={{ x: 800 }}
        />
      </Card>

      {/* 流程轨迹 */}
      {showTrace && traceData.length > 0 && (
        <Card
          title={`流程轨迹 - ${traceData[0]?.activityName || ''}`}
          style={{ marginTop: 24 }}
          extra={
            <Button onClick={() => setShowTrace(null)}>收起</Button>
          }
        >
          <Timeline
            items={traceData.map((item: any) => ({
              color: item.status === 'completed' ? 'green' : 'blue',
              children: (
                <div>
                  <div style={{ fontWeight: 'bold' }}>
                    {item.activityName}
                    <Tag color={item.activityType === 'userTask' ? 'orange' : 'gray'} style={{ marginLeft: 8 }}>
                      {item.activityType === 'userTask' ? '人工节点' : item.activityType === 'startEvent' ? '开始' : item.activityType === 'endEvent' ? '结束' : item.activityType}
                    </Tag>
                  </div>
                  <div style={{ color: '#999', fontSize: 12, marginTop: 4 }}>
                    {item.startTime ? new Date(item.startTime).toLocaleString('zh-CN') : '-'} → {item.endTime ? new Date(item.endTime).toLocaleString('zh-CN') : '进行中'}
                    {item.duration != null && item.duration > 0 && ` (${(item.duration / 1000).toFixed(1)}s)`}
                    {item.assignee && ` | 处理人: ${item.assignee}`}
                  </div>
                </div>
              ),
            }))}
          />
        </Card>
      )}
    </div>
  );
};

export default Monitor;
