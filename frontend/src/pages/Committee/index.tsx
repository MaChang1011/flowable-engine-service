import { useState, useEffect } from 'react';
import {
  Table, Button, Modal, Form, Input, Space, message, Tag, Select,
  Descriptions, Typography, Popconfirm, Card, Divider, InputNumber,
} from 'antd';
import {
  PlusOutlined, CheckCircleOutlined,
  AimOutlined, UnorderedListOutlined, ReloadOutlined,
} from '@ant-design/icons';
import { initCommittee, castVote, getTally, getVoteDetails } from '@/api/committee';
import request from '@/utils/request';
import type { VoteResult } from '@/api/committee';

const { Title } = Typography;
const { Option } = Select;

const Committee = () => {
  const [data, setData] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);

  // Init modal
  const [initVisible, setInitVisible] = useState(false);
  const [initForm] = Form.useForm();

  // Vote modal
  const [voteVisible, setVoteVisible] = useState(false);
  const [voteForm] = Form.useForm();
  const [votingTask, setVotingTask] = useState<any>(null);

  // Tally modal
  const [tallyVisible, setTallyVisible] = useState(false);
  const [tallyData, setTallyData] = useState<VoteResult | null>(null);
  const [tallyLoading, setTallyLoading] = useState(false);
  const [tallyThreshold, setTallyThreshold] = useState<number>(50);

  // Vote details modal
  const [detailsVisible, setDetailsVisible] = useState(false);
  const [detailsData, setDetailsData] = useState<VoteResult | null>(null);
  const [detailsLoading, setDetailsLoading] = useState(false);

  useEffect(() => {
    loadData();
  }, [page]);

  const loadData = async () => {
    setLoading(true);
    try {
      // Use a generic list — the backend returns tasks needing committee voting
      const res: any = await request.get('/wf/committee/tasks', { params: { page, pageSize: 20 } });
      const list = res.data || [];
      setData(list);
      setTotal(list.length);
    } catch (error: any) {
      console.error(error);
      message.error('加载委员会任务失败');
    } finally {
      setLoading(false);
    }
  };

  // ===== Initialize Committee =====
  const openInit = () => {
    initForm.resetFields();
    setInitVisible(true);
  };

  const handleInit = async () => {
    try {
      const values = await initForm.validateFields();
      const memberArray = values.memberIds
        .split(',')
        .map((s: string) => s.trim())
        .filter((s: string) => s.length > 0);
      await initCommittee(values.taskId, values.committeeName, memberArray);
      message.success('委员会初始化成功');
      setInitVisible(false);
      loadData();
    } catch (error: any) {
      if (error.name !== 'Error') { /* form validation */ }
      else { message.error(error.message || '初始化失败'); }
    }
  };

  // ===== Vote =====
  const openVote = (record: any) => {
    setVotingTask(record);
    voteForm.resetFields();
    setVoteVisible(true);
  };

  const handleVote = async () => {
    try {
      const values = await voteForm.validateFields();
      await castVote(votingTask.taskId, values.memberId, values.vote, values.comment, values.threshold);
      message.success('投票成功');
      setVoteVisible(false);
      loadData();
    } catch (error: any) {
      if (error.name !== 'Error') { /* form validation */ }
      else { message.error(error.message || '投票失败'); }
    }
  };

  // ===== Tally =====
  const openTally = async (taskId: string) => {
    setTallyLoading(true);
    setTallyVisible(true);
    try {
      const res = await getTally(taskId, tallyThreshold);
      setTallyData(res.data);
    } catch (error: any) {
      console.error(error);
      message.warning('获取计票结果失败');
      setTallyData(null);
    } finally {
      setTallyLoading(false);
    }
  };

  // ===== Vote Details =====
  const openDetails = async (taskId: string) => {
    setDetailsLoading(true);
    setDetailsVisible(true);
    try {
      const res = await getVoteDetails(taskId);
      setDetailsData(res.data);
    } catch (error: any) {
      console.error(error);
      message.warning('获取投票详情失败');
      setDetailsData(null);
    } finally {
      setDetailsLoading(false);
    }
  };

  const voteColorMap: Record<string, string> = {
    APPROVE: 'green',
    REJECT: 'red',
    ABSTAIN: 'orange',
  };

  const voteTextMap: Record<string, string> = {
    APPROVE: '同意',
    REJECT: '反对',
    ABSTAIN: '弃权',
  };

  const columns = [
    {
      title: '任务 ID',
      dataIndex: 'taskId',
      key: 'taskId',
      width: 150,
    },
    {
      title: '任务名称',
      dataIndex: 'taskName',
      key: 'taskName',
      width: 200,
      ellipsis: true,
    },
    {
      title: '流程定义 Key',
      dataIndex: 'processDefinitionKey',
      key: 'processDefinitionKey',
      width: 160,
      render: (text: string) => <Tag color="blue">{text || '-'}</Tag>,
    },
    {
      title: '当前节点',
      dataIndex: 'currentActivityName',
      key: 'currentActivityName',
      width: 140,
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
      width: 320,
      fixed: 'right' as const,
      render: (_: any, record: any) => (
        <Space size="small">
          <Button
            size="small"
            icon={<PlusOutlined />}
            onClick={() => openInit()}
          >
            初始化委员会
          </Button>
          <Button
            size="small"
            type="primary"
            icon={<CheckCircleOutlined />}
            onClick={() => openVote(record)}
          >
            投票
          </Button>
          <Popconfirm
            title="查看计票结果"
            onConfirm={() => openTally(record.taskId)}
            okText="确认"
            cancelText="取消"
          >
            <Button size="small" icon={<AimOutlined />}>查看计票</Button>
          </Popconfirm>
          <Button
            size="small"
            icon={<UnorderedListOutlined />}
            onClick={() => openDetails(record.taskId)}
          >
            投票详情
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>委员会投票</Title>

      <Card>
        <Table
          columns={columns}
          dataSource={data}
          loading={loading}
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

      {/* ===== Init Committee Modal ===== */}
      <Modal
        title="初始化委员会"
        open={initVisible}
        onOk={handleInit}
        onCancel={() => setInitVisible(false)}
        okText="初始化"
        cancelText="取消"
        width={500}
      >
        <Form form={initForm} layout="vertical">
          <Form.Item
            label="任务 ID"
            name="taskId"
            rules={[{ required: true, message: '请输入任务 ID' }]}
          >
            <Input placeholder="需要委员会投票的任务 ID" />
          </Form.Item>
          <Form.Item
            label="委员会名称"
            name="committeeName"
            rules={[{ required: true, message: '请输入委员会名称' }]}
          >
            <Input placeholder="例如：项目评审委员会" />
          </Form.Item>
          <Form.Item
            label="成员 IDs"
            name="memberIds"
            rules={[{ required: true, message: '请输入成员 IDs' }]}
          >
            <Input.TextArea rows={3} placeholder="多个 ID 用逗号分隔，例如：user001,user002,user003" />
          </Form.Item>
        </Form>
      </Modal>

      {/* ===== Vote Modal ===== */}
      <Modal
        title="投票"
        open={voteVisible}
        onOk={handleVote}
        onCancel={() => setVoteVisible(false)}
        okText="提交投票"
        cancelText="取消"
        width={500}
      >
        <p>正在投票：<strong>{votingTask?.taskName || ''}</strong></p>
        <Form form={voteForm} layout="vertical">
          <Form.Item
            label="任务 ID"
            name="taskId"
            initialValue={votingTask?.taskId}
            rules={[{ required: true, message: '请输入任务 ID' }]}
          >
            <Input disabled />
          </Form.Item>
          <Form.Item
            label="投票人 ID"
            name="memberId"
            rules={[{ required: true, message: '请输入投票人 ID' }]}
          >
            <Input placeholder="你的用户 ID" />
          </Form.Item>
          <Form.Item
            label="投票结果"
            name="vote"
            rules={[{ required: true, message: '请选择投票结果' }]}
          >
            <Select>
              <Option value="APPROVE">同意 (APPROVE)</Option>
              <Option value="REJECT">反对 (REJECT)</Option>
              <Option value="ABSTAIN">弃权 (ABSTAIN)</Option>
            </Select>
          </Form.Item>
          <Form.Item label="备注" name="comment">
            <Input.TextArea rows={2} placeholder="投票备注（可选）" />
          </Form.Item>
          <Form.Item label="通过阈值 (%)" name="threshold" initialValue={50}>
            <InputNumber min={0} max={100} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      {/* ===== Tally Modal ===== */}
      <Modal
        title="计票结果"
        open={tallyVisible}
        onCancel={() => setTallyVisible(false)}
        footer={null}
        width={600}
      >
        {tallyLoading && <p>加载中...</p>}
        {!tallyLoading && !tallyData && <p>暂无计票数据</p>}
        {!tallyLoading && tallyData && (
          <>
            <Descriptions bordered column={1} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="任务 ID">{tallyData.taskId}</Descriptions.Item>
              <Descriptions.Item label="委员会名称">{tallyData.committeeName}</Descriptions.Item>
              <Descriptions.Item label="通过阈值">
                <Space>
                  <InputNumber
                    min={0}
                    max={100}
                    value={tallyThreshold}
                    onChange={(v) => setTallyThreshold(v || 50)}
                    style={{ width: 100 }}
                  />
                  <span>%</span>
                </Space>
              </Descriptions.Item>
              <Divider />
              <Descriptions.Item label="同意票数">
                <Tag color="green">{tallyData.votes.filter((v: any) => v.vote === 'APPROVE').length}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="反对票数">
                <Tag color="red">{tallyData.votes.filter((v: any) => v.vote === 'REJECT').length}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="弃权票数">
                <Tag color="orange">{tallyData.votes.filter((v: any) => v.vote === 'ABSTAIN').length}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="投票结果">
                <Tag color={tallyData.result === 'PASS' ? 'green' : 'red'}>
                  {tallyData.result === 'PASS' ? '通过' : tallyData.result === 'REJECT' ? '否决' : tallyData.result || '待定'}
                </Tag>
              </Descriptions.Item>
            </Descriptions>
          </>
        )}
      </Modal>

      {/* ===== Vote Details Modal ===== */}
      <Modal
        title="投票详情"
        open={detailsVisible}
        onCancel={() => setDetailsVisible(false)}
        footer={null}
        width={600}
      >
        {detailsLoading && <p>加载中...</p>}
        {!detailsLoading && !detailsData && <p>暂无投票数据</p>}
        {!detailsLoading && detailsData && detailsData.votes.length === 0 && <p>暂无投票记录</p>}
        {!detailsLoading && detailsData && detailsData.votes.length > 0 && (
          <Table
            dataSource={detailsData.votes}
            rowKey="memberId"
            pagination={false}
            size="small"
            columns={[
              {
                title: '投票人',
                dataIndex: 'memberId',
                key: 'memberId',
              },
              {
                title: '投票结果',
                dataIndex: 'vote',
                key: 'vote',
                width: 120,
                render: (v: string) => (
                  <Tag color={voteColorMap[v] || 'default'}>
                    {voteTextMap[v] || v}
                  </Tag>
                ),
              },
              {
                title: '备注',
                dataIndex: 'comment',
                key: 'comment',
                ellipsis: true,
              },
              {
                title: '投票时间',
                dataIndex: 'voteTime',
                key: 'voteTime',
                width: 180,
                render: (t: string) => t ? new Date(t).toLocaleString('zh-CN') : '-',
              },
            ]}
          />
        )}
      </Modal>
    </div>
  );
};

export default Committee;
