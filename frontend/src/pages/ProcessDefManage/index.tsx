import { useState, useEffect } from 'react';
import {
  Table, Button, Modal, Form, Input, Space, message, Tag, Popconfirm,
  Upload, Typography, Select,
} from 'antd';
import {
  PlusOutlined, PauseCircleOutlined, PlayCircleOutlined,
  DeleteOutlined, ReloadOutlined, UploadOutlined,
} from '@ant-design/icons';
import {
  listProcessDefs, deployProcess, suspendDefinition,
  activateDefinition, deleteDeployment,
} from '@/api/processDef';

const { Title } = Typography;
const { Dragger } = Upload;

interface DefRecord {
  id: string;
  processKey: string;
  processName: string;
  version: number;
  category?: string;
  suspended?: boolean;
  status?: number;
}

const ProcessDefManage = () => {
  const [data, setData] = useState<DefRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [pageSize] = useState(20);

  // Upload modal
  const [uploadVisible, setUploadVisible] = useState(false);
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    loadData();
  }, [page]);

  const loadData = async () => {
    setLoading(true);
    try {
      const res = await listProcessDefs({ page, pageSize });
      const list = res.data || [];
      setData(list);
      setTotal(list.length);
    } catch (error: any) {
      console.error(error);
      message.error('加载流程定义失败');
    } finally {
      setLoading(false);
    }
  };

  // ===== Deploy =====
  const handleDeploy = async (file: File) => {
    setUploading(true);
    try {
      await deployProcess(file);
      message.success('部署成功');
      setUploadVisible(false);
      loadData();
    } catch (error: any) {
      message.error(error.message || '部署失败');
    } finally {
      setUploading(false);
    }
  };

  // ===== Suspend / Activate =====
  const handleSuspend = async (record: DefRecord) => {
    try {
      await suspendDefinition(record.id);
      message.success('已挂起');
      loadData();
    } catch (error: any) {
      message.error(error.message || '挂起失败');
    }
  };

  const handleActivate = async (record: DefRecord) => {
    try {
      await activateDefinition(record.id);
      message.success('已激活');
      loadData();
    } catch (error: any) {
      message.error(error.message || '激活失败');
    }
  };

  // ===== Delete =====
  const handleDelete = async (record: DefRecord) => {
    try {
      await deleteDeployment(record.id);
      message.success('已删除');
      loadData();
    } catch (error: any) {
      message.error(error.message || '删除失败');
    }
  };

  const columns = [
    {
      title: '流程名称',
      dataIndex: 'processName',
      key: 'processName',
      width: 180,
      ellipsis: true,
      render: (text: string) => <strong>{text || '-'}</strong>,
    },
    {
      title: '流程 Key',
      dataIndex: 'processKey',
      key: 'processKey',
      width: 160,
      render: (text: string) => <Tag color="blue">{text || '-'}</Tag>,
    },
    {
      title: '版本',
      dataIndex: 'version',
      key: 'version',
      width: 80,
      align: 'center' as const,
      render: (v: number) => <Tag color="purple">v{v}</Tag>,
    },
    {
      title: '分类',
      dataIndex: 'category',
      key: 'category',
      width: 120,
      render: (text: string) => text ? <Tag>{text}</Tag> : '-',
    },
    {
      title: '状态',
      dataIndex: 'suspended',
      key: 'suspended',
      width: 100,
      render: (_: any, record: DefRecord) =>
        record.suspended ? (
          <Tag color="orange">已挂起</Tag>
        ) : (
          <Tag color="green">已激活</Tag>
        ),
    },
    {
      title: '操作',
      key: 'action',
      width: 240,
      fixed: 'right' as const,
      render: (_: any, record: DefRecord) => (
        <Space size="small">
          {!record.suspended ? (
            <Popconfirm
              title="确定挂起该流程定义？"
              description="挂起后无法启动新流程实例"
              onConfirm={() => handleSuspend(record)}
              okText="确认"
              cancelText="取消"
            >
              <Button type="link" size="small" icon={<PauseCircleOutlined />}>
                挂起
              </Button>
            </Popconfirm>
          ) : (
            <Popconfirm
              title="确定激活该流程定义？"
              onConfirm={() => handleActivate(record)}
              okText="确认"
              cancelText="取消"
            >
              <Button type="link" size="small" icon={<PlayCircleOutlined />}>
                激活
              </Button>
            </Popconfirm>
          )}
          <Popconfirm
            title="确定删除该流程定义？"
            description="此操作不可恢复"
            onConfirm={() => handleDelete(record)}
            okText="确认"
            cancelText="取消"
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>流程定义管理</Title>

      {/* Upload Button */}
      <div style={{ marginBottom: 16 }}>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => setUploadVisible(true)}
        >
          上传部署
        </Button>
      </div>

      <Table
        columns={columns}
        dataSource={data}
        loading={loading}
        rowKey="id"
        scroll={{ x: 1000 }}
        pagination={{
          current: page,
          total,
          pageSize,
          onChange: (p) => setPage(p),
          showSizeChanger: false,
        }}
      />

      {/* ===== Upload Modal ===== */}
      <Modal
        title="上传部署 BPMN 文件"
        open={uploadVisible}
        onCancel={() => { setUploadVisible(false); setUploading(false); }}
        footer={null}
        width={600}
      >
        <Dragger
          accept=".bpmn,.xml"
          multiple={false}
          beforeUpload={(file) => {
            handleDeploy(file);
            return false; // prevent auto upload
          }}
          disabled={uploading}
        >
          <p className="ant-upload-drag-icon">
            <UploadOutlined />
          </p>
          <p className="ant-upload-text">点击或拖拽 BPMN 文件到此区域</p>
          <p className="ant-upload-hint">支持 .bpmn、.xml 格式</p>
        </Dragger>
        {uploading && <p style={{ textAlign: 'center', marginTop: 12 }}>正在部署...</p>}
      </Modal>
    </div>
  );
};

export default ProcessDefManage;
