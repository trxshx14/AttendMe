import React, { useState, useEffect } from 'react';
import { userService } from '../../services/userService';
import {
  Users, ShieldCheck, UserCircle2, CheckCircle2,
  Search, Pencil, Trash2, X, Plus, Key,
  ToggleLeft, ToggleRight, Eye, EyeOff
} from 'lucide-react';
import './ManageUsers.css';

const ManageUsers = () => {
  const [users, setUsers]           = useState([]);
  const [loading, setLoading]       = useState(true);
  const [saving, setSaving]         = useState(false);
  const [error, setError]           = useState('');
  const [showModal, setShowModal]   = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [formData, setFormData]     = useState({ fullName: '', email: '', role: 'TEACHER', password: '' });
  const [showPassword, setShowPassword] = useState(false);

  const [searchTerm, setSearchTerm]     = useState('');
  const [filterRole, setFilterRole]     = useState('all');
  const [filterStatus, setFilterStatus] = useState('all');

  const token = localStorage.getItem('accessToken');

  useEffect(() => { fetchUsers(); }, []);

  /* ── Fetch users ─────────────────────────────────────── */
  const fetchUsers = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await userService.getAllUsers();
      if (response?.success) {
        const raw = response.data || [];

        // Debug: log the first user so we can see exact field names from backend
        if (raw.length > 0) {
          // Log every user's active-related fields clearly
          console.log('[ManageUsers] ALL USERS STATUS CHECK:');
          raw.forEach(u => {
            console.log(
              `  ${u.fullName} → active=${u.active} | isActive=${u.isActive} | enabled=${u.enabled} | status=${u.status} | accountNonLocked=${u.accountNonLocked}`
            );
          });
        }

        // Determine active status strictly:
        // Only trust `active` if it's explicitly a boolean on the object.
        // Fall back to `isActive`, then `enabled`, then `status === 'ACTIVE'`.
        // Do NOT chain with ?? false until we've tried all known fields.
        const normalized = raw.map(u => {
          let isActive = false;
          if (typeof u.active   === 'boolean') isActive = u.active;
          else if (typeof u.isActive === 'boolean') isActive = u.isActive;
          else if (typeof u.enabled  === 'boolean') isActive = u.enabled;
          else if (typeof u.status   === 'string')  isActive = u.status.toUpperCase() === 'ACTIVE';
          return { ...u, active: isActive };
        });
        setUsers(normalized);
      } else {
        setError('Failed to load users');
      }
    } catch { setError('Error loading users'); }
    finally { setLoading(false); }
  };

  /* ── Filters ─────────────────────────────────────────── */
  const filteredUsers = users.filter(user => {
    const matchSearch = !searchTerm ||
      user.fullName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      user.email?.toLowerCase().includes(searchTerm.toLowerCase());
    const matchRole   = filterRole === 'all' || user.role === filterRole;
    const matchStatus = filterStatus === 'all' ||
      (filterStatus === 'active'   &&  user.active) ||
      (filterStatus === 'inactive' && !user.active);
    return matchSearch && matchRole && matchStatus;
  });

  const clearFilters = () => { setSearchTerm(''); setFilterRole('all'); setFilterStatus('all'); };
  const hasFilters   = searchTerm || filterRole !== 'all' || filterStatus !== 'all';

  const totalAdmins   = users.filter(u => u.role === 'ADMIN').length;
  const totalTeachers = users.filter(u => u.role === 'TEACHER').length;
  const totalActive   = users.filter(u => u.active).length;

  /* ── Modal helpers ───────────────────────────────────── */
  const handleOpenModal = (user = null) => {
    setEditingUser(user);
    setFormData(user
      ? { fullName: user.fullName, email: user.email, role: user.role, password: '' }
      : { fullName: '', email: '', role: 'TEACHER', password: '' }
    );
    setShowModal(true);
    setShowPassword(false);
    setError('');
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setEditingUser(null);
    setFormData({ fullName: '', email: '', role: 'TEACHER', password: '' });
    setError('');
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  /* ── Submit create/edit ──────────────────────────────── */
  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      let response;
      if (editingUser) {
        response = await userService.updateUser(editingUser.userId, {
          fullName: formData.fullName,
          email:    formData.email,
          role:     formData.role,
          ...(formData.password && { password: formData.password }),
        });
      } else {
        response = await userService.createUser(formData);
      }
      if (response?.success) { handleCloseModal(); await fetchUsers(); }
      else setError(response?.message || 'Failed to save user');
    } catch (err) { setError(err.message || 'An error occurred'); }
    finally { setSaving(false); }
  };

  /* ── Delete ──────────────────────────────────────────── */
  const handleDeleteUser = async (userId, userName) => {
    if (!window.confirm(`Are you sure you want to delete ${userName}?`)) return;
    setError('');
    try {
      const response = await userService.deleteUser(userId);
      if (response?.success) {
        // Optimistic remove
        setUsers(prev => prev.filter(u => u.userId !== userId));
      } else setError('Failed to delete user');
    } catch { setError('Error deleting user'); }
  };

  /* ── Toggle active / inactive ────────────────────────── */
  const handleToggleActive = async (userId, currentActive) => {
    setError('');
    // Optimistic UI update first so the toggle feels instant
    setUsers(prev => prev.map(u =>
      u.userId === userId ? { ...u, active: !currentActive } : u
    ));
    try {
      let response;
      if (currentActive) {
        // Currently active → deactivate
        response = await userService.deactivateUser(userId);
      } else {
        // Currently inactive → activate
        response = await userService.activateUser(userId);
      }

      if (response?.success) {
        // Sync the real value back from the server response if available
        const d = response.data || {};
        let serverActive;
        if      (typeof d.active   === 'boolean') serverActive = d.active;
        else if (typeof d.isActive === 'boolean') serverActive = d.isActive;
        else if (typeof d.enabled  === 'boolean') serverActive = d.enabled;
        else if (typeof d.status   === 'string')  serverActive = d.status.toUpperCase() === 'ACTIVE';
        else serverActive = !currentActive; // fallback: trust the toggle worked

        console.log('[ManageUsers] toggle response active =', serverActive, '| raw:', d);

        setUsers(prev => prev.map(u =>
          u.userId === userId ? { ...u, active: serverActive } : u
        ));
      } else {
        // Revert optimistic update on failure
        setUsers(prev => prev.map(u =>
          u.userId === userId ? { ...u, active: currentActive } : u
        ));
        setError(response?.message || 'Failed to update user status');
      }
    } catch {
      // Revert on error
      setUsers(prev => prev.map(u =>
        u.userId === userId ? { ...u, active: currentActive } : u
      ));
      setError('Error updating user status');
    }
  };

  /* ── Reset password ──────────────────────────────────── */
  const handleResetPassword = async (userId) => {
    if (!window.confirm('Reset password for this user? They will receive a temporary password.')) return;
    setError('');
    try {
      const response = await userService.resetPassword(userId);
      if (response?.success) {
        alert(`Password reset!\n\nTemporary Password: ${response.data?.tempPassword || response.tempPassword || '(check server logs)'}`);
      } else setError('Error resetting password');
    } catch { setError('Error resetting password'); }
  };

  const getInitials = (name) =>
    name ? name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2) : '??';

  if (loading && users.length === 0) return <div className="mu-loading">Loading users...</div>;

  return (
    <div className="manage-users">

      {/* Header */}
      <div className="mu-page-header">
        <div className="mu-header-left">
          <h1>Manage Teachers</h1>
          <p className="mu-page-description">Add and manage teacher and administrator accounts</p>
        </div>
        <button className="mu-btn-primary" onClick={() => handleOpenModal()}>
          <Plus size={16} /> New Teacher/Admin
        </button>
      </div>

      {error && (
        <div className="mu-error">
          {error}
          <button className="mu-error-close" onClick={() => setError('')}><X size={13} /></button>
        </div>
      )}

      {/* Stats */}
      <div className="mu-stats-row">
        <div className="mu-stat-card">
          <div className="mu-stat-icon blue"><Users size={22} color="#0F2D5E" strokeWidth={2} /></div>
          <div>
            <div className="mu-stat-value">{users.length}</div>
            <div className="mu-stat-label">Total Users</div>
          </div>
        </div>
        <div className="mu-stat-card">
          <div className="mu-stat-icon purple"><ShieldCheck size={22} color="#0F2D5E" strokeWidth={2} /></div>
          <div>
            <div className="mu-stat-value">{totalAdmins}</div>
            <div className="mu-stat-label">Admins</div>
          </div>
        </div>
        <div className="mu-stat-card">
          <div className="mu-stat-icon blue"><UserCircle2 size={22} color="#0F2D5E" strokeWidth={2} /></div>
          <div>
            <div className="mu-stat-value">{totalTeachers}</div>
            <div className="mu-stat-label">Teachers</div>
          </div>
        </div>
        <div className="mu-stat-card">
          <div className="mu-stat-icon green"><CheckCircle2 size={22} color="#0F2D5E" strokeWidth={2} /></div>
          <div>
            <div className="mu-stat-value">{totalActive}</div>
            <div className="mu-stat-label">Active</div>
          </div>
        </div>
      </div>

      {/* Toolbar */}
      <div className="mu-toolbar">
        <div className="mu-search-wrap">
          <span className="mu-search-icon"><Search size={16} color="#64748B" /></span>
          <input
            type="text"
            className="mu-search-input"
            placeholder="Search by name or email..."
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
          />
          {searchTerm && (
            <button className="mu-clear-btn" onClick={() => setSearchTerm('')}><X size={12} /></button>
          )}
        </div>
        <div className="mu-toolbar-divider" />
        <select className="mu-filter-select" value={filterRole} onChange={e => setFilterRole(e.target.value)}>
          <option value="all">All Roles</option>
          <option value="ADMIN">Admin</option>
          <option value="TEACHER">Teacher</option>
        </select>
        <div className="mu-toolbar-divider" />
        <select className="mu-filter-select" value={filterStatus} onChange={e => setFilterStatus(e.target.value)}>
          <option value="all">All Status</option>
          <option value="active">Active</option>
          <option value="inactive">Inactive</option>
        </select>
        {hasFilters && <button className="mu-clear-filters" onClick={clearFilters}>Clear</button>}
      </div>

      <p className="mu-results-count">
        Showing <strong>{filteredUsers.length}</strong> of <strong>{users.length}</strong> users
      </p>

      {/* Table */}
      <div className="mu-table-wrap">
        <table className="mu-table">
          <thead>
            <tr>
              <th>User</th>
              <th>Email</th>
              <th>Role</th>
              <th>Status</th>
              <th>Last Login</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredUsers.length > 0 ? filteredUsers.map(user => (
              <tr key={user.userId}>
                <td>
                  <div className="mu-user-cell">
                    <div className="mu-avatar">{getInitials(user.fullName)}</div>
                    <span className="mu-user-name">{user.fullName}</span>
                  </div>
                </td>
                <td className="mu-email">{user.email}</td>
                <td>
                  <span className={`mu-role-badge mu-role-${user.role?.toLowerCase()}`}>
                    {user.role === 'ADMIN'
                      ? <><ShieldCheck size={12} /> Admin</>
                      : <><UserCircle2 size={12} /> Teacher</>
                    }
                  </span>
                </td>
                <td>
                  <span className={`mu-status-badge ${user.active ? 'mu-active' : 'mu-inactive'}`}>
                    <span className="mu-status-dot" />
                    {user.active ? 'Active' : 'Inactive'}
                  </span>
                </td>
                <td className="mu-last-login">
                  {user.lastLogin
                    ? new Date(user.lastLogin).toLocaleDateString('en-PH', { month: 'short', day: 'numeric', year: 'numeric' })
                    : '—'}
                </td>
                <td>
                  <div className="mu-actions">
                    <button className="mu-btn-icon" onClick={() => handleOpenModal(user)} title="Edit">
                      <Pencil size={15} color="#0F2D5E" />
                    </button>
                    <button
                      className="mu-btn-icon"
                      onClick={() => handleToggleActive(user.userId, user.active)}
                      title={user.active ? 'Deactivate' : 'Activate'}
                    >
                      {user.active
                        ? <ToggleRight size={20} color="#16a34a" />
                        : <ToggleLeft  size={20} color="#dc2626" />
                      }
                    </button>
                    <button className="mu-btn-icon" onClick={() => handleResetPassword(user.userId)} title="Reset Password">
                      <Key size={15} color="#0F2D5E" />
                    </button>
                    <button className="mu-btn-icon mu-delete" onClick={() => handleDeleteUser(user.userId, user.fullName)} title="Delete">
                      <Trash2 size={15} color="#ef4444" />
                    </button>
                  </div>
                </td>
              </tr>
            )) : (
              <tr>
                <td colSpan="6">
                  <div className="mu-empty">
                    <p>{hasFilters ? 'No users match your filters.' : 'No users found.'}</p>
                    {hasFilters && <button className="mu-btn-outline" onClick={clearFilters}>Clear Filters</button>}
                  </div>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Add/Edit Modal */}
      {showModal && (
        <div className="mu-modal-overlay" onClick={handleCloseModal}>
          <div className="mu-modal" onClick={e => e.stopPropagation()}>
            <div className="mu-modal-header">
              <h3>{editingUser ? 'Edit User' : 'Register New User'}</h3>
              <button className="mu-modal-close" onClick={handleCloseModal}><X size={16} /></button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="mu-modal-body">
                <div className="mu-form-group">
                  <label className="mu-form-label">Full Name *</label>
                  <input type="text" name="fullName" className="mu-form-input" value={formData.fullName}
                    onChange={handleInputChange} required placeholder="Enter full name" />
                </div>
                <div className="mu-form-group">
                  <label className="mu-form-label">Email Address *</label>
                  <input type="email" name="email" className="mu-form-input" value={formData.email}
                    onChange={handleInputChange} required placeholder="user@school.edu" />
                  {!editingUser && <small className="mu-form-hint">Login credentials will be sent to this email</small>}
                </div>
                <div className="mu-form-group">
                  <label className="mu-form-label">Role *</label>
                  <select name="role" className="mu-form-input" value={formData.role} onChange={handleInputChange} required>
                    <option value="TEACHER">Teacher</option>
                    <option value="ADMIN">Admin</option>
                  </select>
                </div>
                {!editingUser ? (
                  <div className="mu-form-group">
                    <label className="mu-form-label">Temporary Password</label>
                    <div className="mu-password-row">
                      <input
                        type={showPassword ? 'text' : 'password'}
                        name="password" className="mu-form-input"
                        value={formData.password} onChange={handleInputChange}
                        placeholder="Leave empty for auto-generated"
                      />
                      <button type="button" className="mu-btn-icon" onClick={() => setShowPassword(!showPassword)}>
                        {showPassword ? <EyeOff size={15} color="#0F2D5E" /> : <Eye size={15} color="#0F2D5E" />}
                      </button>
                    </div>
                    <small className="mu-form-hint">If left empty, a random password will be generated</small>
                  </div>
                ) : (
                  <div className="mu-form-group">
                    <label className="mu-form-label">New Password (optional)</label>
                    <input type="password" name="password" className="mu-form-input"
                      value={formData.password} onChange={handleInputChange}
                      placeholder="Leave empty to keep current" />
                  </div>
                )}
              </div>
              <div className="mu-modal-footer">
                <button type="button" className="mu-btn-outline" onClick={handleCloseModal}>Cancel</button>
                <button type="submit" className="mu-btn-primary" disabled={saving}>
                  {saving ? 'Saving...' : (editingUser ? 'Update User' : 'Create User')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default ManageUsers;