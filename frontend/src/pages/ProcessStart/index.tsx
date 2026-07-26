import { useState, useEffect } from 'react';
import { Form, Button, Input, Select, Card, message } from 'antd';
import { getProcessList } from '@/api/workflow';
import { startProcess } from '@/api/workflow';

const ProcessStart = () => {
  const [processes, setProcesses] = useState<any[]>([]);
  const [form] = Form.useForm();

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

  const onFinish = async (values: any) => {
    try {
      await startProcess(values.processKey, `BUSINESS_${Date.now()}`, values);
      message.success('流程启动成功');
      form.resetFields();
    } catch (error) {
      console.error(error);
    }
  };

  return (
    <Card title="发起流程">
      <Form form={form} onFinish={onFinish} layout="vertical">
        <Form.Item 
          name="processKey" 
          label="选择流程" 
          rules={[{ required: true }]}
        >
          <Select>
            {processes.map((p) => (
              <Select.Option key={p.id} value={p.processKey}>
                {p.processName} (v{p.version})
              </Select.Option>
            ))}
          </Select>
        </Form.Item>
        
        <Form.Item name="applicant" label="申请人" rules={[{ required: true }]}>
          <Input />
        </Form.Item>
        
        <Form.Item name="days" label="天数" rules={[{ required: true }]}>
          <Input type="number" min="0.5" max="30" />
        </Form.Item>
        
        <Form.Item name="reason" label="事由" rules={[{ required: true }]}>
          <Input.TextArea rows={4} />
        </Form.Item>
        
        <Button type="primary" htmlType="submit" block>
          提交申请
        </Button>
      </Form>
    </Card>
  );
};

export default ProcessStart;
