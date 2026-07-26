import { useState, useEffect } from 'react';
import { Table, Button, Modal, Form, Input, Select, Space, Popconfirm, message } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, PlayCircleOutlined, PauseCircleOutlined } from '@ant-design/icons';
import { getProcessList } from '@/api/workflow';

const ProcessDef = () => {
  const [data, setData] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [form] = Form.useForm();

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const res = await getProcessList();
      setData(res.data || []);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const columns = [
    { title: '流程名称', dataIndex: 'processName', key: 'processName' },
    { title: '流程Key', dataIndex: 'processKey', key: 'processKey' },
    { title: '版本', dataIndex: 'version', key: 'version' },
    { 
      title: '状态', 
      dataIndex: 'status', 
      key: 'status',
      render: (status: number) => status === 1 ? '启用' : '停用'
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: any) => (
        <Space>
          <Button type="link" icon={<PlayCircleOutlined />}>
            发起
          </Button>
          <Button type="link" icon={<EditOutlined />}>
            编辑
          </Button>
          <Popconfirm title="确定删除？">
            <Button type="link" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <h2>流程定义</h2>
      <Button 
        type="primary" 
        icon={<PlusOutlined />} 
        onClick={() => {
          form.resetFields();
          setModalVisible(true);
        }}
        style={{ marginBottom: 16 }}
      >
        新增流程
      </Button>
      
      <Table 
        columns={columns} 
        dataSource={data} 
        loading={loading}
        rowKey="id"
        pagination={false}
      />
    </div>
  );
};

export default ProcessDef;
