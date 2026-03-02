import React, { useState, useEffect } from 'react';
import { attendanceService } from '../../services/attendanceService';
import { classService } from '../../services/classService';
import './AdminReports.css';

const AdminReports = () => {
  const [classes, setClasses] = useState([]);
  const [selectedClass, setSelectedClass] = useState('');
  const [dateRange, setDateRange] = useState({
    startDate: new Date(new Date().setDate(new Date().getDate() - 30)).toISOString().split('T')[0],
    endDate: new Date().toISOString().split('T')[0]
  });
  const [reportData, setReportData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [viewMode, setViewMode] = useState('daily'); // 'daily', 'weekly', 'monthly'

  useEffect(() => {
    fetchClasses();
  }, []);

  const fetchClasses = async () => {
    try {
      const response = await classService.getAllClasses();
      if (response.success) {
        setClasses(response.data);
      }
    } catch (err) {
      console.error('Error fetching classes:', err);
    }
  };

  const generateReport = async () => {
    if (!selectedClass) {
      setError('Please select a class');
      return;
    }

    setLoading(true);
    setError('');

    try {
      let response;
      
      if (viewMode === 'daily') {
        // For daily view, use specific date
        response = await attendanceService.getDailyReport(selectedClass, dateRange.startDate);
      } else {
        // For weekly/monthly, get range data
        response = await attendanceService.getAttendanceByClassAndDateRange(
          selectedClass, 
          dateRange.startDate, 
          dateRange.endDate
        );
      }

      if (response.success) {
        setReportData(response.data);
      } else {
        setError('Failed to generate report');
      }
    } catch (err) {
      setError('Error generating report');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const exportToCSV = () => {
    if (!reportData) return;

    let csvContent = "Date,Student Name,Status,Remarks\n";
    
    if (viewMode === 'daily' && reportData.attendanceList) {
      reportData.attendanceList.forEach(record => {
        csvContent += `${reportData.date},${record.studentName},${record.status},${record.remarks || ''}\n`;
      });
    } else if (reportData) {
      // Handle range data format
      reportData.forEach(record => {
        csvContent += `${record.date},${record.studentName},${record.status},${record.remarks || ''}\n`;
      });
    }

    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `attendance-report-${selectedClass}-${dateRange.startDate}.csv`;
    a.click();
  };

  const getStatusColor = (status) => {
    const colors = {
      PRESENT: '#22c55e',
      ABSENT: '#ef4444',
      LATE: '#f59e0b',
      EXCUSED: '#6366f1'
    };
    return colors[status?.toUpperCase()] || '#64748b';
  };

  const getStatusBg = (status) => {
    const colors = {
      PRESENT: '#dcfce7',
      ABSENT: '#fee2e2',
      LATE: '#fef3c7',
      EXCUSED: '#ede9fe'
    };
    return colors[status?.toUpperCase()] || '#f1f5f9';
  };

  const calculateSummary = () => {
    if (!reportData) return null;

    let total = 0;
    let present = 0;
    let absent = 0;
    let late = 0;
    let excused = 0;

    if (viewMode === 'daily' && reportData.attendanceList) {
      reportData.attendanceList.forEach(record => {
        total++;
        switch(record.status?.toUpperCase()) {
          case 'PRESENT': present++; break;
          case 'ABSENT': absent++; break;
          case 'LATE': late++; break;
          case 'EXCUSED': excused++; break;
          default: break;
        }
      });
    } else if (reportData) {
      // Calculate from range data
      reportData.forEach(record => {
        total++;
        switch(record.status?.toUpperCase()) {
          case 'PRESENT': present++; break;
          case 'ABSENT': absent++; break;
          case 'LATE': late++; break;
          case 'EXCUSED': excused++; break;
          default: break;
        }
      });
    }

    return {
      total,
      present,
      absent,
      late,
      excused,
      presentRate: total > 0 ? Math.round((present / total) * 100) : 0,
      absentRate: total > 0 ? Math.round((absent / total) * 100) : 0,
      lateRate: total > 0 ? Math.round((late / total) * 100) : 0,
      excusedRate: total > 0 ? Math.round((excused / total) * 100) : 0
    };
  };

  const summary = calculateSummary();

  return (
    <div className="admin-reports">
      <div className="reports-header">
        <h2>Attendance Reports</h2>
      </div>

      {/* Filters */}
      <div className="reports-filters">
        <div className="filter-group">
          <label className="filter-label">Class</label>
          <select
            className="filter-select"
            value={selectedClass}
            onChange={(e) => setSelectedClass(e.target.value)}
          >
            <option value="">Select a class</option>
            {classes.map(cls => (
              <option key={cls.classId} value={cls.classId}>
                {cls.className} - {cls.subject}
              </option>
            ))}
          </select>
        </div>

        <div className="filter-group">
          <label className="filter-label">View Mode</label>
          <div className="view-mode-buttons">
            <button
              className={`mode-btn ${viewMode === 'daily' ? 'active' : ''}`}
              onClick={() => setViewMode('daily')}
            >
              Daily
            </button>
            <button
              className={`mode-btn ${viewMode === 'weekly' ? 'active' : ''}`}
              onClick={() => setViewMode('weekly')}
            >
              Weekly
            </button>
            <button
              className={`mode-btn ${viewMode === 'monthly' ? 'active' : ''}`}
              onClick={() => setViewMode('monthly')}
            >
              Monthly
            </button>
          </div>
        </div>

        <div className="filter-group">
          <label className="filter-label">Date Range</label>
          <div className="date-range">
            <input
              type="date"
              className="date-input"
              value={dateRange.startDate}
              onChange={(e) => setDateRange({ ...dateRange, startDate: e.target.value })}
            />
            <span className="date-separator">to</span>
            <input
              type="date"
              className="date-input"
              value={dateRange.endDate}
              onChange={(e) => setDateRange({ ...dateRange, endDate: e.target.value })}
            />
          </div>
        </div>

        <div className="filter-actions">
          <button 
            className="btn-primary"
            onClick={generateReport}
            disabled={loading || !selectedClass}
          >
            {loading ? 'Generating...' : 'Generate Report'}
          </button>
          {reportData && (
            <button 
              className="btn-outline"
              onClick={exportToCSV}
            >
              📥 Export CSV
            </button>
          )}
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}

      {/* Summary Cards */}
      {summary && reportData && (
        <div className="summary-cards">
          <div className="summary-card total">
            <div className="summary-icon">📊</div>
            <div className="summary-info">
              <div className="summary-value">{summary.total}</div>
              <div className="summary-label">Total Records</div>
            </div>
          </div>

          <div className="summary-card present">
            <div className="summary-icon">✅</div>
            <div className="summary-info">
              <div className="summary-value">{summary.present}</div>
              <div className="summary-label">Present</div>
              <div className="summary-percent">{summary.presentRate}%</div>
            </div>
          </div>

          <div className="summary-card absent">
            <div className="summary-icon">❌</div>
            <div className="summary-info">
              <div className="summary-value">{summary.absent}</div>
              <div className="summary-label">Absent</div>
              <div className="summary-percent">{summary.absentRate}%</div>
            </div>
          </div>

          <div className="summary-card late">
            <div className="summary-icon">⏰</div>
            <div className="summary-info">
              <div className="summary-value">{summary.late}</div>
              <div className="summary-label">Late</div>
              <div className="summary-percent">{summary.lateRate}%</div>
            </div>
          </div>

          <div className="summary-card excused">
            <div className="summary-icon">📝</div>
            <div className="summary-info">
              <div className="summary-value">{summary.excused}</div>
              <div className="summary-label">Excused</div>
              <div className="summary-percent">{summary.excusedRate}%</div>
            </div>
          </div>
        </div>
      )}

      {/* Report Table */}
      {reportData && (
        <div className="report-table-container">
          <h3>
            {viewMode === 'daily' 
              ? `Attendance Report for ${new Date(dateRange.startDate).toLocaleDateString('en-PH', { 
                  weekday: 'long', 
                  year: 'numeric', 
                  month: 'long', 
                  day: 'numeric' 
                })}`
              : `Attendance Report from ${new Date(dateRange.startDate).toLocaleDateString()} to ${new Date(dateRange.endDate).toLocaleDateString()}`
            }
          </h3>

          <div className="table-responsive">
            <table className="report-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Student Name</th>
                  <th>Student ID</th>
                  <th>Status</th>
                  <th>Remarks</th>
                </tr>
              </thead>
              <tbody>
                {viewMode === 'daily' && reportData.attendanceList ? (
                  reportData.attendanceList.map((record, index) => (
                    <tr key={index}>
                      <td>{new Date(reportData.date).toLocaleDateString()}</td>
                      <td>{record.studentName}</td>
                      <td>{record.rollNumber}</td>
                      <td>
                        <span 
                          className="status-badge"
                          style={{ 
                            background: getStatusBg(record.status),
                            color: getStatusColor(record.status)
                          }}
                        >
                          <span 
                            className="status-dot" 
                            style={{ background: getStatusColor(record.status) }} 
                          />
                          {record.status}
                        </span>
                      </td>
                      <td>{record.remarks || '-'}</td>
                    </tr>
                  ))
                ) : reportData && reportData.length > 0 ? (
                  reportData.map((record, index) => (
                    <tr key={index}>
                      <td>{new Date(record.date).toLocaleDateString()}</td>
                      <td>{record.studentName}</td>
                      <td>{record.rollNumber}</td>
                      <td>
                        <span 
                          className="status-badge"
                          style={{ 
                            background: getStatusBg(record.status),
                            color: getStatusColor(record.status)
                          }}
                        >
                          <span 
                            className="status-dot" 
                            style={{ background: getStatusColor(record.status) }} 
                          />
                          {record.status}
                        </span>
                      </td>
                      <td>{record.remarks || '-'}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="5" className="empty-state">
                      No attendance records found for the selected period.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Chart Section - Placeholder for future chart integration */}
      {summary && reportData && (
        <div className="chart-section">
          <h3>Attendance Distribution</h3>
          <div className="chart-container">
            <div className="chart-bars">
              <div className="chart-bar-item">
                <div 
                  className="chart-bar present" 
                  style={{ height: `${summary.presentRate}%` }}
                />
                <span className="chart-label">Present</span>
                <span className="chart-value">{summary.presentRate}%</span>
              </div>
              <div className="chart-bar-item">
                <div 
                  className="chart-bar absent" 
                  style={{ height: `${summary.absentRate}%` }}
                />
                <span className="chart-label">Absent</span>
                <span className="chart-value">{summary.absentRate}%</span>
              </div>
              <div className="chart-bar-item">
                <div 
                  className="chart-bar late" 
                  style={{ height: `${summary.lateRate}%` }}
                />
                <span className="chart-label">Late</span>
                <span className="chart-value">{summary.lateRate}%</span>
              </div>
              <div className="chart-bar-item">
                <div 
                  className="chart-bar excused" 
                  style={{ height: `${summary.excusedRate}%` }}
                />
                <span className="chart-label">Excused</span>
                <span className="chart-value">{summary.excusedRate}%</span>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminReports;