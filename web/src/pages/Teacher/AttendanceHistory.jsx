import React, { useState, useEffect } from 'react';
import {
  Search, Filter, ChevronDown, X, Calendar,
  CheckCircle2, XCircle, Clock, FileText,
  Users, BookOpen, Pencil, Trash2, ChevronLeft,
  ChevronRight, AlertCircle, RefreshCw
} from 'lucide-react';
import './AttendanceHistory.css';

const STATUS_CONFIG = {
  present: { label: 'Present', color: '#10B981', bg: '#D1FAE5', border: '#6EE7B7', icon: CheckCircle2 },
  absent:  { label: 'Absent',  color: '#EF4444', bg: '#FEE2E2', border: '#FCA5A5', icon: XCircle },
  late:    { label: 'Late',    color: '#F59E0B', bg: '#FEF3C7', border: '#FCD34D', icon: Clock },
  excused: { label: 'Excused', color: '#6366F1', bg: '#EDE9FE', border: '#C4B5FD', icon: FileText },
};

const PAGE_SIZE = 10;

/**
 * Extracts { grade, section } from a class object.
 * Handles all known backend shapes.
 */
const parseClassObj = (cls) => {
  // Shape 1: separate gradeLevel + section fields from backend
  if (cls.gradeLevel != null && cls.section) {
    return { grade: `Grade ${cls.gradeLevel}`, section: cls.section };
  }
  if (cls.gradeLevel != null) {
    return { grade: `Grade ${cls.gradeLevel}`, section: cls.section || null };
  }
  // Shape 2: section field exists, className is the grade label
  if (cls.section) {
    return { grade: cls.className, section: cls.section };
  }
  // Shape 3: parse "Grade 9Integrity" or "Grade 9 Integrity" from className
  const match = (cls.className || '').match(/^(Grade\s*\d+)\s*(.+)$/i);
  if (match) {
    return { grade: match[1].trim(), section: match[2].trim() };
  }
  // Shape 4: className is just "Grade 9" with no section
  const gradeOnly = (cls.className || '').match(/^(Grade\s*\d+)$/i);
  if (gradeOnly) {
    return { grade: gradeOnly[1].trim(), section: null };
  }
  return { grade: cls.className || 'Unknown', section: null };
};

const AttendanceHistory = () => {
  const [classes, setClasses]             = useState([]);
  const [records, setRecords]             = useState([]);
  const [loading, setLoading]             = useState(false);
  const [classLoading, setClassLoading]   = useState(true);
  const [error, setError]                 = useState('');
  const [page, setPage]                   = useState(1);
  const [totalPages, setTotalPages]       = useState(1);

  // Filters
  const [selectedGrade, setSelectedGrade]     = useState('all');   // "all" | "Grade 9"
  const [selectedSection, setSelectedSection] = useState('all');   // "all" | classId (number)
  const [filterStatus, setFilterStatus]       = useState('all');
  const [searchTerm, setSearchTerm]           = useState('');
  const [dateFrom, setDateFrom]               = useState('');
  const [dateTo, setDateTo]                   = useState('');
  const [showGradeDrop, setShowGradeDrop]     = useState(false);
  const [showSectionDrop, setShowSectionDrop] = useState(false);

  // Edit modal
  const [editRecord, setEditRecord]   = useState(null);
  const [editStatus, setEditStatus]   = useState('');
  const [editSaving, setEditSaving]   = useState(false);

  // Delete confirm
  const [deleteId, setDeleteId]           = useState(null);
  const [deleteConfirm, setDeleteConfirm] = useState(false);

  const token = localStorage.getItem('accessToken');
  const user  = JSON.parse(localStorage.getItem('user') || '{}');

  const [allRecords, setAllRecords] = useState([]);

  // All unique grades — deduplicated and sorted numerically
  const gradeOptions = (() => {
    const seen = new Set();
    const result = [];
    for (const c of classes) {
      const { grade } = parseClassObj(c);
      const key = (grade || '').trim().toLowerCase();
      if (!seen.has(key)) {
        seen.add(key);
        result.push(grade);
      }
    }
    return result.sort((a, b) => {
      const numA = parseInt((a || '').match(/\d+/)?.[0] || '0');
      const numB = parseInt((b || '').match(/\d+/)?.[0] || '0');
      return numA - numB;
    });
  })();

  // Sections belonging to the selected grade (case-insensitive match)
  const sectionOptions = selectedGrade !== 'all'
    ? classes.filter(c => parseClassObj(c).grade?.trim().toLowerCase() === selectedGrade?.trim().toLowerCase())
    : [];

  // ── Grade / Section handlers ──────────────────────────────────────────────
  const handleGradeSelect = (grade) => {
    setSelectedGrade(grade);
    setSelectedSection('all');
    setShowGradeDrop(false);
    setPage(1);
  };

  const handleSectionSelect = (classId) => {
    setSelectedSection(classId);
    setShowSectionDrop(false);
    setPage(1);
  };

  // ── Label helpers ─────────────────────────────────────────────────────────
  const gradeTriggerLabel = selectedGrade === 'all' ? 'All Grades' : selectedGrade;

  const sectionTriggerLabel = (() => {
    if (selectedSection === 'all') return selectedGrade === 'all' ? '— select grade first' : 'All Sections';
    const cls = classes.find(c => c.classId === selectedSection);
    if (!cls) return 'All Sections';
    const { section } = parseClassObj(cls);
    return section || cls.subject || `Class ${cls.classId}`;
  })();

  useEffect(() => { fetchClasses(); }, []);

  useEffect(() => {
    if (classes.length > 0) fetchRecords(classes);
    // eslint-disable-next-line
  }, [selectedGrade, selectedSection, dateFrom, dateTo, classes.length]);

  useEffect(() => {
    applyFilters();
    // eslint-disable-next-line
  }, [allRecords, filterStatus, searchTerm, page]);

  useEffect(() => { setPage(1); }, [selectedGrade, selectedSection, filterStatus, searchTerm, dateFrom, dateTo]);

  const fetchClasses = async () => {
    setClassLoading(true);
    try {
      const teacherId = user.userId || user.id;
      const res  = await fetch(`http://localhost:8888/api/classes/teacher/${teacherId}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = await res.json();
      if (data.success) {
        const loaded = data.data || [];
        setClasses(loaded);
        fetchRecords(loaded);
      }
    } catch { setError('Failed to load classes.'); }
    finally { setClassLoading(false); }
  };

  // Accept classList param to avoid stale closure — falls back to state
  const fetchRecords = async (classList = classes) => {
    if (!classList.length) return;
    setLoading(true);
    setError('');
    try {
      const todayStr = new Date().toISOString().split('T')[0];
      // Default: last 30 days so all recent records are visible
      const thirtyDaysAgo = new Date();
      thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
      const from = dateFrom || thirtyDaysAgo.toISOString().split('T')[0];
      const to   = dateTo   || todayStr;

      const dates = [];
      let cur = new Date(from);
      const end = new Date(to);
      while (cur <= end) {
        dates.push(cur.toISOString().split('T')[0]);
        cur.setDate(cur.getDate() + 1);
      }

      // Resolve which classes to query using the passed-in list (avoids stale closure)
      let classesToQuery = classList;
      if (selectedGrade !== 'all') {
        classesToQuery = classList.filter(c => parseClassObj(c).grade === selectedGrade);
        if (selectedSection !== 'all') {
          classesToQuery = classesToQuery.filter(c => c.classId === selectedSection);
        }
      }

      const fetches = classesToQuery.flatMap(cls =>
        dates.map(date =>
          fetch(`http://localhost:8888/api/attendance/class/${cls.classId}/date/${date}`, {
            headers: { Authorization: `Bearer ${token}` },
          })
          .then(r => r.json())
          .then(d => d.success ? (d.data || []) : [])
          .catch(() => [])
        )
      );

      const results = await Promise.all(fetches);
      let raw = results.flat();

      raw = raw.map(r => {
        // API returns a single "studentName" field e.g. "Trisha Raye Cararag"
        const fullName  = r.studentName || '';
        const parts     = fullName.trim().split(/\s+/);
        const firstName = parts.slice(0, -1).join(' ') || fullName;
        const lastName  = parts.length > 1 ? parts[parts.length - 1] : '';

        return {
          ...r,
          status:           r.status?.toLowerCase() || 'present',
          studentFirstName: firstName,
          studentLastName:  lastName,
        };
      });

      setAllRecords(raw);
    } catch { setError('An error occurred while loading records.'); }
    finally { setLoading(false); }
  };

  const applyFilters = () => {
    let filtered = [...allRecords];

    if (filterStatus !== 'all') filtered = filtered.filter(r => r.status === filterStatus);
    if (searchTerm) {
      const q = searchTerm.toLowerCase();
      filtered = filtered.filter(r => {
        const name = (r.studentName || `${r.studentFirstName} ${r.studentLastName}`).toLowerCase();
        return name.includes(q) || (r.rollNumber || '').toLowerCase().includes(q);
      });
    }

    filtered.sort((a, b) => new Date(b.date) - new Date(a.date));

    const total    = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
    setTotalPages(total);
    const safePage = Math.min(page, total);
    const start    = (safePage - 1) * PAGE_SIZE;
    setRecords(filtered.slice(start, start + PAGE_SIZE));
  };

  const handleEdit = (record) => { setEditRecord(record); setEditStatus(record.status); };

  const handleEditSave = async () => {
    if (!editRecord) return;
    setEditSaving(true);
    try {
      const res  = await fetch(`http://localhost:8888/api/attendance/${editRecord.attendanceId}`, {
        method: 'PUT',
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
        body: JSON.stringify({
          studentId: editRecord.studentId,
          classId:   editRecord.classId,
          date:      editRecord.date,
          status:    editStatus,
          markedById: user.userId || user.id,
        }),
      });
      const data = await res.json();
      if (data.success) {
        setEditRecord(null);
        setAllRecords(prev =>
          prev.map(r => r.attendanceId === editRecord.attendanceId
            ? { ...r, status: editStatus.toLowerCase() } : r)
        );
      } else setError(data.message || 'Failed to update record.');
    } catch { setError('Error updating record.'); }
    finally { setEditSaving(false); }
  };

  const handleDelete = async () => {
    if (!deleteId) return;
    try {
      const res  = await fetch(`http://localhost:8888/api/attendance/${deleteId}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = await res.json();
      if (data.success) {
        setDeleteConfirm(false);
        setDeleteId(null);
        setAllRecords(prev => prev.filter(r => r.attendanceId !== deleteId));
      } else setError(data.message || 'Failed to delete record.');
    } catch { setError('Error deleting record.'); }
  };

  const clearFilters = () => {
    setSelectedGrade('all');
    setSelectedSection('all');
    setFilterStatus('all');
    setSearchTerm('');
    setDateFrom('');
    setDateTo('');
  };

  const hasFilters = selectedGrade !== 'all' || selectedSection !== 'all' ||
    filterStatus !== 'all' || searchTerm || dateFrom || dateTo;

  const formatDate = (d) => {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  };

  return (
    <div className="ah-root">

      {/* ── Page Header ─────────────────────────────── */}
      <div className="ah-page-header">
        <div>
          <h1 className="ah-page-title">Attendance History</h1>
          <p className="ah-page-desc">View, filter, edit, and delete past attendance records</p>
        </div>
        <button className="ah-refresh-btn" onClick={() => fetchRecords(classes)} title="Refresh">
          <RefreshCw size={15} /> Refresh
        </button>
      </div>

      {error && (
        <div className="ah-error">
          <AlertCircle size={15} /> {error}
          <button className="ah-error-close" onClick={() => setError('')}><X size={13} /></button>
        </div>
      )}

      {/* ── Filter Bar ──────────────────────────────── */}
      <div className="ah-filter-card">

        {/* Search */}
        <div className="ah-search-box">
          <Search size={15} className="ah-search-icon" />
          <input
            type="text"
            className="ah-search-input"
            placeholder="Search by student name or roll number…"
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
          />
          {searchTerm && (
            <button className="ah-search-clear" onClick={() => setSearchTerm('')}><X size={12} /></button>
          )}
        </div>

        <div className="ah-filter-row">

          {/* ── Grade Dropdown ── */}
          <div className="ah-filter-group">
            <label className="ah-filter-label"><BookOpen size={13} /> Grade</label>
            <div className="ah-dropdown-root">
              <button
                className={`ah-dropdown-trigger ${showGradeDrop ? 'ah-open' : ''}`}
                onClick={() => { setShowGradeDrop(v => !v); setShowSectionDrop(false); }}
                disabled={classLoading}
              >
                <span>{classLoading ? 'Loading…' : gradeTriggerLabel}</span>
                <ChevronDown size={14} className="ah-chevron" />
              </button>
              {showGradeDrop && (
                <div className="ah-dropdown-menu">
                  <button
                    className={`ah-dropdown-item ${selectedGrade === 'all' ? 'ah-active' : ''}`}
                    onClick={() => handleGradeSelect('all')}
                  >
                    All Grades
                  </button>
                  {gradeOptions.map(grade => (
                    <button
                      key={grade}
                      className={`ah-dropdown-item ${selectedGrade === grade ? 'ah-active' : ''}`}
                      onClick={() => handleGradeSelect(grade)}
                    >
                      {grade}
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* ── Section Dropdown ── */}
          <div className="ah-filter-group">
            <label className="ah-filter-label"><Filter size={13} /> Section</label>
            <div className="ah-dropdown-root">
              <button
                className={`ah-dropdown-trigger ${showSectionDrop ? 'ah-open' : ''}`}
                onClick={() => { if (selectedGrade !== 'all') { setShowSectionDrop(v => !v); setShowGradeDrop(false); } }}
                disabled={selectedGrade === 'all' || classLoading}
                style={selectedGrade === 'all' ? { opacity: 0.5, cursor: 'not-allowed' } : {}}
              >
                <span>{sectionTriggerLabel}</span>
                <ChevronDown size={14} className="ah-chevron" />
              </button>
              {showSectionDrop && (
                <div className="ah-dropdown-menu">
                  <button
                    className={`ah-dropdown-item ${selectedSection === 'all' ? 'ah-active' : ''}`}
                    onClick={() => handleSectionSelect('all')}
                  >
                    All Sections
                  </button>
                  {sectionOptions.map(cls => {
                    const { section } = parseClassObj(cls);
                    const label = section || cls.subject || `Class ${cls.classId}`;
                    return (
                      <button
                        key={cls.classId}
                        className={`ah-dropdown-item ${selectedSection === cls.classId ? 'ah-active' : ''}`}
                        onClick={() => handleSectionSelect(cls.classId)}
                      >
                        <span className="ah-item-name">{label}</span>
                      </button>
                    );
                  })}
                </div>
              )}
            </div>
          </div>

          {/* ── Subject (read-only, derived from selected section) ── */}
          <div className="ah-filter-group">
            <label className="ah-filter-label"><BookOpen size={13} /> Subject</label>
            <div className="ah-subject-display">
              {selectedSection !== 'all'
                ? (classes.find(c => c.classId === selectedSection)?.subject || '—')
                : <span className="ah-subject-placeholder">—</span>
              }
            </div>
          </div>

          {/* Status */}
          <div className="ah-filter-group">
            <label className="ah-filter-label"><Filter size={13} /> Status</label>
            <select
              className="ah-select"
              value={filterStatus}
              onChange={e => setFilterStatus(e.target.value)}
            >
              <option value="all">All Statuses</option>
              {Object.entries(STATUS_CONFIG).map(([key, cfg]) => (
                <option key={key} value={key}>{cfg.label}</option>
              ))}
            </select>
          </div>

          {/* Date range */}
          <div className="ah-filter-group">
            <label className="ah-filter-label"><Calendar size={13} /> From</label>
            <input type="date" className="ah-date-input" value={dateFrom} onChange={e => setDateFrom(e.target.value)} />
          </div>

          <div className="ah-filter-group">
            <label className="ah-filter-label"><Calendar size={13} /> To</label>
            <input type="date" className="ah-date-input" value={dateTo} onChange={e => setDateTo(e.target.value)} />
          </div>

          {hasFilters && (
            <button className="ah-clear-btn" onClick={clearFilters}><X size={13} /> Clear</button>
          )}
        </div>
      </div>

      {/* ── Table Panel ─────────────────────────────── */}
      <div className="ah-table-panel">
        {loading ? (
          <div className="ah-loading"><div className="ah-spinner" /><p>Loading records…</p></div>
        ) : records.length === 0 ? (
          <div className="ah-empty">
            <div className="ah-empty-icon"><Users size={38} /></div>
            <h3>{hasFilters ? 'No records match your filters.' : 'No attendance records yet.'}</h3>
            <p>{hasFilters ? 'Try adjusting your search or filters.' : 'Start by taking attendance for your classes.'}</p>
            {hasFilters && <button className="ah-clear-btn-lg" onClick={clearFilters}><X size={14} /> Clear Filters</button>}
          </div>
        ) : (
          <>
            <div className="ah-table-wrap">
              <table className="ah-table">
                <thead>
                  <tr>
                    <th>Student</th>
                    <th>Roll No.</th>
                    <th>Class</th>
                    <th>Date</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {records.map((rec, idx) => {
                    const cfg  = STATUS_CONFIG[rec.status] || STATUS_CONFIG.present;
                    const Icon = cfg.icon;
                    return (
                      <tr key={rec.attendanceId} style={{ animationDelay: `${idx * 25}ms` }}>
                        <td>
                          <div className="ah-student-cell">
                            <div className="ah-avatar">
                              {((rec.studentFirstName?.[0] || '') + (rec.studentLastName?.[0] || '')).toUpperCase() || '?'}
                            </div>
                            <span className="ah-student-name">{rec.studentName || `${rec.studentFirstName} ${rec.studentLastName}`}</span>
                          </div>
                        </td>
                        <td className="ah-roll">{rec.rollNumber || '—'}</td>
                        <td className="ah-class">{rec.className || '—'}</td>
                        <td className="ah-date">{formatDate(rec.date)}</td>
                        <td>
                          <span className="ah-status-badge"
                            style={{ '--s-color': cfg.color, '--s-bg': cfg.bg, '--s-border': cfg.border }}>
                            <Icon size={12} />{cfg.label}
                          </span>
                        </td>
                        <td>
                          <div className="ah-actions">
                            <button className="ah-action-btn" title="Edit" onClick={() => handleEdit(rec)}>
                              <Pencil size={14} color="#0F2D5E" />
                            </button>
                            <button className="ah-action-btn ah-delete" title="Delete"
                              onClick={() => { setDeleteId(rec.attendanceId); setDeleteConfirm(true); }}>
                              <Trash2 size={14} color="#EF4444" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            <div className="ah-pagination">
              <span className="ah-page-info">Page <strong>{page}</strong> of <strong>{totalPages}</strong></span>
              <div className="ah-page-btns">
                <button className="ah-page-btn" onClick={() => setPage(p => Math.max(1, p - 1))} disabled={page === 1}>
                  <ChevronLeft size={15} /> Prev
                </button>
                {Array.from({ length: totalPages }, (_, i) => i + 1)
                  .filter(p => p === 1 || p === totalPages || Math.abs(p - page) <= 1)
                  .reduce((acc, p, idx, arr) => {
                    if (idx > 0 && p - arr[idx - 1] > 1) acc.push('…');
                    acc.push(p);
                    return acc;
                  }, [])
                  .map((p, i) =>
                    p === '…'
                      ? <span key={`e-${i}`} className="ah-page-ellipsis">…</span>
                      : <button key={p} className={`ah-page-num ${page === p ? 'ah-page-active' : ''}`}
                          onClick={() => setPage(p)}>{p}</button>
                  )
                }
                <button className="ah-page-btn" onClick={() => setPage(p => Math.min(totalPages, p + 1))} disabled={page === totalPages}>
                  Next <ChevronRight size={15} />
                </button>
              </div>
            </div>
          </>
        )}
      </div>

      {/* ── Edit Modal ──────────────────────────────── */}
      {editRecord && (
        <div className="ah-modal-overlay" onClick={() => setEditRecord(null)}>
          <div className="ah-modal" onClick={e => e.stopPropagation()}>
            <div className="ah-modal-header">
              <h3>Edit Attendance Record</h3>
              <button className="ah-modal-close" onClick={() => setEditRecord(null)}><X size={15} /></button>
            </div>
            <div className="ah-modal-body">
              <div className="ah-modal-info-row">
                <div className="ah-modal-avatar">
                  {((editRecord.studentFirstName?.[0] || '') + (editRecord.studentLastName?.[0] || '')).toUpperCase()}
                </div>
                <div>
                  <div className="ah-modal-student-name">{editRecord.studentName || `${editRecord.studentFirstName} ${editRecord.studentLastName}`}</div>
                  <div className="ah-modal-meta">{editRecord.className} · {formatDate(editRecord.date)}</div>
                </div>
              </div>
              <div className="ah-modal-field">
                <label className="ah-modal-label">Attendance Status</label>
                <div className="ah-status-picker">
                  {Object.entries(STATUS_CONFIG).map(([key, cfg]) => {
                    const Icon = cfg.icon;
                    return (
                      <button key={key}
                        className={`ah-status-option ${editStatus === key ? 'ah-status-selected' : ''}`}
                        style={{ '--opt-color': cfg.color, '--opt-bg': cfg.bg, '--opt-border': cfg.border }}
                        onClick={() => setEditStatus(key)}
                      >
                        <Icon size={18} />{cfg.label}
                      </button>
                    );
                  })}
                </div>
              </div>
            </div>
            <div className="ah-modal-footer">
              <button className="ah-btn-outline" onClick={() => setEditRecord(null)}>Cancel</button>
              <button className="ah-btn-primary" onClick={handleEditSave} disabled={editSaving}>
                {editSaving ? <><span className="ah-btn-spinner" /> Saving…</> : 'Update Record'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── Delete Confirm Modal ─────────────────────── */}
      {deleteConfirm && (
        <div className="ah-modal-overlay" onClick={() => { setDeleteConfirm(false); setDeleteId(null); }}>
          <div className="ah-modal ah-modal-sm" onClick={e => e.stopPropagation()}>
            <div className="ah-modal-header">
              <h3>Delete Record</h3>
              <button className="ah-modal-close" onClick={() => { setDeleteConfirm(false); setDeleteId(null); }}><X size={15} /></button>
            </div>
            <div className="ah-modal-body">
              <div className="ah-delete-warning">
                <XCircle size={36} color="#EF4444" />
                <p>Are you sure you want to delete this attendance record? This action <strong>cannot be undone</strong>.</p>
              </div>
            </div>
            <div className="ah-modal-footer">
              <button className="ah-btn-outline" onClick={() => { setDeleteConfirm(false); setDeleteId(null); }}>Cancel</button>
              <button className="ah-btn-danger" onClick={handleDelete}>Delete Record</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AttendanceHistory;