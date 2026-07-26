import { Routes, Route, Navigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import MainLayout from '@/components/Layout/MainLayout';
import LoginPage from '@/pages/Login';
import Dashboard from '@/pages/Dashboard';
import OrgManage from '@/pages/OrgManage';
import RoleManage from '@/pages/RoleManage';
import UserManage from '@/pages/UserManage';
import ProcessDef from '@/pages/ProcessDef';
import TaskTodo from '@/pages/TaskTodo';
import ProcessStart from '@/pages/ProcessStart';
import Monitor from '@/pages/Monitor';

const App = () => {
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  useEffect(() => {
    const token = localStorage.getItem('token');
    setIsAuthenticated(!!token);
  }, []);

  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      
      {isAuthenticated ? (
        <Route element={<MainLayout />}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/org-manage" element={<OrgManage />} />
          <Route path="/role-manage" element={<RoleManage />} />
          <Route path="/user-manage" element={<UserManage />} />
          <Route path="/process-def" element={<ProcessDef />} />
          <Route path="/task-todo" element={<TaskTodo />} />
          <Route path="/process-start" element={<ProcessStart />} />
          <Route path="/monitor" element={<Monitor />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Route>
      ) : (
        <Route path="*" element={<Navigate to="/login" replace />} />
      )}
    </Routes>
  );
};

export default App;
