import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  BookOpen, Users, CheckCircle2, XCircle, Clock,
  BarChart2, ClipboardList, ArrowRight, Activity,
  CalendarDays, TrendingUp, GraduationCap, Bell
} from 'lucide-react';
import WeatherWidget from './WeatherWidget'; 
import './TeacherDashboard.css';

/* ── helpers ─────────────────────────────────────────── */
const parseClassObj = (cls) => {
  if (cls.gradeLevel && cls.section)
    return { grade: `Grade ${cls.gradeLevel}`.trim(), section: cls.section.trim() };
  if (cls.section)
    return { grade: (cls.className || '').trim(), section: cls.section.trim() };
  const match = (cls.className || '').match(/^(Grade\s*\d+)\s*(.+)$/i);
  if (match) return { grade: match[1].trim(), section: match[2].trim() };
  return { grade: cls.className || '', section: null };
};

const timeToMinutes = (t) => {
  if (!t) return null;
  const [h, m] = t.split(':').map(Number);
  return h * 60 + (m || 0);
};

const formatTime = (t) => {
  if (!t) return '';
  const [h, m] = t.split(':').map(Number);
  const ampm = h >= 12 ? 'PM' : 'AM';
  return `${h % 12 || 12}:${String(m).padStart(2, '0')} ${ampm}`;
};

/* ──────────────────────────────────────────────────────── */

const TeacherDashboard = () => {
  const [stats, setStats] = useState({
    totalClasses: 0, totalStudents: 0,
    todayPresent: 0, todayAbsent: 0,
    todayLate: 0, todayExcused: 0,
  });
  const [myClasses, setMyClasses]     = useState([]);
  const [nextClass, setNextClass]     = useState(null);
  const [loading, setLoading]         = useState(true);
  const [userName, setUserName]       = useState('');
  const [currentTime, setCurrentTime] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    setUserName(user.fullName || user.name || user.username || 'Teacher');
    updateTime();
    const timer = setInterval(updateTime, 60000);
    fetchTeacherData();
    return () => clearInterval(timer);
  }, []);

  const updateTime = () => {
    setCurrentTime(new Date().toLocaleDateString('en-US', {
      weekday: 'long', year: 'numeric', month: 'long', day: 'numeric'
    }));
  };

  const fetchTeacherData = async () => {
    setLoading(true);
    try {
      const token     = localStorage.getItem('accessToken');
      const user      = JSON.parse(localStorage.getItem('user') || '{}');
      const teacherId = user.userId || user.id;

      const classesRes  = await fetch(
        `http://localhost:8888/api/classes/teacher/${teacherId}`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      const classesData = await classesRes.json();
      const classes     = classesData.data || [];

      const today = new Date().toISOString().split('T')[0];
      let totalStudents = 0, totalPresent = 0, totalAbsent = 0,
          totalLate = 0, totalExcused = 0;

      const classDetails = await Promise.all(classes.map(async (cls) => {
        try {
          const res  = await fetch(
            `http://localhost:8888/api/attendance/class/${cls.classId}/date/${today}`,
            { headers: { Authorization: `Bearer ${token}` } }
          );
          const data = await res.json();
          const att  = data.data || [];
          const norm    = s => (s || '').toLowerCase();
          const present = att.filter(a => norm(a.status) === 'present').length;
          const absent  = att.filter(a => norm(a.status) === 'absent').length;
          const late    = att.filter(a => norm(a.status) === 'late').length;
          const excused = att.filter(a => norm(a.status) === 'excused').length;
          totalStudents += cls.studentCount || 0;
          totalPresent  += present;
          totalAbsent   += absent;
          totalLate     += late;
          totalExcused  += excused;
          const { grade, section } = parseClassObj(cls);
          return {
            ...cls, grade, section,
            presentCount: present, absentCount: absent,
            lateCount: late, excusedCount: excused,
            attendanceRate: cls.studentCount
              ? Math.round(((present + late) / cls.studentCount) * 100) : 0,
          };
        } catch {
          const { grade, section } = parseClassObj(cls);
          totalStudents += cls.studentCount || 0;
          return { ...cls, grade, section, presentCount: 0, absentCount: 0, lateCount: 0, excusedCount: 0, attendanceRate: 0 };
        }
      }));

      const nowMins = new Date().getHours() * 60 + new Date().getMinutes();
      const dayName = new Date().toLocaleDateString('en-US', { weekday: 'long' }).toLowerCase();
      const scheduled = classDetails
        .filter(cls => {
          if (!cls.scheduleTime) return false;
          if (cls.scheduleDay) {
            const days = cls.scheduleDay.toLowerCase().split(/[,\s]+/);
            return days.some(d => dayName.startsWith(d.trim().slice(0, 3)));
          }
          return true;
        })
        .sort((a, b) => timeToMinutes(a.scheduleTime) - timeToMinutes(b.scheduleTime));
      const upcoming = scheduled.filter(c => timeToMinutes(c.scheduleTime) > nowMins);
      const next     = upcoming[0] || scheduled[scheduled.length - 1] || null;
      if (next) {
        setNextClass({
          grade: next.grade, section: next.section, subject: next.subject,
          time: formatTime(next.scheduleTime),
          timeEnd: next.scheduleTimeEnd ? formatTime(next.scheduleTimeEnd) : null,
          students: next.studentCount || 0, isPast: !upcoming[0],
        });
      }
      setStats({ totalClasses: classes.length, totalStudents, todayPresent: totalPresent, todayAbsent: totalAbsent, todayLate: totalLate, todayExcused: totalExcused });
      setMyClasses(classDetails);
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

  if (loading) {
    return (
      <div className="td-loading">
        <div className="td-spinner" />
        <p>Loading your dashboard...</p>
      </div>
    );
  }

  const totalMarked = stats.todayPresent + stats.todayAbsent + stats.todayLate + stats.todayExcused;
  const firstName   = userName.split(' ')[0];

  const statCards = [
    { label: 'My Classes',     value: stats.totalClasses,  icon: <BookOpen size={22} />,      accent: '#3B6FD4' },
    { label: 'Total Students', value: stats.totalStudents, icon: <GraduationCap size={22} />, accent: '#2DB87B' },
    {
      label: 'Present Today', value: stats.todayPresent,
      sub: `${Math.round((stats.todayPresent / (totalMarked || 1)) * 100)}% of marked`,
      icon: <CheckCircle2 size={22} />, accent: '#10B981'
    },
    {
      label: 'Absent / Late', value: stats.todayAbsent + stats.todayLate,
      sub: `${stats.todayLate} late · ${stats.todayAbsent} absent`,
      icon: <XCircle size={22} />, accent: '#EF4444'
    }
  ];

  return (
    <div className="td-root">

      {/* ── Hero Banner ─────────────────────────────── */}
      <div className="td-hero">
        <div className="td-hero-bg" />
        <div className="td-hero-content">
          <div>
            <p className="td-greeting-label">{getGreeting()}</p>
            <h1 className="td-hero-name">{firstName} <span className="td-wave">👋</span></h1>
            <p className="td-hero-sub">Ready to take attendance for your classes today?</p>
          </div>
          <div className="td-date-pill">
            <CalendarDays size={15} />
            {currentTime}
          </div>
        </div>
      </div>

      {/* ── Next Class Reminder ──────────────────────── */}
      {nextClass && (
        <div className={`td-next-banner${nextClass.isPast ? ' td-next-past' : ''}`}>
          <div className="td-next-bell"><Bell size={20} /></div>
          <div className="td-next-body">
            <span className="td-next-eyebrow">
              {nextClass.isPast ? 'Last class today' : 'Your next class'}
            </span>
            <span className="td-next-title">
              {nextClass.grade}{nextClass.section ? ` ${nextClass.section}` : ''}
              {nextClass.subject ? <span className="td-next-subject"> · {nextClass.subject}</span> : null}
            </span>
            <span className="td-next-meta">
              <Clock size={13} />
              {nextClass.time}{nextClass.timeEnd ? ` – ${nextClass.timeEnd}` : ''}
              &nbsp;·&nbsp;{nextClass.students} students
            </span>
          </div>
          <button className="td-next-cta" onClick={() => navigate('/teacher/take-attendance')}>
            Take Attendance <ArrowRight size={14} />
          </button>
        </div>
      )}

      {/* ── Quick Actions ────────────────────────────── */}
      <div className="td-actions-row">
        <button className="td-action-btn td-action-primary" onClick={() => navigate('/teacher/take-attendance')}>
          <ClipboardList size={18} /> Take Attendance <ArrowRight size={15} className="td-arrow" />
        </button>
        <button className="td-action-btn td-action-ghost" onClick={() => navigate('/teacher/history')}>
          <Activity size={18} /> View History
        </button>
        <button className="td-action-btn td-action-ghost" onClick={() => navigate('/teacher/reports')}>
          <BarChart2 size={18} /> Reports
        </button>
      </div>

      {/* ── Stats Row ────────────────────────────────── */}
      <div className="td-stats-row">
        {statCards.map((s, i) => (
          <div key={i} className="td-stat-card" style={{ '--accent': s.accent }}>
            <div className="td-stat-icon-wrap">{s.icon}</div>
            <div className="td-stat-body">
              <span className="td-stat-label">{s.label}</span>
              <span className="td-stat-value">{s.value}</span>
              {s.sub && <span className="td-stat-sub">{s.sub}</span>}
            </div>
          </div>
        ))}
      </div>

      {/* ── Main Grid ────────────────────────────────── */}
      <div className="td-main-grid">

        {/* Left Column: My Classes */}
        <div className="td-card td-classes-card">
          <div className="td-card-header">
            <div className="td-card-title"><BookOpen size={17} /> My Classes</div>
            <button className="td-header-btn" onClick={() => navigate('/teacher/take-attendance')}>
              Take Attendance <ArrowRight size={13} />
            </button>
          </div>
          <div className="td-classes-list">
            {myClasses.length === 0
              ? <div className="td-empty"><p>No classes assigned yet.</p></div>
              : myClasses.map(cls => (
                <div key={cls.classId} className="td-class-row">
                  <div className="td-class-dot" />
                  <div className="td-class-info">
                    <span className="td-class-name">
                      {cls.grade}{cls.section ? ` — ${cls.section}` : ''}
                    </span>
                    <span className="td-class-meta">
                      {cls.subject}
                      {cls.scheduleTime ? ` · ${formatTime(cls.scheduleTime)}` : ''}
                      {' · '}{cls.studentCount || 0} students
                    </span>
                  </div>
                  <div className="td-class-right">
                    <span className="td-present-chip">
                      <span className="td-green-dot" />
                      {cls.presentCount || 0} present
                    </span>
                    <div className="td-ring-wrap">
                      <svg width="48" height="48" viewBox="0 0 48 48">
                        <circle cx="24" cy="24" r="19" fill="none" stroke="#E8EFF8" strokeWidth="4" />
                        <circle
                          cx="24" cy="24" r="19" fill="none"
                          stroke={cls.attendanceRate > 80 ? '#10B981' : cls.attendanceRate > 60 ? '#F59E0B' : '#EF4444'}
                          strokeWidth="4"
                          strokeDasharray={`${(cls.attendanceRate / 100) * 119.4} 119.4`}
                          strokeLinecap="round"
                          transform="rotate(-90 24 24)"
                        />
                      </svg>
                      <span className="td-ring-text">{cls.attendanceRate}%</span>
                    </div>
                  </div>
                </div>
              ))
            }
          </div>
        </div>

        {/* Right Column: Weather + Schedule + Summary */}
        <div className="td-right-col">
          
          {/* ✅ SIDEBAR WEATHER: Now at the top of the right column */}
          <div className="td-weather-sidebar-wrapper" style={{ marginBottom: '0 rem' }}>
             <WeatherWidget />
          </div>

          {/* Today's Schedule */}
          <div className="td-card">
            <div className="td-card-header">
              <div className="td-card-title"><Clock size={17} /> Today's Schedule</div>
              <span className="td-date-chip">
                {new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
              </span>
            </div>
            <div className="td-schedule-list">
              {myClasses.length === 0
                ? <div className="td-empty"><p>No classes today.</p></div>
                : [...myClasses]
                    .sort((a, b) => timeToMinutes(a.scheduleTime) - timeToMinutes(b.scheduleTime))
                    .map(cls => {
                      const nowMins = new Date().getHours() * 60 + new Date().getMinutes();
                      const isPast  = timeToMinutes(cls.scheduleTime) !== null && timeToMinutes(cls.scheduleTime) < nowMins;
                      return (
                        <div key={cls.classId} className={`td-schedule-row${isPast ? ' td-sched-past' : ''}`}>
                          <span className="td-sched-time">
                            {cls.scheduleTime ? formatTime(cls.scheduleTime) : '—'}
                          </span>
                          <div className="td-sched-info">
                            <span className="td-sched-name">
                              {cls.grade}{cls.section ? ` — ${cls.section}` : ''}
                            </span>
                            <span className="td-sched-meta">
                              {cls.subject} · {cls.studentCount || 0} students
                            </span>
                          </div>
                          <button className="td-take-btn" onClick={() => navigate('/teacher/take-attendance')}>
                            Take
                          </button>
                        </div>
                      );
                    })
              }
            </div>
          </div>

          {/* Today's Summary */}
          <div className="td-card">
            <div className="td-card-header">
              <div className="td-card-title"><TrendingUp size={17} /> Today's Summary</div>
              <button className="td-header-btn" onClick={() => navigate('/teacher/history')}>
                View All <ArrowRight size={13} />
              </button>
            </div>
            <div className="td-summary-list">
              {[
                { label: 'Present', value: stats.todayPresent, color: '#10B981', bg: '#D1FAE5' },
                { label: 'Absent',  value: stats.todayAbsent,  color: '#EF4444', bg: '#FEE2E2' },
                { label: 'Late',    value: stats.todayLate,    color: '#F59E0B', bg: '#FEF3C7' },
                { label: 'Excused', value: stats.todayExcused, color: '#6366F1', bg: '#EDE9FE' },
              ].map(item => (
                <div key={item.label} className="td-summary-row">
                  <span className="td-summary-dot" style={{ background: item.color }} />
                  <span className="td-summary-label">{item.label}</span>
                  <span className="td-summary-badge" style={{ color: item.color, background: item.bg }}>
                    {item.value}
                  </span>
                </div>
              ))}
              <div className="td-summary-total">
                <span>Total Marked</span>
                <strong>{totalMarked}</strong>
              </div>
            </div>
          </div>

        </div>
      </div>
    </div>
  );
};

export default TeacherDashboard;