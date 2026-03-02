import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useGoogleLogin } from '@react-oauth/google';
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
      const username = form.email.split('@')[0];
      
      const response = await fetch('http://localhost:8888/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username: username,
          password: form.password,
        }),
      });

      const data = await response.json();
      
      if (data.success) {
        localStorage.setItem('accessToken', data.data.accessToken);
        localStorage.setItem('refreshToken', data.data.refreshToken);
        
        const userResponse = await fetch('http://localhost:8888/api/auth/me', {
          headers: {
            'Authorization': `Bearer ${data.data.accessToken}`
          }
        });
        
        const userData = await userResponse.json();
        
        if (userData.success) {
          const user = userData.data;
          
          localStorage.setItem('user', JSON.stringify({
            id: user.userId,
            name: user.fullName,
            email: user.email,
            role: user.role.toLowerCase(),
            avatar: user.fullName.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase()
          }));
          
          navigate('/dashboard');
        }
      } else {
        setError(data.message || 'Invalid email or password');
      }
    } catch (err) {
      setError('Unable to connect to server. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const googleLogin = useGoogleLogin({
    onSuccess: async (tokenResponse) => {
      setLoading(true);
      try {
        const response = await fetch('http://localhost:8888/api/auth/google', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ idToken: tokenResponse.access_token }),
        });

        const data = await response.json();
        
        if (data.success) {
          localStorage.setItem('accessToken', data.data.accessToken);
          localStorage.setItem('refreshToken', data.data.refreshToken);
          localStorage.setItem('user', JSON.stringify({
            id: data.data.userId,
            name: data.data.name,
            email: data.data.email,
            role: data.data.role.toLowerCase(),
            avatar: data.data.name.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase()
          }));
          navigate('/dashboard');
        } else {
          setError('Google login failed');
        }
      } catch (err) {
        setError('Google login error: ' + err.message);
      } finally {
        setLoading(false);
      }
    },
    onError: () => {
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
                placeholder="teacher@school.edu"
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

            <button type="submit" className="btn-primary" disabled={loading}>
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
            Contact your school administrator if you need access
          </p>
        </div>
      </div>
    </div>
  );
};

export default Login;