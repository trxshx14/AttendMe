import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  GraduationCap, UserCircle2, BookOpen, BarChart2,
  Users, ShieldCheck, CalendarDays, Trophy,
  School, CheckCircle2, ChevronRight
} from 'lucide-react';
import './AdminDashboard.css';

const AdminDashboard = () => {
  const [stats, setStats] = useState({
    totalTeachers: 0,
    totalStudents: 0,
    totalClasses: 0,
    activeClasses: 0,
    teachersWithClasses: 0,
    avgClassSize: 0
  });

  const [topTeachers, setTopTeachers] = useState([]);
  const [classDistribution, setClassDistribution] = useState({ small: 0, medium: 0, large: 0 });
  const [loading, setLoading] = useState(true);
  const [userName, setUserName] = useState('');
  const [currentTime, setCurrentTime] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    setUserName(user.name || 'Administrator');
    updateTime();
    const timer = setInterval(updateTime, 60000);
    fetchDashboardData();
    return () => clearInterval(timer);
  }, []);

  const updateTime = () => {
    const now = new Date();
    setCurrentTime(now.toLocaleDateString('en-US', {
      weekday: 'long', year: 'numeric', month: 'long', day: 'numeric'
    }));
  };

  const fetchDashboardData = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem('accessToken');
      const [teachersRes, studentsRes, classesRes] = await Promise.all([
        fetch('http://localhost:8888/api/users/role/TEACHER', { headers: { 'Authorization': `Bearer ${token}` } }),
        fetch('http://localhost:8888/api/students', { headers: { 'Authorization': `Bearer ${token}` } }),
        fetch('http://localhost:8888/api/classes', { headers: { 'Authorization': `Bearer ${token}` } })
      ]);

      const teachersData = await teachersRes.json();
      const studentsData = await studentsRes.json();
      const classesData = await classesRes.json();

      const teachers = teachersData.data || [];
      const students = studentsData.data || [];
      const classes = classesData.data || [];

      const activeClasses = classes.filter(c => c.studentCount > 0).length;
      const avgClassSize = classes.length > 0 ? Math.round(students.length / classes.length) : 0;
      const teacherIdsWithClasses = [...new Set(classes.map(c => c.teacherId))];

      setStats({
        totalTeachers: teachers.length,
        totalStudents: students.length,
        totalClasses: classes.length,
        activeClasses,
        teachersWithClasses: teacherIdsWithClasses.length,
        avgClassSize
      });

      const teacherClassCount = {};
      classes.forEach(cls => {
        teacherClassCount[cls.teacherId] = (teacherClassCount[cls.teacherId] || 0) + 1;
      });

      setTopTeachers(
        Object.entries(teacherClassCount)
          .map(([teacherId, count]) => {
            const teacher = teachers.find(t => t.userId === parseInt(teacherId));
            return { teacherId, teacherName: teacher?.fullName || 'Unknown', classCount: count };
          })
          .sort((a, b) => b.classCount - a.classCount)
          .slice(0, 5)
      );

      setClassDistribution({
        small: classes.filter(c => c.studentCount < 15).length,
        medium: classes.filter(c => c.studentCount >= 15 && c.studentCount < 30).length,
        large: classes.filter(c => c.studentCount >= 30).length
      });

    } catch (err) {
      console.error('Dashboard fetch error:', err);
    } finally {
      setLoading(false);
    }
  };

  const getGreeting = () => {
    const h = new Date().getHours();
    if (h < 12) return 'Good Morning';
    if (h < 18) return 'Good Afternoon';
    return 'Good Evening';
  };

  const getInitials = (name) =>
    name ? name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2) : '??';

  const totalDist = stats.totalClasses || 1;

  if (loading) {
    return (
      <div className="ad-loading">
        <div className="ad-spinner" />
        <p>Loading dashboard…</p>
      </div>
    );
  }

  return (
    <div className="admin-dashboard">

      {/* ── Welcome Banner ─────────────────────────────── */}
      <div className="ad-welcome">
        <div className="ad-welcome-orb ad-orb1" />
        <div className="ad-welcome-orb ad-orb2" />
        <div className="ad-welcome-orb ad-orb3" />
        <div className="ad-welcome-left">
          <p className="ad-greeting-label">{getGreeting()}</p>
          <h1 className="ad-greeting-name">{userName}</h1>
          <p className="ad-greeting-sub">Here's what's happening with your school today.</p>
        </div>
        <div className="ad-welcome-right">
          <div className="ad-date-pill">
            <CalendarDays size={16} />
            <span>{currentTime}</span>
          </div>
          <div className="ad-mini-stats">
            <div className="ad-mini-stat">
              <span className="ad-mini-val">{stats.totalStudents}</span>
              <span className="ad-mini-lbl">Students</span>
            </div>
            <div className="ad-mini-divider" />
            <div className="ad-mini-stat">
              <span className="ad-mini-val">{stats.totalClasses}</span>
              <span className="ad-mini-lbl">Classes</span>
            </div>
            <div className="ad-mini-divider" />
            <div className="ad-mini-stat">
              <span className="ad-mini-val">{stats.totalTeachers}</span>
              <span className="ad-mini-lbl">Teachers</span>
            </div>
          </div>
        </div>
      </div>

      {/* ── Stats Grid ─────────────────────────────────── */}
      <div className="ad-stats-grid">
        <div className="ad-stat-card">
          <div className="ad-stat-icon-wrap">
            <GraduationCap size={28} color="#FFFFFF" strokeWidth={2} />
          </div>
          <div className="ad-stat-body">
            <span className="ad-stat-label">Total Students</span>
            <span className="ad-stat-value">{stats.totalStudents}</span>
            <span className="ad-stat-sub ad-positive">↑ 12% this month</span>
          </div>
        </div>

        <div className="ad-stat-card">
          <div className="ad-stat-icon-wrap">
            <UserCircle2 size={28} color="#FFFFFF" strokeWidth={2} />
          </div>
          <div className="ad-stat-body">
            <span className="ad-stat-label">Total Teachers</span>
            <span className="ad-stat-value">{stats.totalTeachers}</span>
            <span className="ad-stat-sub">{stats.teachersWithClasses} with classes</span>
          </div>
        </div>

        <div className="ad-stat-card">
          <div className="ad-stat-icon-wrap">
            <BookOpen size={28} color="#FFFFFF" strokeWidth={2} />
          </div>
          <div className="ad-stat-body">
            <span className="ad-stat-label">Total Classes</span>
            <span className="ad-stat-value">{stats.totalClasses}</span>
            <span className="ad-stat-sub">{stats.activeClasses} active</span>
          </div>
        </div>

        <div className="ad-stat-card">
          <div className="ad-stat-icon-wrap">
            <BarChart2 size={28} color="#FFFFFF" strokeWidth={2} />
          </div>
          <div className="ad-stat-body">
            <span className="ad-stat-label">Avg Class Size</span>
            <span className="ad-stat-value">{stats.avgClassSize}</span>
            <span className="ad-stat-sub">students per class</span>
          </div>
        </div>
      </div>

      {/* ── Analytics Row ──────────────────────────────── */}
      <div className="ad-analytics-row">

        {/* Class Distribution */}
        <div className="ad-card">
          <div className="ad-card-header">
            <div>
              <h3 className="ad-card-title">Class Size Distribution</h3>
              <p className="ad-card-sub">Breakdown by enrollment size</p>
            </div>
            <span className="ad-badge">Overview</span>
          </div>

          <div className="ad-dist-list">
            {[
              { label: 'Small Classes', key: 'small', desc: '< 15 students' },
              { label: 'Medium Classes', key: 'medium', desc: '15–30 students' },
              { label: 'Large Classes', key: 'large', desc: '30+ students' },
            ].map(({ label, key, desc }) => (
              <div key={key} className="ad-dist-item">
                <div className="ad-dist-top">
                  <span className="ad-dist-label">{label}</span>
                  <div className="ad-dist-right">
                    <span className="ad-dist-desc">{desc}</span>
                    <span className="ad-dist-count">{classDistribution[key]}</span>
                  </div>
                </div>
                <div className="ad-progress-track">
                  <div
                    className="ad-progress-fill"
                    style={{ width: `${(classDistribution[key] / totalDist) * 100}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Top Teachers */}
        <div className="ad-card">
          <div className="ad-card-header">
            <div>
              <h3 className="ad-card-title ad-title-with-icon">
                <Trophy size={16} color="#0F2D5E" /> Top Teachers
              </h3>
              <p className="ad-card-sub">Ranked by class assignments</p>
            </div>
            <button className="ad-view-all" onClick={() => navigate('/admin/users')}>
              View All <ChevronRight size={14} />
            </button>
          </div>

          <div className="ad-teachers-list">
            {topTeachers.length === 0 ? (
              <div className="ad-empty">No teachers assigned yet</div>
            ) : topTeachers.map((t, i) => (
              <div key={t.teacherId} className="ad-teacher-row">
                <div className={`ad-rank ad-rank-${i + 1}`}>#{i + 1}</div>
                <div className="ad-teacher-avatar">{getInitials(t.teacherName)}</div>
                <div className="ad-teacher-info">
                  <span className="ad-teacher-name">{t.teacherName}</span>
                  <span className="ad-teacher-meta">{t.classCount} class{t.classCount !== 1 ? 'es' : ''} assigned</span>
                </div>
                <div className="ad-teacher-chip">{t.classCount}</div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* ── School Summary ─────────────────────────────── */}
      <div className="ad-card ad-summary-card">
        <div className="ad-card-header">
          <div>
            <h3 className="ad-card-title ad-title-with-icon">
              <ShieldCheck size={16} color="#0F2D5E" /> School Summary
            </h3>
            <p className="ad-card-sub">Key metrics at a glance</p>
          </div>
        </div>
        <div className="ad-summary-grid">
          {[
            { icon: <School size={22} color="#0F2D5E" />, val: stats.totalClasses, lbl: 'Total Classes' },
            { icon: <CheckCircle2 size={22} color="#0F2D5E" />, val: stats.activeClasses, lbl: 'Active Classes' },
            { icon: <UserCircle2 size={22} color="#0F2D5E" />, val: stats.teachersWithClasses, lbl: 'Teachers w/ Classes' },
            { icon: <BarChart2 size={22} color="#0F2D5E" />, val: stats.avgClassSize, lbl: 'Avg Class Size' },
            { icon: <GraduationCap size={22} color="#0F2D5E" />, val: stats.totalStudents, lbl: 'Total Students' },
            { icon: <Users size={22} color="#0F2D5E" />, val: stats.totalTeachers, lbl: 'Total Teachers' },
          ].map(({ icon, val, lbl }) => (
            <div key={lbl} className="ad-summary-item">
              <span className="ad-summary-icon">{icon}</span>
              <div>
                <span className="ad-summary-val">{val}</span>
                <span className="ad-summary-lbl">{lbl}</span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* ── Quick Actions ──────────────────────────────── */}
      <div className="ad-quick-section">
        <h3 className="ad-quick-title">Quick Actions</h3>
        <div className="ad-quick-grid">
          {[
            { icon: <Users size={26} color="#FFFFFF" />, label: 'Manage Users', desc: 'Add or edit accounts', path: '/admin/users' },
            { icon: <BookOpen size={26} color="#FFFFFF" />, label: 'Manage Classes', desc: 'Create or modify classes', path: '/admin/classes' },
            { icon: <BarChart2 size={26} color="#FFFFFF" />, label: 'View Reports', desc: 'Analytics and insights', path: '/admin/reports' },
          ].map(({ icon, label, desc, path }) => (
            <button key={path} className="ad-quick-card" onClick={() => navigate(path)}>
              <div className="ad-quick-icon">{icon}</div>
              <div className="ad-quick-info">
                <span className="ad-quick-label">{label}</span>
                <span className="ad-quick-desc">{desc}</span>
              </div>
              <ChevronRight size={18} className="ad-quick-arrow" />
            </button>
          ))}
        </div>
      </div>

    </div>
  );
};

export default AdminDashboard;