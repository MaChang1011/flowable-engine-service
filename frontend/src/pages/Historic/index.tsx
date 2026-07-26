import { useState, useEffect } from 'react';
import {
  Tabs, Table, Input, Button, Space, Tag, Card, Typography, Select, message,
} from 'antd';
import {
  ReloadOutlined, SearchOutlined,
} from '@ant-design/icons';
import { listHistoricProcesses, listHistoricTasks } from '@/api/historic';
import type { HistoricProcessInstance, HistoricTaskInstance } from '@/api/historic';

const { Title } = Typography;
const { Option } = Select;

const Historic = () => {
  const [activeTab, setActiveTab] = useState('process');
  const [processLoading, setProcessLoading] = useState(false);
  const [taskLoading, setTaskLoading] = useState(false);
  const [processData, setProcessData] = useState<HistoricProcessInstance[]>([]);
  const [taskData, setTaskData] = useState<HistoricTaskInstance[]>([]);
  const [processDefFilter, setProcessDefFilter] = useState<string>('');
  const [businessKeyFilter, setBusinessKeyFilter] = useState<string>('');
  const [processInstanceIdFilter, setProcessInstanceIdFilter] = useState<string>('');
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);

  // ===== Load Process History =====
  const loadProcesses = async () => {
    setProcessLoading(true);
    try {
      const params: Record<string, any> = { page, pageSize: 20 };
      if (processDefFilter) params.processDefinitionKey = processDefFilter;
      if (businessKeyFilter) params.businessKey = businessKeyFilter;
      const res = await listHistoricProcesses(params);
      const list = res.data || [];
      setProcessData(list);
      setTotal(list.length);
    } catch (error: any) {
      console.error(error);
      message.error('加载历史流程实例失败');
    } finally {
      setProcessLoading(false);
    }
  };

  // ===== Load Task History =====
  const loadTasks = async () => {
    setTaskLoading(true);
    try {
      const res = await listHistoricTasks(processInstanceIdFilter);
      const list = res.data || [];
      setTaskData(list);
      setTotal(list.length);
    } catch (error: any) {
      console.error(error);
      message.error('加载历史任务失败');
    } finally {
      setTaskLoading(false);
    }
  };

  useEffect(() => {
    if (activeTab === 'process') loadProcesses();
  }, [activeTab, processDefFilter, businessKeyFilter, page]);

  useEffect(() => {
    if (activeTab === 'task') loadTasks();
  }, [activeTab, processInstanceIdFilter]);

  const handleSearch = () => {
    if (activeTab === 'process') { setPage(1); loadProcesses(); }
    else { setPage(1); loadTasks(); }
  };

  const handleReset = () => {
    if (activeTab === 'process') {
      setProcessDefFilter('');
      setBusinessKeyFilter('');
      setPage(1);
      loadProcesses();
    } else {
      setProcessInstanceIdFilter('');
      setPage(1);
      loadTasks();
    }
  };

  // ===== Process Columns =====
  const processColumns = [
    {
      title: '业务单号',
      dataIndex: 'businessKey',
      key: 'businessKey',
      width: 180,
      ellipsis: true,
    },
    {
      title: '流程定义 Key',
      dataIndex: 'processDefinitionKey',
      key: 'processDefinitionKey',
      width: 180,
      render: (text: string) => <Tag color="blue">{text || '-'}</Tag>,
    },
    {
      title: '流程名称',
      dataIndex: 'processDefinitionName',
      key: 'processDefinitionName',
      width: 200,
      ellipsis: true,
    },
    {
      title: '发起人',
      dataIndex: 'initiator',
      key: 'initiator',
      width: 120,
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
      title: '耗时',
      dataIndex: 'duration',
      key: 'duration',
      width: 100,
      render: (d: number) => d ? `${(d / 1000).toFixed(1)}s` : '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: () => <Tag color="green">已完成</Tag>,
    },
  ];

  // ===== Task Columns =====
  const taskColumns = [
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
      title: '流程定义 Key',
      dataIndex: 'processDefinitionKey',
      key: 'processDefinitionKey',
      width: 180,
      render: (text: string) => <Tag color="blue">{text || '-'}</Tag>,
    },
    {
      title: '创建时间',
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
      title: '耗时',
      dataIndex: 'duration',
      key: 'duration',
      width: 100,
      render: (d: number) => d ? `${(d / 1000).toFixed(1)}s` : '-',
    },
  ];

  const tabItems = [
    {
      key: 'process',
      label: '历史流程实例',
      children: (
        <Card>
          <Space style={{ marginBottom: 16 }} size="middle">
            <Input
              placeholder="流程定义 Key"
              value={processDefFilter}
              onChange={(e) => setProcessDefFilter(e.target.value)}
              style={{ width: 200 }}
              allowClear
            />
            <Input
              placeholder="业务单号"
              value={businessKeyFilter}
              onChange={(e) => setBusinessKeyFilter(e.target.value)}
              style={{ width: 200 }}
              allowClear
            />
            <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
              查询
            </Button>
            <Button icon={<ReloadOutlined />} onClick={handleReset}>
              重置
            </Button>
          </Space>

          <Table
            columns={processColumns}
            dataSource={processData}
            loading={processLoading}
            rowKey="id"
            scroll={{ x: 1200 }}
            pagination={{
              current: page,
              total,
              pageSize: 20,
              onChange: (p) => setPage(p),
            }}
          />
        </Card>
      ),
    },
    {
      key: 'task',
      label: '历史任务',
      children: (
        <Card>
          <Space style={{ marginBottom: 16 }} size="middle">
            <Input
              placeholder="流程实例 ID"
              value={processInstanceIdFilter}
              onChange={(e) => setProcessInstanceIdFilter(e.target.value)}
              style={{ width: 300 }}
              allowClear
            />
            <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
              查询
            </Button>
            <Button icon={<ReloadOutlined />} onClick={handleReset}>
              重置
            </Button>
          </Space>

          <Table
            columns={taskColumns}
            dataSource={taskData}
            loading={taskLoading}
            rowKey="taskId"
            scroll={{ x: 1200 }}
            pagination={{
              current: page,
              total,
              pageSize: 20,
              onChange: (p) => setPage(p),
            }}
          />
        </Card>
      ),
    },
  ];

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>历史记录</Title>
      <Tabs activeKey={activeTab} onChange={setActiveTab} items={tabItems} />
    </div>
  );
};

export default Historic;
