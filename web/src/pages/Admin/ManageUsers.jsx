import React, { useState, useEffect } from 'react';
import { userService } from '../../services/userService';
import './ManageUsers.css';

const ManageUsers = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    role: 'TEACHER',
    password: ''
  });
  const [showPassword, setShowPassword] = useState(false);

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      console.log('🔄 Fetching users...');
      const response = await userService.getAllUsers();
      console.log('📥 Fetch users response:', response);
      
      if (response && response.success) {
        console.log('✅ Users data:', response.data);
        setUsers(response.data || []);
      } else {
        console.error('❌ Failed to load users:', response);
        setError('Failed to load users');
      }
    } catch (err) {
      console.error('❌ Error loading users:', err);
      setError('Error loading users');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenModal = (user = null) => {
    if (user) {
      setEditingUser(user);
      setFormData({
        fullName: user.fullName,
        email: user.email,
        role: user.role,
        password: ''
      });
    } else {
      setEditingUser(null);
      setFormData({
        fullName: '',
        email: '',
        role: 'TEACHER',
        password: ''
      });
    }
    setShowModal(true);
    setShowPassword(false);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setEditingUser(null);
    setFormData({
      fullName: '',
      email: '',
      role: 'TEACHER',
      password: ''
    });
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      console.log('📝 Submitting form:', editingUser ? 'Edit' : 'Create', formData);
      
      let response;
      
      if (editingUser) {
        // Update existing user
        console.log('✏️ Updating user:', editingUser.userId);
        response = await userService.updateUser(editingUser.userId, {
          fullName: formData.fullName,
          email: formData.email,
          role: formData.role,
          ...(formData.password && { password: formData.password })
        });
        console.log('📥 Update response:', response);
      } else {
        // Create new user
        console.log('➕ Creating new user');
        response = await userService.createUser(formData);
        console.log('📥 Create response:', response);
      }

      if (response && response.success) {
        // Close modal first
        handleCloseModal();
        
        // Small delay to ensure modal is closed
        setTimeout(async () => {
          // Refresh the user list
          console.log('🔄 Refreshing user list after save...');
          await fetchUsers();
          
          // Show success message
          alert(editingUser ? 'User updated successfully!' : 'User created successfully!');
        }, 100);
      } else {
        console.error('❌ Save failed:', response);
        setError(response?.message || 'Failed to save user');
      }
    } catch (err) {
      console.error('❌ Error saving user:', err);
      setError(err.message || 'An error occurred');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteUser = async (userId, userName) => {
    if (!window.confirm(`Are you sure you want to delete ${userName}?`)) {
      return;
    }

    setLoading(true);
    try {
      console.log('🗑️ Deleting user:', userId);
      const response = await userService.deleteUser(userId);
      console.log('📥 Delete response:', response);
      
      if (response && response.success) {
        alert('User deleted successfully');
        fetchUsers();
      } else {
        setError('Failed to delete user');
      }
    } catch (err) {
      console.error('❌ Error deleting user:', err);
      setError('Error deleting user');
    } finally {
      setLoading(false);
    }
  };

  const handleToggleActive = async (userId, currentStatus) => {
    try {
      console.log('🔄 Toggling active status for user:', userId, 'Current:', currentStatus);
      
      let response;
      if (currentStatus) {
        response = await userService.deactivateUser(userId);
      } else {
        response = await userService.activateUser(userId);
      }
      
      console.log('📥 Toggle response:', response);
      
      if (response && response.success) {
        fetchUsers(); // Refresh list
      } else {
        setError('Error updating user status');
      }
    } catch (err) {
      console.error('❌ Error updating user status:', err);
      setError('Error updating user status');
    }
  };

  const handleResetPassword = async (userId) => {
    if (!window.confirm('Reset password for this user? They will receive a temporary password.')) {
      return;
    }

    try {
      console.log('🔑 Resetting password for user:', userId);
      const response = await userService.resetPassword(userId);
      console.log('📥 Reset password response:', response);
      
      if (response && response.success) {
        alert(`Password reset successfully!\n\nTemporary Password: ${response.tempPassword}`);
      } else {
        setError('Error resetting password');
      }
    } catch (err) {
      console.error('❌ Error resetting password:', err);
      setError('Error resetting password');
    }
  };

  if (loading && users.length === 0) {
    return <div className="loading">Loading users...</div>;
  }

  return (
    <div className="manage-users">
      <div className="users-header">
        <h2>Manage Users</h2>
        <button 
          className="btn-primary"
          onClick={() => handleOpenModal()}
        >
          + Add New User
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      <div className="users-table-container">
        <table className="users-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Role</th>
              <th>Status</th>
              <th>Last Login</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {users && users.length > 0 ? (
              users.map(user => (
                <tr key={user.userId}>
                  <td>
                    <div className="user-name">
                      <div className="user-avatar-small">
                        {user.fullName ? user.fullName.split(' ').map(n => n[0]).join('').toUpperCase() : '??'}
                      </div>
                      {user.fullName}
                    </div>
                  </td>
                  <td>{user.email}</td>
                  <td>
                    <span className={`role-badge role-${user.role ? user.role.toLowerCase() : 'unknown'}`}>
                      {user.role || 'Unknown'}
                    </span>
                  </td>
                  <td>
                    <span className={`status-badge ${user.active ? 'status-active' : 'status-inactive'}`}>
                      {user.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td>{user.lastLogin ? new Date(user.lastLogin).toLocaleDateString() : 'Never'}</td>
                  <td className="actions">
                    <button 
                      className="btn-icon" 
                      onClick={() => handleOpenModal(user)}
                      title="Edit"
                    >
                      ✏️
                    </button>
                    <button 
                      className="btn-icon" 
                      onClick={() => handleToggleActive(user.userId, user.active)}
                      title={user.active ? 'Deactivate' : 'Activate'}
                    >
                      {user.active ? '🔴' : '🟢'}
                    </button>
                    <button 
                      className="btn-icon" 
                      onClick={() => handleResetPassword(user.userId)}
                      title="Reset Password"
                    >
                      🔑
                    </button>
                    <button 
                      className="btn-icon delete" 
                      onClick={() => handleDeleteUser(user.userId, user.fullName)}
                      title="Delete"
                    >
                      🗑️
                    </button>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="6" style={{ textAlign: 'center', padding: '40px' }}>
                  No users found. Click "Add New User" to create one.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Add/Edit User Modal */}
      {showModal && (
        <div className="modal-overlay" onClick={handleCloseModal}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{editingUser ? 'Edit User' : 'Add New User'}</h3>
              <button className="modal-close" onClick={handleCloseModal}>✕</button>
            </div>
            
            <form onSubmit={handleSubmit}>
              <div className="modal-body">
                <div className="form-group">
                  <label className="form-label">Full Name</label>
                  <input
                    type="text"
                    name="fullName"
                    className="form-input"
                    value={formData.fullName}
                    onChange={handleInputChange}
                    required
                    placeholder="Enter full name"
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Email Address</label>
                  <input
                    type="email"
                    name="email"
                    className="form-input"
                    value={formData.email}
                    onChange={handleInputChange}
                    required
                    placeholder="user@school.edu"
                  />
                  {!editingUser && (
                    <small className="form-hint">
                      Login credentials will be sent to this email
                    </small>
                  )}
                </div>

                <div className="form-group">
                  <label className="form-label">Role</label>
                  <select
                    name="role"
                    className="form-input"
                    value={formData.role}
                    onChange={handleInputChange}
                    required
                  >
                    <option value="TEACHER">Teacher</option>
                    <option value="ADMIN">Admin</option>
                  </select>
                </div>

                {!editingUser && (
                  <div className="form-group">
                    <label className="form-label">Temporary Password</label>
                    <div style={{ display: 'flex', gap: '10px' }}>
                      <input
                        type={showPassword ? 'text' : 'password'}
                        name="password"
                        className="form-input"
                        value={formData.password}
                        onChange={handleInputChange}
                        placeholder="Leave empty for auto-generated"
                      />
                      <button
                        type="button"
                        className="btn-icon"
                        onClick={() => setShowPassword(!showPassword)}
                        title={showPassword ? 'Hide' : 'Show'}
                      >
                        {showPassword ? '👁️' : '👁️‍🗨️'}
                      </button>
                    </div>
                    <small className="form-hint">
                      If left empty, a random password will be generated
                    </small>
                  </div>
                )}

                {editingUser && (
                  <div className="form-group">
                    <label className="form-label">New Password (optional)</label>
                    <input
                      type="password"
                      name="password"
                      className="form-input"
                      value={formData.password}
                      onChange={handleInputChange}
                      placeholder="Leave empty to keep current"
                    />
                  </div>
                )}
              </div>

              <div className="modal-footer">
                <button type="button" className="btn-outline" onClick={handleCloseModal}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary" disabled={loading}>
                  {loading ? 'Saving...' : (editingUser ? 'Update User' : 'Create User')}
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