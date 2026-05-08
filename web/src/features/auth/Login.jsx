import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useGoogleLogin } from '@react-oauth/google';
import { authService } from './authService';
import './Login.css';
import logo from '../../assets/logo.png';
import { useAuth } from '../../context/AuthContext';

const Login = () => {
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { login } = useAuth();

  /* ── Standard login ──────────────────────────────────── */
  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await authService.login(form.email, form.password);

      if (response.success) {
        const user = response.data;

        const userData = {
          id:     user.userId,
          name:   user.fullName || user.username,
          email:  user.email,
          role:   user.role.toLowerCase(),
          avatar: (user.fullName || user.username || 'U')
                    .split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase(),
          profilePicUrl: user.profilePicUrl || null,
        };

        localStorage.setItem('user', JSON.stringify(userData));
        login(userData);

        navigate(
          user.role.toLowerCase() === 'admin' ? '/admin/dashboard' : '/teacher/dashboard',
          { replace: true }
        );
      } else {
        setError(response.message || 'Invalid email or password');
        setLoading(false);
      }
    } catch (err) {
      console.error('❌ Login error:', err);
      setError(err.message || 'Unable to connect to server. Please try again.');
      setLoading(false);
    }
  };

  /* ── Google login ────────────────────────────────────── */
  const googleLogin = useGoogleLogin({
    onSuccess: async (tokenResponse) => {
      setError('');
      setLoading(true);
      try {
        console.log('1️⃣ Google access_token received, sending to backend...');

        // Send access_token to backend — backend fetches user info from Google
        const response = await authService.googleLogin(tokenResponse.access_token);
        console.log('2️⃣ Backend response:', response);

        if (response.success) {
          const user = response.data;
          console.log('3️⃣ Google user:', user);

          const userData = {
            id:     user.userId,
            name:   user.fullName || user.username,
            email:  user.email,
            role:   user.role.toLowerCase(),
            avatar: (user.fullName || user.username || 'G')
                      .split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase(),
            profilePicUrl: user.profilePicUrl || null,
          };

          localStorage.setItem('user', JSON.stringify(userData));
          login(userData);

          console.log('4️⃣ Navigating to dashboard...');
          navigate(
            user.role.toLowerCase() === 'admin' ? '/admin/dashboard' : '/teacher/dashboard',
            { replace: true }
          );
        } else {
          console.error('❌ Google login failed:', response);
          setError(response.message || 'Google login failed. Please try again.');
          setLoading(false);
        }
      } catch (err) {
        console.error('❌ Google login error:', err);
        setError(err.response?.data?.message || err.message || 'Google login failed');
        setLoading(false);
      }
    },
    onError: (err) => {
      console.error('❌ Google OAuth error:', err);
      setError('Google sign-in was cancelled or failed.');
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
          <span /><span /><span />
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
              style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', width: '100%' }}
            >
              {loading ? 'Signing in...' : 'Sign In →'}
            </button>
          </form>

          {/* Divider */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', margin: '20px 0', color: 'var(--muted)', fontSize: '0.8rem' }}>
            <hr style={{ flex: 1, border: 'none', borderTop: '1px solid var(--border)' }} />
            <span>OR</span>
            <hr style={{ flex: 1, border: 'none', borderTop: '1px solid var(--border)' }} />
          </div>

          {/* Google Button */}
          <button
            type="button"
            onClick={() => googleLogin()}
            disabled={loading}
            className="google-btn"
          >
            <svg width="20" height="20" viewBox="0 0 48 48">
              <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
              <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
              <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
              <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
              <path fill="none" d="M0 0h48v48H0z"/>
            </svg>
            Continue with Google
          </button>

          
        </div>
      </div>
    </div>
  );
};

export default Login;