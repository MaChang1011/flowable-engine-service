import { useState, useEffect } from 'react';
import { Form, Button, Select, Card, message, Spin, Empty } from 'antd';
import { getProcessList, startProcess, getProcessFormFields } from '@/api/workflow';
import DynamicFormRenderer from '@/components/DynamicFormRenderer';
import type { FormField } from '@/components/DynamicFormRenderer';

const ProcessStart = () => {
  const [processes, setProcesses] = useState<any[]>([]);
  const [form] = Form.useForm();
  const [selectedProcess, setSelectedProcess] = useState<any>(null);
  const [formFields, setFormFields] = useState<FormField[]>([]);
  const [fieldsLoading, setFieldsLoading] = useState(false);

  useEffect(() => {
    loadProcesses();
  }, []);

  const loadProcesses = async () => {
    try {
      const res = await getProcessList();
      setProcesses(res.data || []);
    } catch (error) {
      console.error(error);
    }
  };

  const onProcessChange = async (processKey: string) => {
    const proc = processes.find((p) => p.processKey === processKey);
    setSelectedProcess(proc || null);
    if (!proc?.formSchemaId) {
      setFormFields([]);
      return;
    }

    setFieldsLoading(true);
    try {
      const res = await getProcessFormFields(proc.id);
      setFormFields(res.data || []);
    } catch (error) {
      console.error(error);
      setFormFields([]);
    } finally {
      setFieldsLoading(false);
    }
  };

  const onFinish = async (values: any) => {
    try {
      const { processKey, ...variables } = values;
      await startProcess(processKey, `BUSINESS_${Date.now()}`, variables);
      message.success('流程启动成功');
      form.resetFields();
      setFormFields([]);
      setSelectedProcess(null);
    } catch (error) {
      console.error(error);
      message.error('流程启动失败');
    }
  };

  return (
    <Card title="发起流程">
      <Form form={form} onFinish={onFinish} layout="vertical">
        <Form.Item
          name="processKey"
          label="选择流程"
          rules={[{ required: true, message: '请选择流程' }]}
        >
          <Select
            placeholder="请选择要发起的流程"
            onChange={onProcessChange}
            showSearch
            optionFilterProp="label"
          >
            {processes.map((p) => (
              <Select.Option key={p.id} value={p.processKey} label={p.processName}>
                {p.processName} (v{p.version})
              </Select.Option>
            ))}
          </Select>
        </Form.Item>

        {selectedProcess && (
          <>
            {fieldsLoading ? (
              <div style={{ textAlign: 'center', padding: 20 }}>
                <Spin tip="加载表单..." />
              </div>
            ) : formFields.length > 0 ? (
              <DynamicFormRenderer fields={formFields} form={form} />
            ) : (
              <Empty
                description="该流程未配置表单"
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                style={{ margin: '20px 0' }}
              >
                <p style={{ color: '#999', textAlign: 'center' }}>可以直接提交，无需填写表单字段。</p>
              </Empty>
            )}
          </>
        )}

        <Form.Item style={{ marginTop: 24 }}>
          <Button type="primary" htmlType="submit" block size="large">
            提交申请
          </Button>
        </Form.Item>
      </Form>
    </Card>
  );
};

export default ProcessStart;
