import React, { useState, useEffect } from 'react';
import {
  BarChart2, ChevronDown, Calendar, Users, BookOpen,
  TrendingUp, CheckCircle2, XCircle,
  Clock, FileText, Download, RefreshCw, X,
  AlertCircle, GraduationCap, Percent, Filter
} from 'lucide-react';
import './TeacherReports.css';

const STATUS_CONFIG = {
  present: { label: 'Present', color: '#10B981', bg: '#D1FAE5', border: '#6EE7B7', icon: CheckCircle2 },
  absent:  { label: 'Absent',  color: '#EF4444', bg: '#FEE2E2', border: '#FCA5A5', icon: XCircle },
  late:    { label: 'Late',    color: '#F59E0B', bg: '#FEF3C7', border: '#FCD34D', icon: Clock },
  excused: { label: 'Excused', color: '#6366F1', bg: '#EDE9FE', border: '#C4B5FD', icon: FileText },
};

const PERIOD_OPTIONS = [
  { label: 'This Week',    value: 'this_week' },
  { label: 'Last Week',    value: 'last_week' },
  { label: 'Last 2 Weeks', value: 'last_2_weeks' },
  { label: 'This Month',   value: 'this_month' },
  { label: 'Custom Range', value: 'custom' },
];

/* ── helpers ─────────────────────────────────────────── */
const toISO = d => d.toISOString().split('T')[0];

const startOfWeek = (d) => {
  const day = new Date(d);
  day.setDate(d.getDate() - ((d.getDay() + 6) % 7)); // Monday
  return day;
};

function getDateRange(period) {
  const today = new Date();
  switch (period) {
    case 'this_week': {
      const s = startOfWeek(today);
      const e = new Date(s); e.setDate(s.getDate() + 6);
      return { from: toISO(s), to: toISO(e) };
    }
    case 'last_week': {
      const s = startOfWeek(today); s.setDate(s.getDate() - 7);
      const e = new Date(s); e.setDate(s.getDate() + 6);
      return { from: toISO(s), to: toISO(e) };
    }
    case 'last_2_weeks': {
      const s = startOfWeek(today); s.setDate(s.getDate() - 14);
      const e = new Date(startOfWeek(today)); e.setDate(e.getDate() + 6);
      return { from: toISO(s), to: toISO(e) };
    }
    case 'this_month': {
      const s = new Date(today.getFullYear(), today.getMonth(), 1);
      const e = new Date(today.getFullYear(), today.getMonth() + 1, 0);
      return { from: toISO(s), to: toISO(e) };
    }
    default: return { from: '', to: '' };
  }
}

const parseClassObj = (cls) => {
  if (cls.gradeLevel && cls.section)
    return { grade: `Grade ${cls.gradeLevel}`.trim(), section: cls.section.trim() };
  if (cls.section)
    return { grade: (cls.className || '').trim(), section: cls.section.trim() };
  const match = (cls.className || '').match(/^(Grade\s*\d+)\s*(.+)$/i);
  if (match) return { grade: match[1].trim(), section: match[2].trim() };
  return { grade: cls.className || '', section: null };
};

const formatDate = d =>
  d ? new Date(d + 'T00:00:00').toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) : '—';

/* ──────────────────────────────────────────────────────── */

const TeacherReports = () => {
  const [classes, setClasses]           = useState([]);
  const [classLoading, setClassLoading] = useState(true);

  // Filters
  const [selectedGrade, setSelectedGrade]     = useState('all');
  const [selectedSection, setSelectedSection] = useState('all'); // classId or 'all'
  const [period, setPeriod]                   = useState('this_week');
  const [customFrom, setCustomFrom]           = useState('');
  const [customTo, setCustomTo]               = useState('');

  // Dropdown open states
  const [showGradeDrop, setShowGradeDrop]     = useState(false);
  const [showSectionDrop, setShowSectionDrop] = useState(false);
  const [showPeriodDrop, setShowPeriodDrop]   = useState(false);

  // Report data (computed client-side)
  const [summary, setSummary]         = useState(null);
  const [studentRows, setStudentRows] = useState([]);
  const [weeklyTrend, setWeeklyTrend] = useState([]);
  const [loading, setLoading]         = useState(false);
  const [error, setError]             = useState('');
  const [generated, setGenerated]     = useState(false);

  const token = localStorage.getItem('accessToken');
  const user  = JSON.parse(localStorage.getItem('user') || '{}');
  const teacherId = user.userId || user.id;

  useEffect(() => { fetchClasses(); }, []);

  /* ── Grade options (sorted numerically) ─────────────── */
  const gradeOptions = (() => {
    const seen = new Set();
    const result = [];
    for (const c of classes) {
      const { grade } = parseClassObj(c);
      const key = (grade || '').trim().toLowerCase();
      if (!seen.has(key)) { seen.add(key); result.push(grade); }
    }
    return result.sort((a, b) => {
      const na = parseInt((a || '').match(/\d+/)?.[0] || '0');
      const nb = parseInt((b || '').match(/\d+/)?.[0] || '0');
      return na - nb;
    });
  })();

  /* ── Section options for selected grade ─────────────── */
  const sectionOptions = selectedGrade !== 'all'
    ? classes.filter(c => parseClassObj(c).grade?.trim().toLowerCase() === selectedGrade?.trim().toLowerCase())
    : [];

  /* ── Subject derived from selected section ───────────── */
  const selectedSectionObj = selectedSection !== 'all'
    ? classes.find(c => c.classId === selectedSection)
    : null;
  const derivedSubject = selectedSectionObj?.subject || null;

  /* ── Which classes to query ──────────────────────────── */
  const classesToQuery = (() => {
    if (selectedSection !== 'all') return classes.filter(c => c.classId === selectedSection);
    if (selectedGrade !== 'all')   return classes.filter(c => parseClassObj(c).grade?.trim().toLowerCase() === selectedGrade.trim().toLowerCase());
    return classes;
  })();

  const getRange = () =>
    period === 'custom' ? { from: customFrom, to: customTo } : getDateRange(period);

  /* ── Fetch classes ───────────────────────────────────── */
  const fetchClasses = async () => {
    setClassLoading(true);
    try {
      const res  = await fetch(`http://localhost:8888/api/classes/teacher/${teacherId}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = await res.json();
      if (data.success) setClasses(data.data || []);
    } catch { setError('Failed to load classes.'); }
    finally { setClassLoading(false); }
  };

  /* ── Generate report ─────────────────────────────────── */
  const fetchReport = async () => {
    const { from, to } = getRange();
    if (!from || !to) { setError('Please select a valid date range.'); return; }
    if (!classesToQuery.length) { setError('No classes match your selection.'); return; }

    setLoading(true);
    setError('');
    setGenerated(false);

    try {
      // Build list of all dates in range
      const dates = [];
      let cur = new Date(from + 'T00:00:00');
      const end = new Date(to + 'T00:00:00');
      while (cur <= end) { dates.push(toISO(cur)); cur.setDate(cur.getDate() + 1); }

      // Fetch attendance for every class × every date in parallel
      const fetches = classesToQuery.flatMap(cls =>
        dates.map(date =>
          fetch(`http://localhost:8888/api/attendance/class/${cls.classId}/date/${date}`, {
            headers: { Authorization: `Bearer ${token}` },
          })
          .then(r => r.json())
          .then(d => (d.success ? (d.data || []) : []).map(rec => ({ ...rec, _classId: cls.classId, _date: date })))
          .catch(() => [])
        )
      );

      const results = await Promise.all(fetches);
      const allRecords = results.flat().map(r => ({
        ...r,
        status: (r.status || 'present').toLowerCase(),
      }));

      /* ── Summary ── */
      const sum = { present: 0, absent: 0, late: 0, excused: 0 };
      allRecords.forEach(r => { if (sum[r.status] !== undefined) sum[r.status]++; });
      const totalRecords = allRecords.length;
      const averageAttendance = totalRecords
        ? Math.round(((sum.present + sum.late) / totalRecords) * 100)
        : 0;
      setSummary({ ...sum, totalRecords, averageAttendance });

      /* ── Per-student breakdown ── */
      const studentMap = {};
      allRecords.forEach(r => {
        const sid = r.studentId;
        if (!studentMap[sid]) {
          const fullName = r.studentName || '';
          const parts = fullName.trim().split(/\s+/);
          studentMap[sid] = {
            studentId:  sid,
            firstName:  parts.slice(0, -1).join(' ') || fullName,
            lastName:   parts.length > 1 ? parts[parts.length - 1] : '',
            rollNumber: r.rollNumber || '',
            className:  r.className || '',
            present: 0, absent: 0, late: 0, excused: 0,
          };
        }
        studentMap[sid][r.status] = (studentMap[sid][r.status] || 0) + 1;
      });

      const rows = Object.values(studentMap).map(s => {
        const total = s.present + s.absent + s.late + s.excused;
        return {
          ...s,
          total,
          attendanceRate: total ? Math.round(((s.present + s.late) / total) * 100) : 0,
        };
      }).sort((a, b) => a.lastName.localeCompare(b.lastName) || a.firstName.localeCompare(b.firstName));
      setStudentRows(rows);

      /* ── Weekly trend ── */
      // Group dates into weeks (Mon–Sun)
      const weekMap = {};
      dates.forEach(d => {
        const dt     = new Date(d + 'T00:00:00');
        const mon    = startOfWeek(dt);
        const key    = toISO(mon);
        if (!weekMap[key]) weekMap[key] = { present: 0, absent: 0, late: 0, excused: 0, dates: [] };
        weekMap[key].dates.push(d);
      });
      allRecords.forEach(r => {
        const dt  = new Date(r._date + 'T00:00:00');
        const key = toISO(startOfWeek(dt));
        if (weekMap[key] && weekMap[key][r.status] !== undefined) weekMap[key][r.status]++;
      });
      const trend = Object.entries(weekMap)
        .sort(([a], [b]) => a.localeCompare(b))
        .map(([key, w]) => {
          const dt = new Date(key + 'T00:00:00');
          return {
            ...w,
            label: dt.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
          };
        });
      setWeeklyTrend(trend);
      setGenerated(true);

    } catch (err) {
      console.error(err);
      setError('Failed to generate report. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  /* ── Export CSV ──────────────────────────────────────── */
  const handleExport = () => {
    if (!studentRows.length) return;
    const headers = ['Student Name', 'Roll No.', 'Class', 'Present', 'Absent', 'Late', 'Excused', 'Attendance %'];
    const rows = studentRows.map(s => [
      `${s.firstName} ${s.lastName}`, s.rollNumber, s.className,
      s.present, s.absent, s.late, s.excused, `${s.attendanceRate}%`
    ]);
    const csv  = [headers, ...rows].map(r => r.join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement('a');
    a.href = url;
    const { from, to } = getRange();
    a.download = `attendance-report-${from}-to-${to}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  /* ── Label helpers ───────────────────────────────────── */
  const gradeTriggerLabel   = selectedGrade === 'all' ? 'All Grades' : selectedGrade;
  const sectionTriggerLabel = (() => {
    if (selectedSection === 'all') return selectedGrade === 'all' ? '— select grade first' : 'All Sections';
    const cls = classes.find(c => c.classId === selectedSection);
    if (!cls) return 'All Sections';
    const { section } = parseClassObj(cls);
    return section || cls.subject || `Class ${cls.classId}`;
  })();
  const periodLabel = PERIOD_OPTIONS.find(o => o.value === period)?.label;
  const { from, to } = getRange();

  const barMax = weeklyTrend.length
    ? Math.max(...weeklyTrend.map(w => w.present + w.absent + w.late + w.excused), 1)
    : 1;

  return (
    <div className="tr-root">

      {/* ── Page Header ─────────────────────────────── */}
      <div className="tr-page-header">
        <div>
          <h1 className="tr-page-title">Attendance Reports</h1>
          <p className="tr-page-desc">Generate weekly summaries and per-student breakdowns</p>
        </div>
      </div>

      {error && (
        <div className="tr-error">
          <AlertCircle size={15} /> {error}
          <button className="tr-error-close" onClick={() => setError('')}><X size={13} /></button>
        </div>
      )}

      {/* ── Controls Card ───────────────────────────── */}
      <div className="tr-controls-card">
        <div className="tr-controls-row">

          {/* Grade */}
          <div className="tr-ctrl-group">
            <label className="tr-ctrl-label"><GraduationCap size={13} /> Grade</label>
            <div className="tr-dropdown-root">
              <button
                className={`tr-dropdown-trigger ${showGradeDrop ? 'tr-open' : ''}`}
                onClick={() => { setShowGradeDrop(v => !v); setShowSectionDrop(false); setShowPeriodDrop(false); }}
                disabled={classLoading}
              >
                <span>{classLoading ? 'Loading…' : gradeTriggerLabel}</span>
                <ChevronDown size={14} className="tr-chevron" />
              </button>
              {showGradeDrop && (
                <div className="tr-dropdown-menu">
                  <button
                    className={`tr-dropdown-item tr-period-item ${selectedGrade === 'all' ? 'tr-active' : ''}`}
                    onClick={() => { setSelectedGrade('all'); setSelectedSection('all'); setShowGradeDrop(false); }}
                  >All Grades</button>
                  {gradeOptions.map(grade => (
                    <button
                      key={grade}
                      className={`tr-dropdown-item tr-period-item ${selectedGrade === grade ? 'tr-active' : ''}`}
                      onClick={() => { setSelectedGrade(grade); setSelectedSection('all'); setShowGradeDrop(false); }}
                    >{grade}</button>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Section */}
          <div className="tr-ctrl-group">
            <label className="tr-ctrl-label"><Filter size={13} /> Section</label>
            <div className="tr-dropdown-root">
              <button
                className={`tr-dropdown-trigger ${showSectionDrop ? 'tr-open' : ''}`}
                onClick={() => { if (selectedGrade !== 'all') { setShowSectionDrop(v => !v); setShowGradeDrop(false); setShowPeriodDrop(false); } }}
                disabled={selectedGrade === 'all' || classLoading}
                style={selectedGrade === 'all' ? { opacity: 0.5, cursor: 'not-allowed' } : {}}
              >
                <span>{sectionTriggerLabel}</span>
                <ChevronDown size={14} className="tr-chevron" />
              </button>
              {showSectionDrop && (
                <div className="tr-dropdown-menu">
                  <button
                    className={`tr-dropdown-item tr-period-item ${selectedSection === 'all' ? 'tr-active' : ''}`}
                    onClick={() => { setSelectedSection('all'); setShowSectionDrop(false); }}
                  >All Sections</button>
                  {sectionOptions.map(cls => {
                    const { section } = parseClassObj(cls);
                    return (
                      <button
                        key={cls.classId}
                        className={`tr-dropdown-item tr-period-item ${selectedSection === cls.classId ? 'tr-active' : ''}`}
                        onClick={() => { setSelectedSection(cls.classId); setShowSectionDrop(false); }}
                      >{section || cls.subject || `Class ${cls.classId}`}</button>
                    );
                  })}
                </div>
              )}
            </div>
          </div>

          {/* Subject (read-only) */}
          <div className="tr-ctrl-group">
            <label className="tr-ctrl-label"><BookOpen size={13} /> Subject</label>
            <div className="tr-subject-display">
              {derivedSubject || <span className="tr-subject-placeholder">—</span>}
            </div>
          </div>

          {/* Period */}
          <div className="tr-ctrl-group">
            <label className="tr-ctrl-label"><Calendar size={13} /> Period</label>
            <div className="tr-dropdown-root">
              <button
                className={`tr-dropdown-trigger ${showPeriodDrop ? 'tr-open' : ''}`}
                onClick={() => { setShowPeriodDrop(v => !v); setShowGradeDrop(false); setShowSectionDrop(false); }}
              >
                <span>{periodLabel}</span>
                <ChevronDown size={14} className="tr-chevron" />
              </button>
              {showPeriodDrop && (
                <div className="tr-dropdown-menu">
                  {PERIOD_OPTIONS.map(opt => (
                    <button
                      key={opt.value}
                      className={`tr-dropdown-item tr-period-item ${period === opt.value ? 'tr-active' : ''}`}
                      onClick={() => { setPeriod(opt.value); setShowPeriodDrop(false); }}
                    >{opt.label}</button>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Custom dates */}
          {period === 'custom' && (
            <>
              <div className="tr-ctrl-group">
                <label className="tr-ctrl-label"><Calendar size={13} /> From</label>
                <input type="date" className="tr-date-input" value={customFrom} onChange={e => setCustomFrom(e.target.value)} />
              </div>
              <div className="tr-ctrl-group">
                <label className="tr-ctrl-label"><Calendar size={13} /> To</label>
                <input type="date" className="tr-date-input" value={customTo} onChange={e => setCustomTo(e.target.value)} />
              </div>
            </>
          )}

          {/* Generate */}
          <div className="tr-ctrl-group tr-generate-wrap">
            <label className="tr-ctrl-label">&nbsp;</label>
            <button className="tr-generate-btn" onClick={fetchReport} disabled={loading || classLoading}>
              {loading
                ? <><span className="tr-btn-spinner" /> Generating…</>
                : <><BarChart2 size={16} /> Generate Report</>
              }
            </button>
          </div>
        </div>

        {/* Date range + scope preview */}
        {from && to && (
          <div className="tr-range-preview">
            <Calendar size={13} />
            {formatDate(from)} — {formatDate(to)}
            {selectedGrade !== 'all' && <span className="tr-range-sep">·</span>}
            {selectedGrade !== 'all' && <span>{selectedGrade}</span>}
            {selectedSection !== 'all' && sectionTriggerLabel !== 'All Sections' && (
              <><span className="tr-range-sep">—</span><span>{sectionTriggerLabel}</span></>
            )}
            {derivedSubject && <><span className="tr-range-sep">·</span><span>{derivedSubject}</span></>}
          </div>
        )}
      </div>

      {/* ── Report Output ───────────────────────────── */}
      {loading && (
        <div className="tr-loading">
          <div className="tr-spinner" />
          <p>Generating report…</p>
        </div>
      )}

      {!loading && !generated && (
        <div className="tr-placeholder">
          <div className="tr-placeholder-icon"><BarChart2 size={40} /></div>
          <h3>No Report Generated Yet</h3>
          <p>Select a grade, section, and period above, then click <strong>Generate Report</strong> to see results.</p>
        </div>
      )}

      {!loading && generated && (
        <>
          {/* ── Summary Cards ───────────────────────── */}
          {summary && (
            <div className="tr-summary-grid">
              <div className="tr-sum-card tr-sum-total">
                <div className="tr-sum-icon"><GraduationCap size={22} /></div>
                <div className="tr-sum-body">
                  <span className="tr-sum-label">Total Records</span>
                  <span className="tr-sum-value">{summary.totalRecords ?? 0}</span>
                </div>
              </div>
              {Object.entries(STATUS_CONFIG).map(([key, cfg]) => {
                const Icon = cfg.icon;
                return (
                  <div key={key} className="tr-sum-card" style={{ '--sc-color': cfg.color, '--sc-bg': cfg.bg, '--sc-border': cfg.border }}>
                    <div className="tr-sum-icon tr-sum-status-icon"><Icon size={20} /></div>
                    <div className="tr-sum-body">
                      <span className="tr-sum-label">{cfg.label}</span>
                      <span className="tr-sum-value">{summary[key] ?? 0}</span>
                      <span className="tr-sum-pct">
                        {summary.totalRecords
                          ? Math.round(((summary[key] ?? 0) / summary.totalRecords) * 100) : 0}%
                      </span>
                    </div>
                  </div>
                );
              })}
              <div className="tr-sum-card tr-sum-rate">
                <div className="tr-sum-icon"><Percent size={22} /></div>
                <div className="tr-sum-body">
                  <span className="tr-sum-label">Avg. Attendance</span>
                  <span className="tr-sum-value">{summary.averageAttendance ?? 0}%</span>
                  <div className="tr-rate-bar-wrap">
                    <div className="tr-rate-bar" style={{ width: `${summary.averageAttendance ?? 0}%` }} />
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* ── Weekly Trend Chart ───────────────────── */}
          {weeklyTrend.length > 0 && (
            <div className="tr-chart-card">
              <div className="tr-card-header">
                <div className="tr-card-title"><TrendingUp size={17} /> Weekly Trend</div>
              </div>
              <div className="tr-chart-body">
                <div className="tr-bar-chart">
                  {weeklyTrend.map((w, i) => {
                    const total = w.present + w.absent + w.late + w.excused;
                    return (
                      <div key={i} className="tr-bar-col">
                        <div className="tr-bar-stack-wrap">
                          <div className="tr-bar-stack" style={{ height: `${(total / barMax) * 100}%` }}>
                            {(['present','late','excused','absent']).map(s => {
                              const pct = total ? (w[s] / total) * 100 : 0;
                              if (!pct) return null;
                              return (
                                <div key={s} className="tr-bar-segment"
                                  style={{ height: `${pct}%`, background: STATUS_CONFIG[s].color }}
                                  title={`${STATUS_CONFIG[s].label}: ${w[s]}`}
                                />
                              );
                            })}
                          </div>
                          <span className="tr-bar-count">{total}</span>
                        </div>
                        <span className="tr-bar-label">{w.label}</span>
                      </div>
                    );
                  })}
                </div>
                <div className="tr-chart-legend">
                  {Object.entries(STATUS_CONFIG).map(([key, cfg]) => (
                    <div key={key} className="tr-legend-item">
                      <span className="tr-legend-dot" style={{ background: cfg.color }} />
                      {cfg.label}
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* ── Student Breakdown ────────────────────── */}
          <div className="tr-table-card">
            <div className="tr-card-header">
              <div className="tr-card-title"><Users size={17} /> Student Breakdown</div>
              <button className="tr-export-btn" onClick={handleExport} disabled={!studentRows.length}>
                <Download size={14} /> Export CSV
              </button>
            </div>
            {studentRows.length === 0 ? (
              <div className="tr-empty"><p>No student data for the selected period.</p></div>
            ) : (
              <div className="tr-table-wrap">
                <table className="tr-table">
                  <thead>
                    <tr>
                      <th>Student</th>
                      <th>Class</th>
                      <th className="tr-center">Present</th>
                      <th className="tr-center">Absent</th>
                      <th className="tr-center">Late</th>
                      <th className="tr-center">Excused</th>
                      <th className="tr-center">Attendance %</th>
                      <th>Standing</th>
                    </tr>
                  </thead>
                  <tbody>
                    {studentRows.map((s, idx) => {
                      const rate = s.attendanceRate ?? 0;
                      const rateColor  = rate >= 80 ? '#10B981' : rate >= 60 ? '#F59E0B' : '#EF4444';
                      const standingBg = rate >= 80 ? '#D1FAE5' : rate >= 60 ? '#FEF3C7' : '#FEE2E2';
                      const standingBd = rate >= 80 ? '#6EE7B7' : rate >= 60 ? '#FCD34D' : '#FCA5A5';
                      return (
                        <tr key={s.studentId} style={{ animationDelay: `${idx * 20}ms` }}>
                          <td>
                            <div className="tr-student-cell">
                              <div className="tr-avatar">
                                {((s.firstName?.[0] || '') + (s.lastName?.[0] || '')).toUpperCase()}
                              </div>
                              <div className="tr-student-info">
                                <span className="tr-student-name">{s.firstName} {s.lastName}</span>
                                <span className="tr-student-roll">{s.rollNumber}</span>
                              </div>
                            </div>
                          </td>
                          <td className="tr-class-cell">{s.className}</td>
                          <td className="tr-center"><span className="tr-count-badge tr-present">{s.present ?? 0}</span></td>
                          <td className="tr-center"><span className="tr-count-badge tr-absent">{s.absent ?? 0}</span></td>
                          <td className="tr-center"><span className="tr-count-badge tr-late">{s.late ?? 0}</span></td>
                          <td className="tr-center"><span className="tr-count-badge tr-excused">{s.excused ?? 0}</span></td>
                          <td className="tr-center">
                            <div className="tr-rate-cell">
                              <span className="tr-rate-num" style={{ color: rateColor }}>{rate}%</span>
                              <div className="tr-mini-bar-bg">
                                <div className="tr-mini-bar-fill" style={{ width: `${rate}%`, background: rateColor }} />
                              </div>
                            </div>
                          </td>
                          <td>
                            <span className="tr-standing-badge"
                              style={{ '--st-color': rateColor, '--st-bg': standingBg, '--st-border': standingBd }}>
                              {rate >= 80 ? 'Good' : rate >= 60 ? 'At Risk' : 'Critical'}
                            </span>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Footer */}
          <div className="tr-report-footer">
            <span className="tr-footer-note">
              {formatDate(from)} — {formatDate(to)}
              {selectedGrade !== 'all' ? ` · ${selectedGrade}` : ' · All Grades'}
              {derivedSubject ? ` · ${derivedSubject}` : ''}
            </span>
            <button className="tr-refresh-btn" onClick={fetchReport}>
              <RefreshCw size={14} /> Regenerate
            </button>
          </div>
        </>
      )}
    </div>
  );
};

export default TeacherReports;