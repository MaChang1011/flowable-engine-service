import { useState, useEffect } from 'react';
import { Card, Row, Col, Statistic, List, Tag, Typography } from 'antd';
import { ClockCircleOutlined, CheckCircleOutlined, InboxOutlined } from '@ant-design/icons';
import { getTodoTasks } from '@/api/workflow';

const Dashboard = () => {
  const [todoCount, setTodoCount] = useState(0);
  const [doneCount, setDoneCount] = useState(0);
  const [pendingList, setPendingList] = useState<any[]>([]);

  useEffect(() => {
    loadStats();
  }, []);

  const loadStats = async () => {
    try {
      const res = await getTodoTasks();
      const tasks = res.data || [];
      setTodoCount(tasks.length);
      setPendingList(tasks.slice(0, 5));
      setDoneCount(Math.floor(Math.random() * 10)); // 模拟数据
    } catch (error) {
      console.error(error);
    }
  };

  return (
    <div>
      <Typography.Title level={4}>工作台</Typography.Title>
      
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={8}>
          <Card>
            <Statistic 
              title="待办任务" 
              value={todoCount} 
              prefix={<InboxOutlined />} 
              valueStyle={{ color: '#ff4d4f' }}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic 
              title="已办任务" 
              value={doneCount} 
              prefix={<CheckCircleOutlined />} 
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic 
              title="发起流程" 
              value={3} 
              prefix={<ClockCircleOutlined />} 
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
      </Row>

      <Card title="我的待办">
        <List
          dataSource={pendingList}
          renderItem={(item) => (
            <List.Item>
              <List.Item.Meta
                title={item.taskName}
                description={`当前节点: ${item.currentActivityName}`}
              />
              <Tag color="blue">{item.createTime}</Tag>
            </List.Item>
          )}
        />
      </Card>
    </div>
  );
};

export default Dashboard;
