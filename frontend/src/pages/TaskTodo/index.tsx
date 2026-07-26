import { useState, useEffect } from 'react';
import { Table, Button, Modal, Form, Input, Space, message } from 'antd';
import { CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons';
import { getTodoTasks, completeTask } from '@/api/workflow';

const TaskTodo = () => {
  const [data, setData] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [currentTask, setCurrentTask] = useState<any>(null);
  const [form] = Form.useForm();

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
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (taskId: string, approved: boolean) => {
    try {
      await completeTask(taskId, { approved });
      message.success(approved ? '审批通过' : '审批拒绝');
      loadData();
    } catch (error) {
      console.error(error);
    }
  };

  const columns = [
    { title: '任务名称', dataIndex: 'taskName', key: 'taskName' },
    { title: '当前节点', dataIndex: 'currentActivityName', key: 'currentActivityName' },
    { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: any) => (
        <Space>
          <Button 
            type="primary" 
            icon={<CheckCircleOutlined />} 
            onClick={() => handleApprove(record.taskId, true)}
          >
            通过
          </Button>
          <Button 
            danger 
            icon={<CloseCircleOutlined />} 
            onClick={() => handleApprove(record.taskId, false)}
          >
            拒绝
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <h2>待办任务</h2>
      
      <Table 
        columns={columns} 
        dataSource={data} 
        loading={loading}
        rowKey="taskId"
        pagination={false}
      />
    </div>
  );
};

export default TaskTodo;
