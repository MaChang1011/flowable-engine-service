import { Form, Input, InputNumber, Select, Switch, DatePicker } from 'antd';
import type { Rule } from 'antd/es/form';

export interface FormField {
  name: string;
  type: string;
  title: string;
  required: boolean;
  widget?: string;
  minimum?: number;
  maximum?: number;
  maxLength?: number;
  options?: string[];
}

interface DynamicFormRendererProps {
  fields: FormField[];
  form: any; // Ant Design FormInstance
  layout?: 'horizontal' | 'vertical' | 'inline';
}

/**
 * 动态表单渲染器 — 根据 JSON Schema 提取的字段列表，
 * 自动渲染对应的 Ant Design 表单控件。
 */
const DynamicFormRenderer: React.FC<DynamicFormRendererProps> = ({
  fields,
  form,
  layout = 'vertical',
}) => {
  if (!fields || fields.length === 0) {
    return <p style={{ color: '#999' }}>该流程未配置表单字段，可直接提交。</p>;
  }

  const renderField = (field: FormField) => {
    const rules: Rule[] = [];
    if (field.required) {
      rules.push({ required: true, message: `请输入${field.title}` });
    }
    if (field.type === 'number' || field.type === 'integer') {
      rules.push({ type: 'number', message: '请输入数字' });
    }
    if (field.maxLength) {
      rules.push({ max: field.maxLength, message: `最多${field.maxLength}个字符` });
    }

    const commonProps = {
      style: { width: '100%' },
    };

    const widget = field.widget || field.type;

    switch (widget) {
      case 'textarea':
        return (
          <Form.Item key={field.name} label={field.title} name={field.name} rules={rules}>
            <Input.TextArea rows={3} placeholder={`请输入${field.title}`} />
          </Form.Item>
        );

      case 'select':
      case 'enum':
        return (
          <Form.Item key={field.name} label={field.title} name={field.name} rules={rules}>
            <Select placeholder={`请选择${field.title}`} {...commonProps}>
              {(field.options || []).map((opt) => (
                <Select.Option key={opt} value={opt}>
                  {opt}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
        );

      case 'number':
      case 'integer':
        return (
          <Form.Item key={field.name} label={field.title} name={field.name} rules={rules}>
            <InputNumber
              placeholder={`请输入${field.title}`}
              min={field.minimum}
              max={field.maximum}
              {...commonProps}
            />
          </Form.Item>
        );

      case 'boolean':
        return (
          <Form.Item key={field.name} label={field.title} name={field.name} valuePropName="checked">
            <Switch />
          </Form.Item>
        );

      case 'date':
        return (
          <Form.Item key={field.name} label={field.title} name={field.name} rules={rules}>
            <DatePicker placeholder={`请选择${field.title}`} {...commonProps} />
          </Form.Item>
        );

      case 'string':
      default:
        return (
          <Form.Item key={field.name} label={field.title} name={field.name} rules={rules}>
            <Input placeholder={`请输入${field.title}`} />
          </Form.Item>
        );
    }
  };

  return <>{fields.map(renderField)}</>;
};

export default DynamicFormRenderer;
