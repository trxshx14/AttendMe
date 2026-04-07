import api from './api';

export const authService = {
  async login(email, password) {
    try {
      const response = await api.post('/auth/login', { email, password });

      if (response.data.success) {
        const { accessToken, refreshToken } = response.data.data;
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);
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
      const response = await api.get('/auth/me');
      return response.data;
    } catch (error) {
      console.error('❌ getCurrentUser error:', error);
      throw error;
    }
  },

  // ✅ Updated logout — calls backend to mark user offline before clearing session
  async logout() {
    try {
      const token = localStorage.getItem('accessToken');
      if (token) {
        await api.post('/auth/logout', {}, {
          headers: { Authorization: `Bearer ${token}` }
        });
        console.log('✅ User marked offline on server');
      }
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      localStorage.clear();
      delete api.defaults.headers.common['Authorization'];
    }
  },

  /**
   * Google login — sends access_token from useGoogleLogin implicit flow.
   * Backend field name: accessToken (matches GoogleAuthRequest.accessToken)
   */
  async googleLogin(googleAccessToken) {
    try {
      console.log('🔵 Sending Google access_token to backend...');
      const response = await api.post('/auth/google', {
        accessToken: googleAccessToken,
      });

      if (response.data.success) {
        const { accessToken, refreshToken } = response.data.data;
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);
        api.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
        console.log('✅ Google tokens stored');
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