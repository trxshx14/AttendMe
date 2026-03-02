import api from './api';

export const userService = {
  // Get all users
  async getAllUsers() {
    try {
      console.log('🔵 Fetching all users...');
      const response = await api.get('/users');
      console.log('✅ Users response:', response.data);
      return response.data;
    } catch (error) {
      console.error('❌ Error fetching users:', error.response?.data || error);
      throw error;
    }
  },

  // Get users by role
  async getUsersByRole(role) {
    try {
      const response = await api.get(`/users/role/${role}`);
      return response.data;
    } catch (error) {
      console.error(`❌ Error fetching ${role} users:`, error);
      throw error;
    }
  },

  // Get single user by ID
  async getUserById(id) {
    try {
      const response = await api.get(`/users/${id}`);
      return response.data;
    } catch (error) {
      console.error('❌ Error fetching user:', error);
      throw error;
    }
  },

  // Create new user (admin only)
  async createUser(userData) {
    try {
      console.log('🔵 Creating new user with data:', userData);
      
      // Generate username from email (remove domain and special characters)
      const username = userData.email.split('@')[0].replace(/[^a-zA-Z0-9]/g, '');
      
      // Prepare the payload for backend - MATCHING EXACT FIELDS
      const payload = {
        username: username,
        email: userData.email,
        fullName: userData.fullName,
        password: userData.password || this.generateTemporaryPassword(),
        role: userData.role.toUpperCase()
      };
      
      console.log('📦 Sending payload to /auth/register:', payload);
      
      const response = await api.post('/auth/register', payload);
      console.log('✅ Create user response:', response.data);
      
      // Add temporary password to response data for display
      if (response.data.success) {
        response.data.data = {
          ...response.data.data,
          tempPassword: payload.password
        };
      }
      
      return response.data;
    } catch (error) {
      // Log the detailed error response
      console.error('❌ Error creating user - Full error:', error);
      console.error('❌ Error response data:', error.response?.data);
      console.error('❌ Error status:', error.response?.status);
      console.error('❌ Error headers:', error.response?.headers);
      
      // Throw a more informative error
      const errorMessage = error.response?.data?.message || 
                          error.response?.data?.error || 
                          error.message || 
                          'Failed to create user';
      throw new Error(errorMessage);
    }
  },

  // Update user
  async updateUser(id, userData) {
    try {
      console.log('🔵 Updating user:', id, userData);
      
      const payload = {
        fullName: userData.fullName,
        email: userData.email,
        role: userData.role.toUpperCase()
      };
      
      // Only include password if it's provided
      if (userData.password) {
        payload.password = userData.password;
      }
      
      const response = await api.put(`/users/${id}`, payload);
      console.log('✅ Update user response:', response.data);
      return response.data;
    } catch (error) {
      console.error('❌ Error updating user:', error.response?.data || error);
      throw error;
    }
  },

  // Delete user
  async deleteUser(id) {
    try {
      console.log('🔵 Deleting user:', id);
      const response = await api.delete(`/users/${id}`);
      console.log('✅ Delete user response:', response.data);
      return response.data;
    } catch (error) {
      console.error('❌ Error deleting user:', error.response?.data || error);
      throw error;
    }
  },

  // Activate user
  async activateUser(id) {
    try {
      console.log('🔵 Activating user:', id);
      const response = await api.patch(`/users/${id}/activate`);
      console.log('✅ Activate user response:', response.data);
      return response.data;
    } catch (error) {
      console.error('❌ Error activating user:', error.response?.data || error);
      throw error;
    }
  },

  // Deactivate user
  async deactivateUser(id) {
    try {
      console.log('🔵 Deactivating user:', id);
      const response = await api.patch(`/users/${id}/deactivate`);
      console.log('✅ Deactivate user response:', response.data);
      return response.data;
    } catch (error) {
      console.error('❌ Error deactivating user:', error.response?.data || error);
      throw error;
    }
  },

  // Reset password
  async resetPassword(id) {
    try {
      console.log('🔵 Resetting password for user:', id);
      const tempPassword = this.generateTemporaryPassword();
      
      const response = await api.put(`/users/${id}`, {
        password: tempPassword
      });
      
      console.log('✅ Reset password response:', response.data);
      
      return {
        ...response.data,
        tempPassword
      };
    } catch (error) {
      console.error('❌ Error resetting password:', error.response?.data || error);
      throw error;
    }
  },

  // Helper function to generate temporary password
  generateTemporaryPassword() {
    const length = 10;
    const charset = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$';
    let password = '';
    for (let i = 0; i < length; i++) {
      password += charset.charAt(Math.floor(Math.random() * charset.length));
    }
    // Ensure it has at least one number and one special character
    if (!/[0-9]/.test(password)) {
      password = password.slice(0, -1) + Math.floor(Math.random() * 10);
    }
    if (!/[!@#$]/.test(password)) {
      password = password.slice(0, -1) + '!';
    }
    return password;
  }
};