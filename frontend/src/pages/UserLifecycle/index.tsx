import { useState } from 'react';
import {
  Tabs, Form, Input, Button, Space, message, Typography, Card,
  Descriptions, Tag, Divider,
} from 'antd';
import {
  ReloadOutlined, CheckCircleOutlined,
} from '@ant-design/icons';
import { transferUser, resignUser, getUserSummary } from '@/api/userLifecycle';
import type { UserSummary } from '@/api/userLifecycle';

const { Title } = Typography;

interface TransferResult {
  fromUserId: string;
  toUserId: string;
  transferredTaskCount: number;
  completedTaskCount: number;
}

interface ResignResult {
  userId: string;
  handoverUserId: string;
  reassignedTaskCount: number;
  completedTaskCount: number;
}

const UserLifecycle = () => {
  const [transferForm] = Form.useForm();
  const [resignForm] = Form.useForm();
  const [summaryForm] = Form.useForm();

  // Transfer result
  const [transferResult, setTransferResult] = useState<TransferResult | null>(null);
  const [transferLoading, setTransferLoading] = useState(false);

  // Resign result
  const [resignResult, setResignResult] = useState<ResignResult | null>(null);
  const [resignLoading, setResignLoading] = useState(false);

  // User summary
  const [userSummary, setUserSummary] = useState<UserSummary | null>(null);
  const [summaryLoading, setSummaryLoading] = useState(false);

  // ===== Transfer =====
  const handleTransfer = async () => {
    try {
      const values = await transferForm.validateFields();
      setTransferLoading(true);
      await transferUser({
        fromUserId: values.userId,
        toUserId: values.newOrgId,
        reason: values.comment,
      });
      setTransferResult({
        fromUserId: values.userId,
        toUserId: values.newOrgId,
        transferredTaskCount: 0,
        completedTaskCount: 0,
      });
      message.success('调岗处理成功');
    } catch (error: any) {
      if (error.name !== 'Error') { /* form validation */ }
      else { message.error(error.message || '调岗处理失败'); }
    } finally {
      setTransferLoading(false);
    }
  };

  // ===== Resign =====
  const handleResign = async () => {
    try {
      const values = await resignForm.validateFields();
      setResignLoading(true);
      await resignUser({
        userId: values.userId,
        handoverUserId: values.successorId,
        reason: values.comment,
      });
      setResignResult({
        userId: values.userId,
        handoverUserId: values.successorId,
        reassignedTaskCount: 0,
        completedTaskCount: 0,
      });
      message.success('离职处理成功');
    } catch (error: any) {
      if (error.name !== 'Error') { /* form validation */ }
      else { message.error(error.message || '离职处理失败'); }
    } finally {
      setResignLoading(false);
    }
  };

  // ===== Summary =====
  const handleSummary = async () => {
    try {
      const values = await summaryForm.validateFields();
      setSummaryLoading(true);
      const res = await getUserSummary(values.userId);
      setUserSummary(res.data);
    } catch (error: any) {
      if (error.name !== 'Error') { /* form validation */ }
      else { message.error(error.message || '获取用户摘要失败'); }
    } finally {
      setSummaryLoading(false);
    }
  };

  const tabItems = [
    {
      key: 'transfer',
      label: '调岗处理',
      children: (
        <Card title="调岗表单">
          <Form
            form={transferForm}
            layout="vertical"
            style={{ maxWidth: 500 }}
          >
            <Form.Item
              label="用户 ID"
              name="userId"
              rules={[{ required: true, message: '请输入用户 ID' }]}
            >
              <Input placeholder="原用户 ID" />
            </Form.Item>
            <Form.Item
              label="新机构 ID"
              name="newOrgId"
              rules={[{ required: true, message: '请输入新机构 ID' }]}
            >
              <Input placeholder="目标机构 ID" />
            </Form.Item>
            <Form.Item label="备注" name="comment">
              <Input.TextArea rows={2} placeholder="调岗原因或备注（可选）" />
            </Form.Item>
            <Form.Item>
              <Button
                type="primary"
                icon={<CheckCircleOutlined />}
                loading={transferLoading}
                onClick={handleTransfer}
              >
                执行调岗
              </Button>
            </Form.Item>
          </Form>

          {transferResult && (
            <Card title="调岗结果" size="small" style={{ marginTop: 16 }}>
              <Descriptions bordered column={1} size="small">
                <Descriptions.Item label="原用户 ID">{transferResult.fromUserId}</Descriptions.Item>
                <Descriptions.Item label="目标机构 ID">{transferResult.toUserId}</Descriptions.Item>
                <Descriptions.Item label="转移任务数">{transferResult.transferredTaskCount}</Descriptions.Item>
                <Descriptions.Item label="已完成任务数">{transferResult.completedTaskCount}</Descriptions.Item>
              </Descriptions>
            </Card>
          )}
        </Card>
      ),
    },
    {
      key: 'resign',
      label: '离职处理',
      children: (
        <Card title="离职表单">
          <Form
            form={resignForm}
            layout="vertical"
            style={{ maxWidth: 500 }}
          >
            <Form.Item
              label="用户 ID"
              name="userId"
              rules={[{ required: true, message: '请输入用户 ID' }]}
            >
              <Input placeholder="离职用户 ID" />
            </Form.Item>
            <Form.Item
              label="交接人 ID"
              name="successorId"
              rules={[{ required: true, message: '请输入交接人 ID' }]}
            >
              <Input placeholder="接替工作的用户 ID" />
            </Form.Item>
            <Form.Item label="备注" name="comment">
              <Input.TextArea rows={2} placeholder="离职原因或备注（可选）" />
            </Form.Item>
            <Form.Item>
              <Button
                type="primary"
                danger
                icon={<CheckCircleOutlined />}
                loading={resignLoading}
                onClick={handleResign}
              >
                执行离职处理
              </Button>
            </Form.Item>
          </Form>

          {resignResult && (
            <Card title="离职处理结果" size="small" style={{ marginTop: 16 }}>
              <Descriptions bordered column={1} size="small">
                <Descriptions.Item label="离职用户 ID">{resignResult.userId}</Descriptions.Item>
                <Descriptions.Item label="交接人 ID">{resignResult.handoverUserId}</Descriptions.Item>
                <Descriptions.Item label="重新分配任务数">
                  <Tag color="blue">{resignResult.reassignedTaskCount}</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="已完成任务数">{resignResult.completedTaskCount}</Descriptions.Item>
              </Descriptions>
            </Card>
          )}
        </Card>
      ),
    },
    {
      key: 'summary',
      label: '用户摘要',
      children: (
        <Card title="用户信息查询">
          <Space style={{ marginBottom: 16 }}>
            <Form
              form={summaryForm}
              layout="inline"
              onFinish={handleSummary}
            >
              <Form.Item
                name="userId"
                rules={[{ required: true, message: '请输入用户 ID' }]}
              >
                <Input placeholder="用户 ID" style={{ width: 200 }} />
              </Form.Item>
              <Form.Item>
                <Button type="primary" icon={<ReloadOutlined />} htmlType="submit" loading={summaryLoading}>
                  查询
                </Button>
              </Form.Item>
            </Form>
          </Space>

          {userSummary && (
            <Card title={`用户信息 - ${userSummary.username}`} size="small">
              <Descriptions bordered column={1} size="small">
                <Descriptions.Item label="用户 ID">{userSummary.userId}</Descriptions.Item>
                <Descriptions.Item label="用户名">{userSummary.username}</Descriptions.Item>
                <Divider />
                <Descriptions.Item label="待办任务数">
                  <Tag color="orange">{userSummary.pendingCount}</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="已完成任务数">
                  <Tag color="green">{userSummary.doneCount}</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="发起流程数">
                  <Tag color="blue">{userSummary.initiatedCount}</Tag>
                </Descriptions.Item>
              </Descriptions>
            </Card>
          )}
        </Card>
      ),
    },
  ];

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>用户交接</Title>
      <Tabs items={tabItems} />
    </div>
  );
};

export default UserLifecycle;
