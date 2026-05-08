import React, { useState, useEffect } from 'react';
import {
  CalendarDays, BarChart2, CheckCircle2, XCircle,
  Clock, FileText, Download, ClipboardList
} from 'lucide-react';
import './AdminReports.css';

const AdminReports = () => {
  const [classes, setClasses]                     = useState([]);
  const [selectedClass, setSelectedClass]         = useState('');
  const [dateRange, setDateRange]                 = useState({
    startDate: new Date(new Date().setDate(new Date().getDate() - 7)).toISOString().split('T')[0],
    endDate:   new Date().toISOString().split('T')[0],
  });
  const [attendanceRecords, setAttendanceRecords] = useState([]);
  const [summary, setSummary]                     = useState(null);
  const [loading, setLoading]                     = useState(true);
  const [reportLoading, setReportLoading]         = useState(false);
  const [error, setError]                         = useState('');
  const [viewMode, setViewMode]                   = useState('daily'); // 'daily' | 'weekly'
  const [reportGenerated, setReportGenerated]     = useState(false);

  useEffect(() => { fetchClasses(); }, []);

  /* ── Fetch classes ───────────────────────────────────── */
  const fetchClasses = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem('accessToken');
      const res   = await fetch('http://localhost:8888/api/classes', {
        headers: { 'Authorization': `Bearer ${token}` },
      });
      const data = await res.json();
      if (data.success) setClasses(data.data || []);
      else setError('Failed to load classes');
    } catch {
      setError('Failed to load classes');
    } finally {
      setLoading(false);
    }
  };

  /* ── Generate report ─────────────────────────────────── */
  const generateReport = async () => {
    if (!selectedClass) { setError('Please select a class'); return; }
    setReportLoading(true);
    setError('');
    setReportGenerated(false);

    try {
      const token = localStorage.getItem('accessToken');
      let records = [];

      if (viewMode === 'daily') {
        // Daily — use existing /report/{date} endpoint
        const url  = `http://localhost:8888/api/attendance/class/${selectedClass}/report/${dateRange.startDate}`;
        const res  = await fetch(url, { headers: { 'Authorization': `Bearer ${token}` } });
        const data = await res.json();

        if (data.success) {
          records = data.data?.attendanceList || [];
        } else {
          setError(data.message || 'Failed to generate report');
          return;
        }

      } else {
        // Weekly — use new /range endpoint
        const url  = `http://localhost:8888/api/attendance/class/${selectedClass}/range?startDate=${dateRange.startDate}&endDate=${dateRange.endDate}`;
        const res  = await fetch(url, { headers: { 'Authorization': `Bearer ${token}` } });
        const data = await res.json();

        if (data.success) {
          records = data.data || [];
        } else {
          setError(data.message || 'Failed to generate report');
          return;
        }
      }

      setAttendanceRecords(records);
      computeSummary(records);
      setReportGenerated(true);

    } catch {
      setError('Failed to generate report');
    } finally {
      setReportLoading(false);
    }
  };

  /* ── Compute summary cards ───────────────────────────── */
  const computeSummary = (records) => {
    const present = records.filter(r => r.status?.toLowerCase() === 'present').length;
    const absent  = records.filter(r => r.status?.toLowerCase() === 'absent').length;
    const late    = records.filter(r => r.status?.toLowerCase() === 'late').length;
    const excused = records.filter(r => r.status?.toLowerCase() === 'excused').length;
    const total   = records.length;
    setSummary({
      total, present, absent, late, excused,
      presentRate: total > 0 ? Math.round((present / total) * 100) : 0,
      absentRate:  total > 0 ? Math.round((absent  / total) * 100) : 0,
      lateRate:    total > 0 ? Math.round((late    / total) * 100) : 0,
      excusedRate: total > 0 ? Math.round((excused / total) * 100) : 0,
    });
  };

  /* ── Export CSV ──────────────────────────────────────── */
  const exportToCSV = () => {
    if (!attendanceRecords.length) return;
    const header = 'Date,Student Name,Roll Number,Status,Remarks\n';
    const rows   = attendanceRecords.map(r =>
      `${r.date || dateRange.startDate},${r.studentName},${r.rollNumber || ''},${r.status},${r.remarks || ''}`
    ).join('\n');
    const blob = new Blob([header + rows], { type: 'text/csv' });
    const url  = window.URL.createObjectURL(blob);
    const a    = document.createElement('a');
    a.href     = url;
    a.download = `attendance-${viewMode}-${dateRange.startDate}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
  };

  const getStatusColor = (s) => ({
    present: '#10b981', absent: '#ef4444', late: '#f59e0b', excused: '#8b5cf6'
  }[s?.toLowerCase()] || '#64748b');

  const getStatusBg = (s) => ({
    present: '#d1fae5', absent: '#fee2e2', late: '#fef3c7', excused: '#ede9fe'
  }[s?.toLowerCase()] || '#f1f5f9');

  if (loading) return <div className="ar-loading">Loading reports...</div>;

  return (
    <div className="admin-reports">

      {/* Header */}
      <div className="reports-header">
        <h1>Reports & Analytics</h1>
        <p className="page-description">Generate and analyze attendance reports for your classes</p>
      </div>

      {/* Report Type Toggle */}
      <div className="report-type-selector">
        <button
          className={`type-btn ${viewMode === 'daily' ? 'active' : ''}`}
          onClick={() => { setViewMode('daily'); setReportGenerated(false); setAttendanceRecords([]); setSummary(null); }}
        >
          <CalendarDays size={16} /> Daily Report
        </button>
        <button
          className={`type-btn ${viewMode === 'weekly' ? 'active' : ''}`}
          onClick={() => { setViewMode('weekly'); setReportGenerated(false); setAttendanceRecords([]); setSummary(null); }}
        >
          <BarChart2 size={16} /> Weekly Report
        </button>
      </div>

      {/* Filters */}
      <div className="filters-section">
        <div className="filter-group">
          <label className="filter-label">Select Class</label>
          <select className="filter-select" value={selectedClass} onChange={e => setSelectedClass(e.target.value)}>
            <option value="">Choose a class</option>
            {classes.map(cls => (
              <option key={cls.classId} value={cls.classId}>
                {cls.className} — {cls.section}
              </option>
            ))}
          </select>
        </div>

        <div className="filter-group">
          <label className="filter-label">{viewMode === 'daily' ? 'Date' : 'Start Date'}</label>
          <input
            type="date"
            className="date-input"
            value={dateRange.startDate}
            onChange={e => setDateRange(prev => ({ ...prev, startDate: e.target.value }))}
          />
        </div>

        {viewMode === 'weekly' && (
          <div className="filter-group">
            <label className="filter-label">End Date</label>
            <input
              type="date"
              className="date-input"
              value={dateRange.endDate}
              onChange={e => setDateRange(prev => ({ ...prev, endDate: e.target.value }))}
            />
          </div>
        )}

        <div className="filter-actions">
          <button className="generate-btn" onClick={generateReport} disabled={reportLoading}>
            {reportLoading ? 'Generating...' : 'Generate Report'}
          </button>
          {attendanceRecords.length > 0 && (
            <button className="export-btn" onClick={exportToCSV}>
              <Download size={15} /> Export CSV
            </button>
          )}
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}

      {/* Summary Cards */}
      {summary && (
        <div className="summary-cards">
          <div className="summary-card total">
            <div className="summary-icon"><BarChart2 size={24} color="#0F2D5E" /></div>
            <div className="summary-info">
              <span className="summary-value">{summary.total}</span>
              <span className="summary-label">Total Records</span>
            </div>
          </div>
          <div className="summary-card present">
            <div className="summary-icon"><CheckCircle2 size={24} color="#10b981" /></div>
            <div className="summary-info">
              <span className="summary-value">{summary.present}</span>
              <span className="summary-label">Present</span>
              <span className="summary-percent">{summary.presentRate}%</span>
            </div>
          </div>
          <div className="summary-card absent">
            <div className="summary-icon"><XCircle size={24} color="#ef4444" /></div>
            <div className="summary-info">
              <span className="summary-value">{summary.absent}</span>
              <span className="summary-label">Absent</span>
              <span className="summary-percent">{summary.absentRate}%</span>
            </div>
          </div>
          <div className="summary-card late">
            <div className="summary-icon"><Clock size={24} color="#f59e0b" /></div>
            <div className="summary-info">
              <span className="summary-value">{summary.late}</span>
              <span className="summary-label">Late</span>
              <span className="summary-percent">{summary.lateRate}%</span>
            </div>
          </div>
          <div className="summary-card excused">
            <div className="summary-icon"><FileText size={24} color="#8b5cf6" /></div>
            <div className="summary-info">
              <span className="summary-value">{summary.excused}</span>
              <span className="summary-label">Excused</span>
              <span className="summary-percent">{summary.excusedRate}%</span>
            </div>
          </div>
        </div>
      )}

      {/* Attendance Table */}
      {attendanceRecords.length > 0 && (
        <div className="report-table-container">
          <h3>
            {viewMode === 'daily' ? 'Daily Attendance Report' : 'Weekly Attendance Report'}
            <span className="report-date-range">
              {viewMode === 'daily'
                ? new Date(dateRange.startDate + 'T00:00:00').toLocaleDateString('en-PH', { month: 'long', day: 'numeric', year: 'numeric' })
                : `${new Date(dateRange.startDate + 'T00:00:00').toLocaleDateString()} – ${new Date(dateRange.endDate + 'T00:00:00').toLocaleDateString()}`
              }
            </span>
          </h3>
          <div className="table-responsive">
            <table className="report-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Student Name</th>
                  <th>Roll Number</th>
                  <th>Status</th>
                  <th>Remarks</th>
                </tr>
              </thead>
              <tbody>
                {attendanceRecords.map((record, i) => (
                  <tr key={i}>
                    <td>
                      {new Date((record.date || dateRange.startDate) + 'T00:00:00').toLocaleDateString('en-PH', {
                        month: 'short', day: 'numeric', year: 'numeric'
                      })}
                    </td>
                    <td className="student-cell">
                      <div className="student-avatar-small">
                        {record.studentName?.split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase()}
                      </div>
                      {record.studentName}
                    </td>
                    <td className="text-center">{record.rollNumber || '—'}</td>
                    <td>
                      <span className="status-badge" style={{ background: getStatusBg(record.status), color: getStatusColor(record.status) }}>
                        <span className="status-dot" style={{ background: getStatusColor(record.status) }} />
                        {record.status}
                      </span>
                    </td>
                    <td>{record.remarks || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Empty state */}
      {reportGenerated && attendanceRecords.length === 0 && (
        <div className="empty-state">
          <div className="empty-icon"><ClipboardList size={56} color="#D6E0F0" /></div>
          <h3>No Records Found</h3>
          <p>No attendance records found for the selected class and date range.</p>
        </div>
      )}

      {!reportGenerated && !reportLoading && (
        <div className="empty-state">
          <div className="empty-icon"><ClipboardList size={56} color="#D6E0F0" /></div>
          <h3>No Report Generated</h3>
          <p>Select a class and date, then click Generate Report.</p>
        </div>
      )}

    </div>
  );
};

export default AdminReports;