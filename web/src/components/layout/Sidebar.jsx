import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import './Sidebar.css';
import logo from '../../assets/dashboardlogo.png';

const Sidebar = ({ currentPage, onPageChange }) => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [showLogoutModal, setShowLogoutModal] = useState(false);

  const teacherNav = [
    { id: 'dashboard',       icon: '🏠', label: 'Dashboard',          path: '/teacher/dashboard' },
    { id: 'take-attendance', icon: '✏️', label: 'Take Attendance',    path: '/teacher/take-attendance' },
    { id: 'history',         icon: '📋', label: 'Attendance History', path: '/teacher/history' },
    { id: 'reports',         icon: '📊', label: 'Reports',            path: '/teacher/reports' },
  ];

  const adminNav = [
    { id: 'dashboard',      icon: '🏠', label: 'Dashboard',       path: '/admin/dashboard' },
    { id: 'manage-classes', icon: '📚', label: 'Manage Classes',  path: '/admin/classes' },
    { id: 'manage-users',   icon: '👥', label: 'Manage Teachers', path: '/admin/users' },
    { id: 'reports',        icon: '📊', label: 'Reports',         path: '/admin/reports' },
  ];

  const nav = user?.role === 'admin' ? adminNav : teacherNav;

  const handleNavigation = (item) => {
    onPageChange?.(item.id);
    navigate(item.path);
  };

  const confirmLogout = async () => {
    setShowLogoutModal(false);
    await logout();
    navigate('/login');
  };

  if (!user) return null;

  const picUrl = user.profilePicUrl
    ? `${user.profilePicUrl}?t=${Date.now()}`
    : null;

  return (
    <>
      <div className="sidebar">
        <div className="sidebar-logo">
          <img src={logo} alt="AttendMe" className="sidebar-logo-image" />
          <div className="sidebar-logo-text">AttendMe</div>
        </div>

        <div className="sidebar-user">
          {picUrl ? (
            <img
              src={picUrl}
              alt={user.name}
              className="user-avatar user-avatar-img"
              onError={(e) => {
                e.target.style.display = 'none';
                e.target.nextSibling.style.display = 'flex';
              }}
            />
          ) : null}
          <div
            className="user-avatar"
            style={{ display: picUrl ? 'none' : 'flex' }}
          >
            {user.avatar}
          </div>
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
          <button className="btn-logout" onClick={() => setShowLogoutModal(true)}>
            <span>🚪</span> Sign Out
          </button>
        </div>
      </div>

      {showLogoutModal && (
        <div className="logout-overlay" onClick={() => setShowLogoutModal(false)}>
          <div className="logout-modal" onClick={(e) => e.stopPropagation()}>
            <div className="logout-icon">
              <span>🚪</span>
            </div>
            <h3>Sign out?</h3>
            <p>You'll be returned to the login screen.</p>
            <div className="logout-modal-actions">
              <button className="btn-cancel" onClick={() => setShowLogoutModal(false)}>
                Cancel
              </button>
              <button className="btn-confirm-logout" onClick={confirmLogout}>
                Sign out
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default Sidebar;