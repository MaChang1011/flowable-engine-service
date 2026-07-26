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
import ProcessManage from '@/pages/ProcessManage';
import ProcessDefManage from '@/pages/ProcessDefManage';
import ApprovalTemplate from '@/pages/ApprovalTemplate';
import FormSchemaManage from '@/pages/FormSchemaManage';
import Historic from '@/pages/Historic';
import Committee from '@/pages/Committee';
import Escalation from '@/pages/Escalation';
import UserLifecycle from '@/pages/UserLifecycle';

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
          <Route path="/process-manage" element={<ProcessManage />} />
          <Route path="/process-def-manage" element={<ProcessDefManage />} />
          <Route path="/approval-template" element={<ApprovalTemplate />} />
          <Route path="/form-schema-manage" element={<FormSchemaManage />} />
          <Route path="/historic" element={<Historic />} />
          <Route path="/committee" element={<Committee />} />
          <Route path="/escalation" element={<Escalation />} />
          <Route path="/user-lifecycle" element={<UserLifecycle />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Route>
      ) : (
        <Route path="*" element={<Navigate to="/login" replace />} />
      )}
    </Routes>
  );
};

export default App;
