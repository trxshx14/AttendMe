import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useGoogleLogin } from '@react-oauth/google';
import { authService } from '../../services/authService';
import './Login.css';
import logo from '../../assets/logo.png';

const Login = () => {
  const [form, setForm] = useState({
    email: '',
    password: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      console.log('1️⃣ Attempting login...');
      const response = await authService.login(form.email, form.password);
      console.log('2️⃣ Login response:', response);
      
      if (response.success) {
        console.log('3️⃣ Getting current user...');
        const userResponse = await authService.getCurrentUser();
        console.log('4️⃣ User response:', userResponse);
        
        if (userResponse.success) {
          const user = userResponse.data;
          console.log('5️⃣ User data:', user);
          
          const userData = {
            id: user.userId,
            name: user.fullName,
            email: user.email,
            role: user.role.toLowerCase(),
            avatar: user.fullName.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase()
          };
          
          localStorage.setItem('user', JSON.stringify(userData));
          console.log('6️⃣ User stored:', userData);
          
          // Small delay to ensure everything is saved
          setTimeout(() => {
            // Determine redirect path based on role
            const redirectPath = user.role.toLowerCase() === 'admin' 
              ? '/admin/dashboard' 
              : '/teacher/dashboard';
            
            console.log('7️⃣ Redirecting to:', redirectPath);
            
            // Try React Router navigation first
            navigate(redirectPath, { replace: true });
            
            // Fallback: if after 500ms we're still on login page, use window.location
            setTimeout(() => {
              if (window.location.pathname === '/login') {
                console.log('⚠️ React Router navigation failed, using window.location');
                window.location.href = redirectPath;
              }
            }, 500);
          }, 100);
        } else {
          console.error('❌ Failed to get user:', userResponse);
          setError('Failed to get user details');
          setLoading(false);
        }
      } else {
        console.error('❌ Login failed:', response);
        setError(response.message || 'Invalid email or password');
        setLoading(false);
      }
    } catch (err) {
      console.error('❌ Login error:', err);
      setError(err.message || 'Unable to connect to server. Please try again.');
      setLoading(false);
    }
  };

  const googleLogin = useGoogleLogin({
    onSuccess: async (tokenResponse) => {
      setLoading(true);
      try {
        console.log('1️⃣ Google login attempt...');
        const response = await authService.googleLogin(tokenResponse.access_token);
        console.log('2️⃣ Google login response:', response);
        
        if (response.success) {
          const user = response.data;
          console.log('3️⃣ Google user data:', user);
          
          localStorage.setItem('accessToken', user.accessToken);
          localStorage.setItem('refreshToken', user.refreshToken);
          
          const userData = {
            id: user.userId,
            name: user.name,
            email: user.email,
            role: user.role.toLowerCase(),
            avatar: user.name.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase()
          };
          
          localStorage.setItem('user', JSON.stringify(userData));
          console.log('4️⃣ User stored:', userData);
          
          // Small delay to ensure everything is saved
          setTimeout(() => {
            // Determine redirect path based on role
            const redirectPath = user.role.toLowerCase() === 'admin' 
              ? '/admin/dashboard' 
              : '/teacher/dashboard';
            
            console.log('5️⃣ Redirecting to:', redirectPath);
            
            // Try React Router navigation first
            navigate(redirectPath, { replace: true });
            
            // Fallback: if after 500ms we're still on login page, use window.location
            setTimeout(() => {
              if (window.location.pathname === '/login') {
                console.log('⚠️ React Router navigation failed, using window.location');
                window.location.href = redirectPath;
              }
            }, 500);
          }, 100);
        } else {
          console.error('❌ Google login failed:', response);
          setError('Google login failed');
          setLoading(false);
        }
      } catch (err) {
        console.error('❌ Google login error:', err);
        setError(err.message || 'Google login error');
        setLoading(false);
      }
    },
    onError: (error) => {
      console.error('❌ Google login error:', error);
      setError('Google login failed');
    },
  });

  return (
    <div className="login-shell">
      <div className="login-hero">
        <div className="hero-circle">
          <span className="hero-icon">📚</span>
        </div>
        <div className="hero-text">
          <h1>AttendMe</h1>
          <p>Smart attendance management for modern schools.</p>
        </div>
        <div className="hero-dots">
          <span />
          <span />
          <span />
        </div>
      </div>

      <div className="login-form-side">
        <div className="login-logo">
          <img src={logo} alt="AttendMe" style={{ height: '40px', width: 'auto' }} />
          <div className="login-logo-text">AttendMe</div>
        </div>

        <div className="login-card">
          <h2>Welcome back</h2>
          <p>Sign in to your account to continue.</p>

          {error && <div className="err-msg">{error}</div>}

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="form-label">Email Address</label>
              <input
                type="email"
                className="form-input"
                placeholder="Enter your email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Password</label>
              <input
                type="password"
                className="form-input"
                placeholder="••••••••"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                required
              />
            </div>

            <button 
              type="submit" 
              className="btn-primary" 
              disabled={loading}
              style={{ 
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                width: '100%'
              }}
            >
              {loading ? 'Signing in...' : 'Sign In →'}
            </button>
          </form>

          {/* Google Login Button */}
          <div style={{ marginTop: '20px', textAlign: 'center' }}>
            <div style={{ 
              display: 'flex', 
              alignItems: 'center', 
              gap: '10px',
              margin: '15px 0',
              color: 'var(--muted)',
              fontSize: '0.8rem'
            }}>
              <hr style={{ flex: 1, border: 'none', borderTop: '1px solid var(--border)' }} />
              <span>OR</span>
              <hr style={{ flex: 1, border: 'none', borderTop: '1px solid var(--border)' }} />
            </div>

            <button
              type="button"
              onClick={() => googleLogin()}
              disabled={loading}
              className="google-btn"
            >
              <img 
                src="https://www.google.com/favicon.ico" 
                alt="Google" 
                style={{ width: '20px', height: '20px' }}
              />
              Continue with Google
            </button>
          </div>

          <p className="login-hint">
            Use your school credentials to login
          </p>
        </div>
      </div>
    </div>
  );
};

export default Login;