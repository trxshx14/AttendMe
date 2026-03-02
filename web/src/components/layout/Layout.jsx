import React, { useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import Sidebar from './Sidebar';
import Topbar from './Topbar';

const PAGE_TITLES = {
  '/dashboard': ['Dashboard', 'Welcome back'],
  '/take-attendance': ['Take Attendance', 'Record student attendance for today'],
  '/history': ['Attendance History', 'Browse and manage past records'],
  '/reports': ['Reports', 'Daily attendance summary & trends'],
  '/users': ['Manage Users', 'Add, edit, and remove user accounts'],
};

const Layout = () => {
  const location = useLocation();
  const [currentPage, setCurrentPage] = useState(
    location.pathname.replace('/', '') || 'dashboard'
  );

  const [title, subtitle] = PAGE_TITLES[location.pathname] || ['', ''];

  return (
    <div className="app-shell">
      <Sidebar currentPage={currentPage} onPageChange={setCurrentPage} />
      <div className="main-area">
        <Topbar title={title} subtitle={subtitle} />
        <div className="page-content">
          <Outlet />
        </div>
      </div>
    </div>
  );
};

export default Layout;