import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
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
  
  const [recentActivity, setRecentActivity] = useState([]);
  const [topTeachers, setTopTeachers] = useState([]);
  const [classDistribution, setClassDistribution] = useState([]);
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
      const [teachersRes, studentsRes, classesRes] = await Promise.all([
        fetch('http://localhost:8888/api/users/role/TEACHER', {
          headers: { 'Authorization': `Bearer ${token}` }
        }),
        fetch('http://localhost:8888/api/students', {
          headers: { 'Authorization': `Bearer ${token}` }
        }),
        fetch('http://localhost:8888/api/classes', {
          headers: { 'Authorization': `Bearer ${token}` }
        })
      ]);

      const teachersData = await teachersRes.json();
      const studentsData = await studentsRes.json();
      const classesData = await classesRes.json();

      const teachers = teachersData.data || [];
      const students = studentsData.data || [];
      const classes = classesData.data || [];

      // Calculate statistics
      const activeClasses = classes.filter(c => c.studentCount > 0).length;
      const avgClassSize = classes.length > 0 
        ? Math.round(students.length / classes.length) 
        : 0;

      // Find teachers with classes
      const teacherIdsWithClasses = [...new Set(classes.map(c => c.teacherId))];
      
      setStats({
        totalTeachers: teachers.length,
        totalStudents: students.length,
        totalClasses: classes.length,
        activeClasses: activeClasses,
        teachersWithClasses: teacherIdsWithClasses.length,
        avgClassSize: avgClassSize
      });

      // Get top teachers by class count
      const teacherClassCount = {};
      classes.forEach(cls => {
        teacherClassCount[cls.teacherId] = (teacherClassCount[cls.teacherId] || 0) + 1;
      });

      const topTeacherList = Object.entries(teacherClassCount)
        .map(([teacherId, count]) => {
          const teacher = teachers.find(t => t.userId === parseInt(teacherId));
          return {
            teacherId,
            teacherName: teacher?.fullName || 'Unknown Teacher',
            classCount: count
          };
        })
        .sort((a, b) => b.classCount - a.classCount)
        .slice(0, 5);

      setTopTeachers(topTeacherList);

      // Class distribution by size
      const distribution = {
        small: classes.filter(c => c.studentCount < 15).length,
        medium: classes.filter(c => c.studentCount >= 15 && c.studentCount < 30).length,
        large: classes.filter(c => c.studentCount >= 30).length
      };
      setClassDistribution(distribution);

      // Fetch recent teacher/class activity
      const activityRes = await fetch('http://localhost:8888/api/classes/recent?limit=5', {
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

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <p>Loading dashboard...</p>
      </div>
    );
  }

  return (
    <div className="admin-dashboard">
      {/* Stats Grid */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon" style={{ background: '#e0f2fe', color: '#0284c7' }}>👥</div>
          <div className="stat-info">
            <div className="stat-value">{stats.totalStudents}</div>
            <div className="stat-label">Total Students</div>
            <div className="stat-sub">Across {stats.totalClasses} classes</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: '#fae8ff', color: '#a855f7' }}>👨‍🏫</div>
          <div className="stat-info">
            <div className="stat-value">{stats.totalTeachers}</div>
            <div className="stat-label">Total Teachers</div>
            <div className="stat-sub">{stats.teachersWithClasses} have classes</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: '#dcfce7', color: '#16a34a' }}>📚</div>
          <div className="stat-info">
            <div className="stat-value">{stats.totalClasses}</div>
            <div className="stat-label">Total Classes</div>
            <div className="stat-sub">{stats.activeClasses} active</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: '#fff7ed', color: '#ea580c' }}>📊</div>
          <div className="stat-info">
            <div className="stat-value">{stats.avgClassSize}</div>
            <div className="stat-label">Avg Class Size</div>
            <div className="stat-sub">Students per class</div>
          </div>
        </div>
      </div>

      {/* Class Distribution and Top Teachers */}
      <div className="two-col">
        <div className="card">
          <div className="card-header">
            <h3>Class Size Distribution</h3>
            <span className="date-badge">📊 Overview</span>
          </div>
          <div className="card-body">
            <div className="distribution-grid">
              <div className="distribution-item">
                <div className="distribution-label">
                  <span className="distribution-badge small">Small</span>
                  <span className="distribution-count">{classDistribution.small}</span>
                </div>
                <div className="progress-wrap">
                  <div 
                    className="progress-bar small" 
                    style={{ 
                      width: `${stats.totalClasses ? (classDistribution.small / stats.totalClasses) * 100 : 0}%`,
                      background: '#3b82f6'
                    }}
                  />
                </div>
                <span className="distribution-desc">Less than 15 students</span>
              </div>

              <div className="distribution-item">
                <div className="distribution-label">
                  <span className="distribution-badge medium">Medium</span>
                  <span className="distribution-count">{classDistribution.medium}</span>
                </div>
                <div className="progress-wrap">
                  <div 
                    className="progress-bar medium" 
                    style={{ 
                      width: `${stats.totalClasses ? (classDistribution.medium / stats.totalClasses) * 100 : 0}%`,
                      background: '#f59e0b'
                    }}
                  />
                </div>
                <span className="distribution-desc">15-30 students</span>
              </div>

              <div className="distribution-item">
                <div className="distribution-label">
                  <span className="distribution-badge large">Large</span>
                  <span className="distribution-count">{classDistribution.large}</span>
                </div>
                <div className="progress-wrap">
                  <div 
                    className="progress-bar large" 
                    style={{ 
                      width: `${stats.totalClasses ? (classDistribution.large / stats.totalClasses) * 100 : 0}%`,
                      background: '#ef4444'
                    }}
                  />
                </div>
                <span className="distribution-desc">30+ students</span>
              </div>
            </div>
          </div>
        </div>

        {/* Top Teachers */}
        <div className="card">
          <div className="card-header">
            <h3>Top Teachers by Classes</h3>
            <button 
              className="btn-outline btn-sm"
              onClick={() => navigate('/admin/users')}
            >
              View All
            </button>
          </div>
          <div className="card-body" style={{ padding: 0 }}>
            <div className="top-teachers-list">
              {topTeachers.length === 0 ? (
                <div className="empty-state">
                  <p>No teachers found.</p>
                </div>
              ) : (
                topTeachers.map((teacher, index) => (
                  <div key={teacher.teacherId} className="top-teacher-item">
                    <div className="teacher-rank">#{index + 1}</div>
                    <div className="teacher-info">
                      <div className="teacher-name">{teacher.teacherName}</div>
                      <div className="teacher-classes">{teacher.classCount} classes</div>
                    </div>
                    <div className="teacher-badge">
                      <span className="class-count-badge">{teacher.classCount}</span>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Recent Activity */}
      <div className="card" style={{ marginTop: '24px' }}>
        <div className="card-header">
          <h3>Recent Class Updates</h3>
          <button 
            className="btn-outline btn-sm"
            onClick={() => navigate('/admin/classes')}
          >
            Manage Classes
          </button>
        </div>
        <div className="card-body" style={{ padding: 0 }}>
          <div className="recent-activity-list">
            {recentActivity.length === 0 ? (
              <div className="empty-state">
                <p>No recent activity.</p>
              </div>
            ) : (
              recentActivity.map((activity, index) => (
                <div key={index} className="activity-item">
                  <div className="activity-icon">
                    {activity.type === 'class' ? '📚' : '👥'}
                  </div>
                  <div className="activity-details">
                    <div className="activity-title">{activity.title}</div>
                    <div className="activity-time">{activity.time}</div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="quick-actions">
        <h3>Quick Actions</h3>
        <div className="quick-actions-grid">
          <button 
            className="quick-action-card"
            onClick={() => navigate('/admin/users')}
          >
            <div className="quick-action-icon" style={{ background: '#e0f2fe', color: '#0284c7' }}>👥</div>
            <div className="quick-action-content">
              <div className="quick-action-title">Manage Teachers</div>
              <div className="quick-action-desc">Add, edit, or deactivate users</div>
            </div>
            <span className="quick-action-arrow">→</span>
          </button>

          <button 
            className="quick-action-card"
            onClick={() => navigate('/admin/classes')}
          >
            <div className="quick-action-icon" style={{ background: '#dcfce7', color: '#16a34a' }}>📚</div>
            <div className="quick-action-content">
              <div className="quick-action-title">Manage Classes</div>
              <div className="quick-action-desc">Create classes and assign teachers</div>
            </div>
            <span className="quick-action-arrow">→</span>
          </button>

          <button 
            className="quick-action-card"
            onClick={() => navigate('/admin/reports')}
          >
            <div className="quick-action-icon" style={{ background: '#fef3c7', color: '#d97706' }}>📊</div>
            <div className="quick-action-content">
              <div className="quick-action-title">View Reports</div>
              <div className="quick-action-desc">Analyze attendance and class data</div>
            </div>
            <span className="quick-action-arrow">→</span>
          </button>
        </div>
      </div>
    </div>
  );
};

export default AdminDashboard;