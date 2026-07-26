import { useState } from 'react';
import { Button, Layout, Menu, Typography } from 'antd';
import { Outlet, Navigate } from 'react-router-dom';
import { useAuthStore } from '@/store/auth';
import {
  DashboardOutlined,
  TeamOutlined,
  UserOutlined,
  SettingOutlined,
  FileTextOutlined,
  InboxOutlined,
  LogoutOutlined,
  EyeOutlined,
} from '@ant-design/icons';
import { useNavigate, useLocation } from 'react-router-dom';

const { Header, Sider, Content } = Layout;

const menuItems = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: '工作台' },
  { key: '/monitor', icon: <EyeOutlined />, label: '流程监控' },
  { key: '/task-todo', icon: <InboxOutlined />, label: '待办任务' },
  { key: '/process-start', icon: <InboxOutlined />, label: '发起流程' },
  { type: 'divider' },
  { key: '/org-manage', icon: <TeamOutlined />, label: '机构管理' },
  { key: '/role-manage', icon: <SettingOutlined />, label: '角色管理' },
  { key: '/user-manage', icon: <UserOutlined />, label: '用户管理' },
  { key: '/process-def', icon: <FileTextOutlined />, label: '流程定义' },
];

const MainLayout = () => {
  const token = useAuthStore((state) => state.token);
  const logout = useAuthStore((state) => state.logout);
  const navigate = useNavigate();
  const location = useLocation();

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider breakpoint="lg" collapsedWidth="80">
        <div style={{ padding: '16px', textAlign: 'center', color: '#fff', fontSize: '18px' }}>
          工作流审批系统
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      
      <Layout>
        <Header style={{ 
          background: '#fff', 
          padding: '0 24px', 
          display: 'flex', 
          alignItems: 'center',
          justifyContent: 'space-between',
          boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
        }}>
          <Typography.Title level={4} style={{ margin: 0 }}>
            {menuItems.find(item => item.key === location.pathname)?.label || '工作台'}
          </Typography.Title>
          <Button icon={<LogoutOutlined />} onClick={handleLogout}>
            退出登录
          </Button>
        </Header>
        
        <Content style={{ margin: '24px 16px', padding: 24, background: '#fff', minHeight: 280 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
};

export default MainLayout;
