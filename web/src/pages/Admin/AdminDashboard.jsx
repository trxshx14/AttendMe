import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './AdminDashboard.css';

const AdminDashboard = () => {
  const [stats, setStats] = useState({
    totalTeachers: 0,
    totalStudents: 0,
    totalClasses: 0,
    totalPresent: 0,
    totalAbsent: 0,
    totalLate: 0,
    totalExcused: 0
  });
  
  const [recentActivity, setRecentActivity] = useState([]);
  const [classSummaries, setClassSummaries] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem('accessToken');
      
      // Fetch all dashboard data
      const [teachersRes, studentsRes, classesRes, attendanceRes] = await Promise.all([
        fetch('http://localhost:8888/api/users/role/TEACHER', {
          headers: { 'Authorization': `Bearer ${token}` }
        }),
        fetch('http://localhost:8888/api/students', {
          headers: { 'Authorization': `Bearer ${token}` }
        }),
        fetch('http://localhost:8888/api/classes', {
          headers: { 'Authorization': `Bearer ${token}` }
        }),
        fetch(`http://localhost:8888/api/attendance/today/summary`, {
          headers: { 'Authorization': `Bearer ${token}` }
        })
      ]);

      const teachersData = await teachersRes.json();
      const studentsData = await studentsRes.json();
      const classesData = await classesRes.json();
      const attendanceData = await attendanceRes.json();

      // Get today's date
      const today = new Date().toISOString().split('T')[0];
      
      // Fetch today's attendance for each class
      const classPromises = classesData.data?.map(async (cls) => {
        const response = await fetch(`http://localhost:8888/api/attendance/class/${cls.classId}/date/${today}`, {
          headers: { 'Authorization': `Bearer ${token}` }
        });
        const data = await response.json();
        return {
          ...cls,
          attendance: data.data || []
        };
      }) || [];

      const classData = await Promise.all(classPromises);
      
      // Calculate stats
      const present = attendanceData.data?.present || 0;
      const absent = attendanceData.data?.absent || 0;
      const late = attendanceData.data?.late || 0;
      const excused = attendanceData.data?.excused || 0;

      setStats({
        totalTeachers: teachersData.data?.length || 0,
        totalStudents: studentsData.data?.length || 0,
        totalClasses: classesData.data?.length || 0,
        totalPresent: present,
        totalAbsent: absent,
        totalLate: late,
        totalExcused: excused
      });

      // Process class summaries
      const summaries = classData.map(cls => {
        const total = cls.attendance.length;
        const present = cls.attendance.filter(a => a.status === 'present').length;
        const rate = total > 0 ? Math.round((present / total) * 100) : 0;
        
        return {
          id: cls.classId,
          name: cls.className,
          subject: cls.subject,
          section: cls.section,
          teacherName: cls.teacherName,
          totalStudents: cls.students?.length || 0,
          markedCount: total,
          attendanceRate: rate,
          presentCount: present
        };
      });

      setClassSummaries(summaries);

      // Fetch recent activity
      const activityRes = await fetch('http://localhost:8888/api/attendance/recent?limit=8', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const activityData = await activityRes.json();
      setRecentActivity(activityData.data || []);

    } catch (error) {
      console.error('Error fetching dashboard data:', error);
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-PH', {
      weekday: 'short',
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  const getStatusColor = (status) => {
    const colors = {
      present: '#22c55e',
      absent: '#ef4444',
      late: '#f59e0b',
      excused: '#6366f1'
    };
    return colors[status] || '#64748b';
  };

  const getStatusBg = (status) => {
    const colors = {
      present: '#dcfce7',
      absent: '#fee2e2',
      late: '#fef3c7',
      excused: '#ede9fe'
    };
    return colors[status] || '#f1f5f9';
  };

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <p>Loading dashboard...</p>
      </div>
    );
  }

  const totalMarked = stats.totalPresent + stats.totalAbsent + stats.totalLate + stats.totalExcused;
  const unmarked = stats.totalStudents - totalMarked;

  return (
    <div className="admin-dashboard">
      {/* Alert for unmarked attendance */}
      {unmarked > 0 && (
        <div className="alert-strip">
          <span className="alert-icon">⚠️</span>
          <div>
            <strong>{unmarked} student{unmarked !== 1 ? 's' : ''}</strong> still have unmarked attendance today.
            {/* Removed the "View details" button that navigated to attendance */}
          </div>
        </div>
      )}

      {/* Stats Grid */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon" style={{ background: '#e0f2fe', color: '#0284c7' }}>👥</div>
          <div className="stat-info">
            <div className="stat-value">{stats.totalStudents}</div>
            <div className="stat-label">Total Students</div>
            <div className="stat-sub">{stats.totalClasses} classes</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: '#dcfce7', color: '#16a34a' }}>✅</div>
          <div className="stat-info">
            <div className="stat-value">{stats.totalPresent}</div>
            <div className="stat-label">Present Today</div>
            <div className="stat-sub">{Math.round((stats.totalPresent / (totalMarked || 1)) * 100)}% of marked</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: '#fee2e2', color: '#dc2626' }}>❌</div>
          <div className="stat-info">
            <div className="stat-value">{stats.totalAbsent}</div>
            <div className="stat-label">Absent Today</div>
            <div className="stat-sub">{Math.round((stats.totalAbsent / (totalMarked || 1)) * 100)}% of marked</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: '#fef3c7', color: '#d97706' }}>⏰</div>
          <div className="stat-info">
            <div className="stat-value">{stats.totalLate + stats.totalExcused}</div>
            <div className="stat-label">Late / Excused</div>
            <div className="stat-sub">{stats.totalLate} late, {stats.totalExcused} excused</div>
          </div>
        </div>
      </div>

      {/* Teachers and Classes Summary */}
      <div className="stats-grid" style={{ gridTemplateColumns: 'repeat(2, 1fr)' }}>
        <div className="stat-card">
          <div className="stat-icon" style={{ background: '#fae8ff', color: '#a855f7' }}>👨‍🏫</div>
          <div className="stat-info">
            <div className="stat-value">{stats.totalTeachers}</div>
            <div className="stat-label">Total Teachers</div>
            <div className="stat-sub">{stats.totalClasses} active classes</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: '#fff7ed', color: '#ea580c' }}>📊</div>
          <div className="stat-info">
            <div className="stat-value">{Math.round((totalMarked / (stats.totalStudents || 1)) * 100)}%</div>
            <div className="stat-label">Overall Attendance</div>
            <div className="stat-sub">{totalMarked} marked today</div>
          </div>
        </div>
      </div>

      <div className="two-col">
        {/* Today's Class Summary */}
        <div className="card">
          <div className="card-header">
            <h3>Today's Class Summary</h3>
            <span className="date-badge">📅 {formatDate(new Date())}</span>
          </div>
          <div className="card-body" style={{ padding: 0 }}>
            <div className="class-summary-list">
              {classSummaries.length === 0 ? (
                <div className="empty-state">
                  <p>No classes found.</p>
                </div>
              ) : (
                classSummaries.map(cls => (
                  <div key={cls.id} className="class-summary-item">
                    <div className="class-info">
                      <div className="class-name">{cls.name}</div>
                      <div className="class-details">
                        {cls.subject} · {cls.teacherName} · {cls.totalStudents} students
                      </div>
                    </div>
                    <div className="class-stats">
                      <div className="progress-section">
                        <div className="progress-label">
                          <span>{cls.markedCount}/{cls.totalStudents} marked</span>
                          <span style={{ color: cls.attendanceRate > 80 ? '#22c55e' : cls.attendanceRate > 50 ? '#f59e0b' : '#ef4444' }}>
                            {cls.attendanceRate}%
                          </span>
                        </div>
                        <div className="progress-wrap">
                          <div 
                            className="progress-bar" 
                            style={{ 
                              width: `${cls.attendanceRate}%`,
                              background: cls.attendanceRate > 80 ? '#22c55e' : cls.attendanceRate > 50 ? '#f59e0b' : '#ef4444'
                            }}
                          />
                        </div>
                      </div>
                      <div className="mini-ring">
                        <svg viewBox="0 0 52 52" width="52" height="52">
                          <circle cx="26" cy="26" r="22" fill="none" stroke="#e2e8f0" strokeWidth="5" />
                          <circle 
                            cx="26" cy="26" r="22" fill="none" 
                            stroke={cls.attendanceRate > 80 ? '#22c55e' : cls.attendanceRate > 50 ? '#f59e0b' : '#ef4444'} 
                            strokeWidth="5"
                            strokeDasharray={`${(cls.attendanceRate / 100) * 138.2} 138.2`}
                            strokeLinecap="round"
                            transform="rotate(-90 26 26)"
                          />
                          <text x="26" y="30" textAnchor="middle" fontSize="9" fill="#0f172a" fontWeight="bold">
                            {cls.attendanceRate}%
                          </text>
                        </svg>
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>

        {/* Recent Activity */}
        <div className="card">
          <div className="card-header">
            <h3>Recent Attendance Records</h3>
            <button 
              className="btn-outline btn-sm"
              onClick={() => navigate('/admin/reports')}
            >
              View All
            </button>
          </div>
          <div className="card-body" style={{ padding: 0 }}>
            <div className="recent-list">
              {recentActivity.length === 0 ? (
                <div className="empty-state">
                  <p>No recent records.</p>
                </div>
              ) : (
                recentActivity.map(record => (
                  <div key={record.id} className="recent-item">
                    <div className="recent-info">
                      <div className="recent-name">{record.studentName}</div>
                      <div className="recent-meta">
                        {record.className} · {formatDate(record.date)}
                      </div>
                    </div>
                    <span 
                      className="status-badge"
                      style={{ 
                        background: getStatusBg(record.status),
                        color: getStatusColor(record.status)
                      }}
                    >
                      <span className="status-dot" style={{ background: getStatusColor(record.status) }} />
                      {record.status}
                    </span>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="quick-actions">
        <button 
          className="btn-primary"
          onClick={() => navigate('/admin/users')}
        >
          👥 Manage Users
        </button>
        <button 
          className="btn-outline"
          onClick={() => navigate('/admin/reports')}
        >
          📊 View Reports
        </button>
        <button 
          className="btn-outline"
          onClick={() => navigate('/admin/classes')}
        >
          📚 Manage Classes
        </button>
      </div>
    </div>
  );
};

export default AdminDashboard;