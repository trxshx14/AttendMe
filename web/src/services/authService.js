import api from './api';

export const authService = {
  async login(email, password) {
    try {
      const username = email.split('@')[0];
      
      const response = await api.post('/auth/login', {
        username,
        password,
      });
      
      // IMPORTANT: Store tokens immediately after successful login
      if (response.data.success) {
        const { accessToken, refreshToken } = response.data.data;
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);
        
        // Also set the default header for future requests
        api.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
        
        console.log('✅ Tokens stored successfully');
      }
      
      return response.data;
    } catch (error) {
      console.error('❌ Login error:', error);
      throw error;
    }
  },

  async getCurrentUser() {
    try {
      // Token will be automatically added by interceptor
      const response = await api.get('/auth/me');
      return response.data;
    } catch (error) {
      console.error('❌ getCurrentUser error:', error);
      throw error;
    }
  },

  async logout() {
    try {
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        await api.post('/auth/logout', { refreshToken });
      }
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      localStorage.clear();
      delete api.defaults.headers.common['Authorization'];
    }
  },

  async googleLogin(idToken) {
    try {
      const response = await api.post('/auth/google', { idToken });
      
      // Store tokens for Google login too
      if (response.data.success) {
        const { accessToken, refreshToken } = response.data.data;
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);
        api.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
      }
      
      return response.data;
    } catch (error) {
      console.error('❌ Google login error:', error);
      throw error;
    }
  },

  isAuthenticated() {
    return !!localStorage.getItem('accessToken');
  },

  getUser() {
    const userStr = localStorage.getItem('user');
    try {
      return userStr ? JSON.parse(userStr) : null;
    } catch {
      return null;
    }
  },
};