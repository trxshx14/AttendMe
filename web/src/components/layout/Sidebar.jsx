import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import './Sidebar.css';

const Sidebar = ({ currentPage, onPageChange }) => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const teacherNav = [
    
  ];

  const adminNav = [
    ...teacherNav,
    { id: 'dashboard', icon: '🏠', label: 'Dashboard', path: '/admin/dashboard' },
    { id: 'manage-classes', icon: '✏️', label: 'Manage Classes', path: '/admin/classes' },
    { id: 'manage-users', icon: '👥', label: 'Manage Teachers', path: '/admin/users' },
    { id: 'reports', icon: '📊', label: 'Reports', path: '/admin/reports' },
  ];

  const nav = user?.role === 'admin' ? adminNav : teacherNav;

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const handleNavigation = (item) => {
    onPageChange?.(item.id);
    navigate(item.path);
  };

  if (!user) return null;

  return (
    <div className="sidebar">
      <div className="sidebar-logo">
        <div className="sidebar-logo-icon">A</div>
        <div className="sidebar-logo-text">AttendMe</div>
      </div>

      <div className="sidebar-user">
        <div className="user-avatar">{user.avatar}</div>
        <div className="user-info">
          <div className="user-name">{user.name}</div>
          <div className="user-role">{user.role}</div>
        </div>
      </div>

      <div className="sidebar-nav">
        <div className="nav-section">
          <div className="nav-section-label">Navigation</div>
        </div>
        {nav.map((item) => (
          <button
            key={item.id}
            className={`nav-item ${currentPage === item.id ? 'active' : ''}`}
            onClick={() => handleNavigation(item)}
          >
            <span className="nav-icon">{item.icon}</span>
            {item.label}
          </button>
        ))}
      </div>

      <div className="sidebar-footer">
        <button className="btn-logout" onClick={handleLogout}>
          <span>🚪</span> Sign Out
        </button>
      </div>
    </div>
  );
};

export default Sidebar;