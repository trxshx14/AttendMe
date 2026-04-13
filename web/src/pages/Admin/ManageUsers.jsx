import React, { useState, useEffect, useRef } from 'react';
import { userService } from '../../services/userService';
import { useAuth } from '../../context/AuthContext';
import {
  Users, ShieldCheck, UserCircle2, CheckCircle2,
  Search, Pencil, Trash2, X, Plus,
  Eye, EyeOff, Camera, Upload
} from 'lucide-react';
import './ManageUsers.css';

const ManageUsers = () => {
  const { user: currentUser, updateUser } = useAuth();

  const [users, setUsers]               = useState([]);
  const [loading, setLoading]           = useState(true);
  const [saving, setSaving]             = useState(false);
  const [error, setError]               = useState('');
  const [showModal, setShowModal]       = useState(false);
  const [editingUser, setEditingUser]   = useState(null);
  const [formData, setFormData]         = useState({ fullName: '', email: '', role: 'TEACHER', password: '' });
  const [showPassword, setShowPassword] = useState(false);

  const [profilePicFile, setProfilePicFile]       = useState(null);
  const [profilePicPreview, setProfilePicPreview] = useState(null);
  const [uploadingPic, setUploadingPic]           = useState(false);
  const fileInputRef = useRef(null);

  const [searchTerm, setSearchTerm]     = useState('');
  const [filterRole, setFilterRole]     = useState('all');
  const [filterStatus, setFilterStatus] = useState('all');

  useEffect(() => {
    fetchUsers();
    // ✅ Auto-refresh every 30 seconds to keep online status current
    const interval = setInterval(fetchUsers, 30000);
    return () => clearInterval(interval);
  }, []);

  /* ── Fetch users ─────────────────────────────────────── */
  const fetchUsers = async () => {
    setLoading(prev => users.length === 0 ? true : prev); // only show loading on first load
    setError('');
    try {
      const response = await userService.getAllUsers();
      if (response?.success) {
        const raw = response.data || [];
        const normalized = raw.map(u => ({
          ...u,
          // ✅ isOnline is the source of truth for active session status
          isOnline: u.isOnline ?? false,
          active: typeof u.isActive === 'boolean' ? u.isActive
                : typeof u.active   === 'boolean' ? u.active
                : true,
        }));
        setUsers(normalized);

        if (currentUser?.id) {
          const me = normalized.find(u => u.userId === currentUser.id);
          if (me && me.profilePicUrl && me.profilePicUrl !== currentUser.profilePicUrl) {
            updateUser({ profilePicUrl: me.profilePicUrl });
          }
        }
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
    // ✅ Filter by online status instead of isActive
    const matchStatus = filterStatus === 'all' ||
      (filterStatus === 'active'   &&  user.isOnline) ||
      (filterStatus === 'inactive' && !user.isOnline);
    return matchSearch && matchRole && matchStatus;
  });

  const clearFilters = () => { setSearchTerm(''); setFilterRole('all'); setFilterStatus('all'); };
  const hasFilters   = searchTerm || filterRole !== 'all' || filterStatus !== 'all';

  const totalAdmins   = users.filter(u => u.role === 'ADMIN').length;
  const totalTeachers = users.filter(u => u.role === 'TEACHER').length;
  // ✅ Active count = currently online
  const totalActive   = users.filter(u => u.isOnline).length;

  /* ── Profile picture handlers ────────────────────────── */
  const handlePicChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) { setError('Please select a valid image file'); return; }
    if (file.size > 2 * 1024 * 1024)    { setError('Image must be smaller than 2MB');    return; }
    setProfilePicFile(file);
    const reader = new FileReader();
    reader.onloadend = () => setProfilePicPreview(reader.result);
    reader.readAsDataURL(file);
  };

  const handleRemovePic = () => {
    setProfilePicFile(null);
    setProfilePicPreview(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  /* ── Modal helpers ───────────────────────────────────── */
  const handleOpenModal = (user = null) => {
    setEditingUser(user);
    setFormData(user
      ? { fullName: user.fullName, email: user.email, role: user.role, password: '' }
      : { fullName: '', email: '', role: 'TEACHER', password: '' }
    );
    setProfilePicFile(null);
    setProfilePicPreview(user?.profilePicUrl || user?.avatarUrl || null);
    setShowModal(true);
    setShowPassword(false);
    setError('');
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setEditingUser(null);
    setFormData({ fullName: '', email: '', role: 'TEACHER', password: '' });
    setProfilePicFile(null);
    setProfilePicPreview(null);
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
      let savedUserId = null;

      if (editingUser) {
        const response = await userService.updateUser(editingUser.userId, {
          fullName: formData.fullName,
          email:    formData.email,
          role:     formData.role,
          ...(formData.password && { password: formData.password }),
        });
        if (!response?.success) { setError(response?.message || 'Failed to save user'); setSaving(false); return; }
        savedUserId = response.data?.userId || editingUser.userId;
      } else {
        const response = await userService.createUser(formData);
        if (!response?.success) { setError(response?.message || 'Failed to save user'); setSaving(false); return; }
        savedUserId = response.data?.userId;
      }

      if (profilePicFile && savedUserId) {
        setUploadingPic(true);
        try {
          const picFormData = new FormData();
          picFormData.append('file', profilePicFile);
          const picResponse = await userService.uploadProfilePicture(savedUserId, picFormData);
          if (picResponse?.success && currentUser?.id === savedUserId) {
            updateUser({ profilePicUrl: picResponse.data?.profilePicUrl });
          }
        } catch (picErr) {
          console.warn('Profile picture upload failed:', picErr);
          setError('User saved but profile picture upload failed.');
        } finally { setUploadingPic(false); }
      }

      handleCloseModal();
      await fetchUsers();
    } catch (err) {
      setError(err.message || 'An error occurred');
    } finally { setSaving(false); }
  };

  /* ── Delete ──────────────────────────────────────────── */
  const handleDeleteUser = async (userId, userName) => {
    if (!window.confirm(`Are you sure you want to delete ${userName}?`)) return;
    setError('');
    try {
      const response = await userService.deleteUser(userId);
      if (response?.success) setUsers(prev => prev.filter(u => u.userId !== userId));
      else setError('Failed to delete user');
    } catch { setError('Error deleting user'); }
  };

  const getInitials = (name) =>
    name ? name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2) : '??';

  const UserAvatar = ({ user }) => {
    const [imgError, setImgError] = useState(false);
    const picUrl = user?.profilePicUrl || user?.avatarUrl;
    if (picUrl && !imgError) {
      return (
        <img src={`${picUrl}?t=${Date.now()}`} alt={user.fullName}
          className="mu-avatar mu-avatar-img" onError={() => setImgError(true)} />
      );
    }
    return <div className="mu-avatar">{getInitials(user.fullName)}</div>;
  };

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
          <div><div className="mu-stat-value">{users.length}</div><div className="mu-stat-label">Total Users</div></div>
        </div>
        <div className="mu-stat-card">
          <div className="mu-stat-icon purple"><ShieldCheck size={22} color="#0F2D5E" strokeWidth={2} /></div>
          <div><div className="mu-stat-value">{totalAdmins}</div><div className="mu-stat-label">Admins</div></div>
        </div>
        <div className="mu-stat-card">
          <div className="mu-stat-icon blue"><UserCircle2 size={22} color="#0F2D5E" strokeWidth={2} /></div>
          <div><div className="mu-stat-value">{totalTeachers}</div><div className="mu-stat-label">Teachers</div></div>
        </div>
        <div className="mu-stat-card">
          <div className="mu-stat-icon green"><CheckCircle2 size={22} color="#0F2D5E" strokeWidth={2} /></div>
          {/* ✅ Now shows online count */}
          <div><div className="mu-stat-value">{totalActive}</div><div className="mu-stat-label">Online Now</div></div>
        </div>
      </div>

      {/* Toolbar */}
      <div className="mu-toolbar">
        <div className="mu-search-wrap">
          <span className="mu-search-icon"><Search size={16} color="#64748B" /></span>
          <input type="text" className="mu-search-input" placeholder="Search by name or email..."
            value={searchTerm} onChange={e => setSearchTerm(e.target.value)} />
          {searchTerm && <button className="mu-clear-btn" onClick={() => setSearchTerm('')}><X size={12} /></button>}
        </div>
        <div className="mu-toolbar-divider" />
        <select className="mu-filter-select" value={filterRole} onChange={e => setFilterRole(e.target.value)}>
          <option value="all">All Roles</option>
          <option value="ADMIN">Admin</option>
          <option value="TEACHER">Teacher</option>
        </select>
        <div className="mu-toolbar-divider" />
        {/* ✅ Filter now filters by online status */}
        <select className="mu-filter-select" value={filterStatus} onChange={e => setFilterStatus(e.target.value)}>
          <option value="all">All Status</option>
          <option value="active">Online</option>
          <option value="inactive">Offline</option>
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
              <th>User</th><th>Email</th><th>Role</th>
              {/* ✅ Changed Status header */}
              <th>Status</th>
              <th>Last Login</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredUsers.length > 0 ? filteredUsers.map(user => (
              <tr key={user.userId}>
                <td>
                  <div className="mu-user-cell">
                    <div className="mu-avatar-wrap">
                      <UserAvatar user={user} />
                      {/* ✅ Online indicator dot on avatar */}
                      {user.isOnline && (
                        <span style={{
                          position: 'absolute', bottom: 0, right: 0,
                          width: '10px', height: '10px', borderRadius: '50%',
                          background: '#16a34a', border: '2px solid #fff'
                        }} />
                      )}
                    </div>
                    <span className="mu-user-name">{user.fullName}</span>
                  </div>
                </td>
                <td className="mu-email">{user.email}</td>
                <td>
                  <span className={`mu-role-badge mu-role-${user.role?.toLowerCase()}`}>
                    {user.role === 'ADMIN'
                      ? <><ShieldCheck size={12} /> Admin</>
                      : <><UserCircle2 size={12} /> Teacher</>}
                  </span>
                </td>
                <td>
                  {/* ✅ Status now shows Online/Offline based on isOnline — no toggle button */}
                  <span className={`mu-status-badge ${user.isOnline ? 'mu-active' : 'mu-inactive'}`}>
                    <span className="mu-status-dot" />
                    {user.isOnline ? 'Online' : 'Offline'}
                  </span>
                </td>
                <td className="mu-last-login">
                  {user.lastLogin
                    ? new Date(user.lastLogin).toLocaleDateString('en-PH', {
                        month: 'short', day: 'numeric', year: 'numeric'
                      })
                    : '—'}
                </td>
                <td>
                  <div className="mu-actions">
                    <button className="mu-btn-icon" onClick={() => handleOpenModal(user)} title="Edit">
                      <Pencil size={15} color="#0F2D5E" />
                    </button>
                    <button className="mu-btn-icon mu-delete"
                      onClick={() => handleDeleteUser(user.userId, user.fullName)} title="Delete">
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

      {/* Add/Edit Modal — unchanged */}
      {showModal && (
        <div className="mu-modal-overlay" onClick={handleCloseModal}>
          <div className="mu-modal" onClick={e => e.stopPropagation()}>
            <div className="mu-modal-header">
              <h3>{editingUser ? 'Edit User' : 'Register New User'}</h3>
              <button className="mu-modal-close" onClick={handleCloseModal}><X size={16} /></button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="mu-modal-body">
                <div className="mu-form-group mu-pic-group">
                  <label className="mu-form-label">Profile Photo</label>
                  <div className="mu-pic-upload-area">
                    <div className="mu-pic-preview-wrap">
                      {profilePicPreview
                        ? <img src={profilePicPreview} alt="Preview" className="mu-pic-preview" />
                        : <div className="mu-pic-placeholder"><Camera size={28} color="#94A3B8" /><span>No photo</span></div>
                      }
                      <button type="button" className="mu-pic-overlay-btn"
                        onClick={() => fileInputRef.current?.click()} title="Upload photo">
                        <Camera size={16} color="#fff" />
                      </button>
                    </div>
                    <div className="mu-pic-actions">
                      <input ref={fileInputRef} type="file" accept="image/*"
                        style={{ display: 'none' }} onChange={handlePicChange} />
                      <button type="button" className="mu-btn-outline mu-pic-btn"
                        onClick={() => fileInputRef.current?.click()}>
                        <Upload size={14} /> Choose Photo
                      </button>
                      {profilePicPreview && (
                        <button type="button" className="mu-btn-outline mu-pic-btn mu-pic-remove"
                          onClick={handleRemovePic}>
                          <X size={14} /> Remove
                        </button>
                      )}
                      <small className="mu-form-hint">JPG, PNG or GIF · Max 2MB</small>
                    </div>
                  </div>
                </div>
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
                      <input type={showPassword ? 'text' : 'password'} name="password" className="mu-form-input"
                        value={formData.password} onChange={handleInputChange}
                        placeholder="Leave empty for auto-generated" />
                      <button type="button" className="mu-btn-icon" onClick={() => setShowPassword(!showPassword)}>
                        {showPassword ? <EyeOff size={15} color="#0F2D5E" /> : <Eye size={15} color="#0F2D5E" />}
                      </button>
                    </div>
                    <small className="mu-form-hint">If left empty, a random password will be generated</small>
                  </div>
                ) : (
                  <div className="mu-form-group">
                    <label className="mu-form-label">New Password</label>
                    <input type="password" name="password" className="mu-form-input"
                      value={formData.password} onChange={handleInputChange}
                      placeholder="Enter new password to reset — teacher will be notified by email" />
                  </div>
                )}
              </div>
              <div className="mu-modal-footer">
                <button type="button" className="mu-btn-outline" onClick={handleCloseModal}>Cancel</button>
                <button type="submit" className="mu-btn-primary" disabled={saving || uploadingPic}>
                  {uploadingPic ? 'Uploading photo...' : saving ? 'Saving...' : (editingUser ? 'Update User' : 'Create User')}
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