import React, { useState, useEffect } from 'react';
import {
  ChevronDown, Search, CheckCircle2, XCircle,
  Clock, FileText, Save, Users, X, Filter,
  AlertCircle, CheckCheck
} from 'lucide-react';
import { classService } from '../../services/classService';
import api from '../../services/api';
import './TakeAttendance.css';

const STATUS_CONFIG = {
  present:  { label: 'Present',  color: '#10B981', bg: '#D1FAE5', border: '#6EE7B7', icon: CheckCircle2 },
  absent:   { label: 'Absent',   color: '#EF4444', bg: '#FEE2E2', border: '#FCA5A5', icon: XCircle },
  late:     { label: 'Late',     color: '#F59E0B', bg: '#FEF3C7', border: '#FCD34D', icon: Clock },
  excused:  { label: 'Excused',  color: '#6366F1', bg: '#EDE9FE', border: '#C4B5FD', icon: FileText },
};

const parseClassObj = (cls) => {
  if (cls.gradeLevel && cls.section) {
    return { grade: `Grade ${cls.gradeLevel}`.trim(), section: cls.section.trim() };
  }
  if (cls.gradeLevel) {
    return { grade: `Grade ${cls.gradeLevel}`.trim(), section: cls.section?.trim() || null };
  }
  if (cls.section) {
    return { grade: (cls.className || '').trim(), section: cls.section.trim() };
  }
  const match = (cls.className || '').match(/^(Grade\s*\d+)\s*(.*)$/i);
  if (match) {
    return { grade: match[1].trim(), section: match[2].trim() || null };
  }
  return { grade: (cls.className || '').trim(), section: null };
};

const TakeAttendance = () => {
  const [classes, setClasses]                           = useState([]);
  const [selectedClass, setSelectedClass]               = useState(null);
  const [selectedGrade, setSelectedGrade]               = useState(null);
  const [selectedSection, setSelectedSection]           = useState(null);
  const [selectedSubject, setSelectedSubject]           = useState(null);
  const [students, setStudents]                         = useState([]);
  const [attendance, setAttendance]                     = useState({});
  const [searchTerm, setSearchTerm]                     = useState('');
  const [filterStatus, setFilterStatus]                 = useState('all');
  const [loading, setLoading]                           = useState(false);
  const [classLoading, setClassLoading]                 = useState(true);
  const [saving, setSaving]                             = useState(false);
  const [saved, setSaved]                               = useState(false);
  const [error, setError]                               = useState('');
  const [date, setDate]                                 = useState(new Date().toISOString().split('T')[0]);
  const [showGradeDropdown, setShowGradeDropdown]       = useState(false);
  const [showSectionDropdown, setShowSectionDropdown]   = useState(false);
  const [showSubjectDropdown, setShowSubjectDropdown]   = useState(false);

  const gradeOptions = [...new Set(
    classes.map(c => parseClassObj(c).grade)
  )].sort((a, b) => {
    const numA = parseInt((a || '').match(/\d+/)?.[0] || '0');
    const numB = parseInt((b || '').match(/\d+/)?.[0] || '0');
    return numA - numB;
  });

  const sectionOptions = selectedGrade
    ? classes.filter(c => parseClassObj(c).grade === selectedGrade)
    : [];

  const subjectOptions = selectedSection
    ? [...new Set(
        classes
          .filter(c => parseClassObj(c).grade === selectedGrade && parseClassObj(c).section === selectedSection)
          .map(c => c.subject)
          .filter(Boolean)
      )]
    : [];

  const handleGradeSelect = (grade) => {
    setSelectedGrade(grade);
    setSelectedSection(null);
    setSelectedClass(null);
    setSelectedSubject(null);
    setStudents([]);
    setAttendance({});
    setSaved(false);
    setShowGradeDropdown(false);
  };

  const handleSectionSelect = (cls) => {
    const { section } = parseClassObj(cls);
    setSelectedSection(section);
    setSelectedClass(cls);
    setSelectedSubject(null);
    setSaved(false);
    setShowSectionDropdown(false);
  };

  const handleSubjectSelect = (subject) => {
    setSelectedSubject(subject);
    setShowSubjectDropdown(false);
  };

  useEffect(() => { fetchClasses(); }, []);

  useEffect(() => {
    if (selectedClass) fetchStudents(selectedClass.classId);
  }, [selectedClass, date]);

  const fetchClasses = async () => {
    setClassLoading(true);
    try {
      const user = JSON.parse(localStorage.getItem('user') || '{}');
      const teacherId = user.userId || user.id;
      const data = await classService.getClassesByTeacher(teacherId);
      if (data.success) setClasses(data.data || []);
    } catch {
      setError('Failed to load classes.');
    } finally {
      setClassLoading(false);
    }
  };

  const fetchStudents = async (classId) => {
    setLoading(true);
    setError('');
    setSaved(false);
    try {
      const studData = await classService.getStudentsInClass(classId);
      const list     = studData.data || [];
      setStudents(list);

      const attRes  = await api.get(`/attendance/class/${classId}/date/${date}`);
      const attData = attRes.data;
      const existing = {};
      (attData.data || []).forEach(a => { existing[a.studentId] = a.status?.toLowerCase(); });

      const init = {};
      list.forEach(s => {
        const s_ = existing[s.studentId];
        init[s.studentId] = STATUS_CONFIG[s_] ? s_ : 'present';
      });
      setAttendance(init);
    } catch {
      setError('Failed to load students.');
    } finally {
      setLoading(false);
    }
  };

  const setStatus = (studentId, status) => {
    setAttendance(prev => ({ ...prev, [studentId]: status.toLowerCase() }));
    setSaved(false);
  };

  const markAll = (status) => {
    const next = {};
    students.forEach(s => { next[s.studentId] = status; });
    setAttendance(next);
    setSaved(false);
  };

  const handleSave = async () => {
    if (!selectedClass) return;
    setSaving(true);
    setError('');
    try {
      const user = JSON.parse(localStorage.getItem('user') || '{}');
      const attendanceData = {};
      students.forEach(s => {
        attendanceData[s.studentId] = attendance[s.studentId] || 'present';
      });
      const payload = {
        classId:    selectedClass.classId,
        date,
        attendanceData,
        markedById: user.userId || user.id,
        remarks:    '',
      };
      const res  = await api.post('/attendance/bulk', payload);
      const data = res.data;
      if (data.success) { setSaved(true); }
      else setError(data.message || 'Failed to save attendance.');
    } catch {
      setError('An error occurred while saving.');
    } finally {
      setSaving(false);
    }
  };

  const counts = Object.values(attendance).reduce((acc, s) => {
    const key  = (s || 'present').toLowerCase();
    const safe = STATUS_CONFIG[key] ? key : 'present';
    acc[safe]  = (acc[safe] || 0) + 1;
    return acc;
  }, {});

  const filteredStudents = students.filter(s => {
    const name        = `${s.firstName} ${s.lastName}`.toLowerCase();
    const matchSearch = !searchTerm || name.includes(searchTerm.toLowerCase()) ||
      s.rollNumber?.toLowerCase().includes(searchTerm.toLowerCase());
    const matchFilter = filterStatus === 'all' || attendance[s.studentId] === filterStatus;
    return matchSearch && matchFilter;
  }).sort((a, b) => {
    const lastCmp = (a.lastName || '').localeCompare(b.lastName || '');
    if (lastCmp !== 0) return lastCmp;
    return (a.firstName || '').localeCompare(b.firstName || '');
  });

  const todayLabel = new Date().toLocaleDateString('en-US', {
    weekday: 'long', month: 'long', day: 'numeric', year: 'numeric'
  });

  return (
    <div className="ta-root">
      <div className="ta-page-header">
        <div className="ta-header-left">
          <h1 className="ta-page-title">Take Attendance</h1>
          <p className="ta-page-desc">Select a class and mark each student's attendance status</p>
        </div>
      </div>

      {error && (
        <div className="ta-error">
          <AlertCircle size={16} /> {error}
          <button className="ta-error-close" onClick={() => setError('')}><X size={14} /></button>
        </div>
      )}

      <div className="ta-controls-row">

        {/* Grade Dropdown */}
        <div className="ta-selector-wrap">
          <label className="ta-control-label">Grade</label>
          <div className="ta-dropdown-root">
            <button
              className={`ta-dropdown-trigger ${showGradeDropdown ? 'ta-dropdown-open' : ''}`}
              onClick={() => {
                setShowGradeDropdown(v => !v);
                setShowSectionDropdown(false);
                setShowSubjectDropdown(false);
              }}
              disabled={classLoading}
            >
              <Users size={16} className="ta-trigger-icon" />
              <span>{selectedGrade ?? (classLoading ? 'Loading…' : 'Select a grade')}</span>
              <ChevronDown size={16} className="ta-chevron" />
            </button>
            {showGradeDropdown && (
              <div className="ta-dropdown-menu">
                {gradeOptions.length === 0
                  ? <div className="ta-dropdown-empty">No grades found</div>
                  : gradeOptions.map(grade => (
                    <button
                      key={grade}
                      className={`ta-dropdown-item ${selectedGrade === grade ? 'ta-dropdown-active' : ''}`}
                      onClick={() => handleGradeSelect(grade)}
                    >
                      <div className="ta-dropdown-item-name">{grade}</div>
                    </button>
                  ))
                }
              </div>
            )}
          </div>
        </div>

        {/* Section Dropdown */}
        <div className="ta-selector-wrap">
          <label className="ta-control-label">Section</label>
          <div className="ta-dropdown-root">
            <button
              className={`ta-dropdown-trigger ${showSectionDropdown ? 'ta-dropdown-open' : ''}`}
              onClick={() => {
                if (selectedGrade) {
                  setShowSectionDropdown(v => !v);
                  setShowGradeDropdown(false);
                  setShowSubjectDropdown(false);
                }
              }}
              disabled={!selectedGrade || classLoading}
              style={!selectedGrade ? { opacity: 0.5, cursor: 'not-allowed' } : {}}
            >
              <ChevronDown size={16} className="ta-trigger-icon" style={{ opacity: 0.5 }} />
              <span>{selectedSection ?? (selectedGrade ? 'Select a section' : '— select grade first')}</span>
              <ChevronDown size={16} className="ta-chevron" />
            </button>
            {showSectionDropdown && (
              <div className="ta-dropdown-menu">
                {sectionOptions.length === 0
                  ? <div className="ta-dropdown-empty">No sections found</div>
                  : sectionOptions.map(cls => {
                    const { section } = parseClassObj(cls);
                    return (
                      <button
                        key={cls.classId}
                        className={`ta-dropdown-item ${selectedClass?.classId === cls.classId ? 'ta-dropdown-active' : ''}`}
                        onClick={() => handleSectionSelect(cls)}
                      >
                        <div className="ta-dropdown-item-name">{section || cls.className}</div>
                        <div className="ta-dropdown-item-meta">{cls.studentCount || 0} students</div>
                      </button>
                    );
                  })
                }
              </div>
            )}
          </div>
        </div>

        {/* Subject Dropdown */}
        <div className="ta-selector-wrap">
          <label className="ta-control-label">Subject</label>
          <div className="ta-dropdown-root">
            <button
              className={`ta-dropdown-trigger ${showSubjectDropdown ? 'ta-dropdown-open' : ''}`}
              onClick={() => {
                if (selectedClass) {
                  setShowSubjectDropdown(v => !v);
                  setShowGradeDropdown(false);
                  setShowSectionDropdown(false);
                }
              }}
              disabled={!selectedClass || classLoading}
              style={!selectedClass ? { opacity: 0.5, cursor: 'not-allowed' } : {}}
            >
              <FileText size={16} className="ta-trigger-icon" />
              <span>{selectedSubject ?? (selectedClass ? 'Select a subject' : '— select section first')}</span>
              <ChevronDown size={16} className="ta-chevron" />
            </button>
            {showSubjectDropdown && (
              <div className="ta-dropdown-menu">
                {subjectOptions.length === 0
                  ? <div className="ta-dropdown-empty">No subjects found</div>
                  : subjectOptions.map(subject => (
                    <button
                      key={subject}
                      className={`ta-dropdown-item ${selectedSubject === subject ? 'ta-dropdown-active' : ''}`}
                      onClick={() => handleSubjectSelect(subject)}
                    >
                      <div className="ta-dropdown-item-name">{subject}</div>
                    </button>
                  ))
                }
              </div>
            )}
          </div>
        </div>

        {/* Date Picker */}
        <div className="ta-selector-wrap">
          <label className="ta-control-label">Date</label>
          <input
            type="date"
            className="ta-date-input"
            value={date}
            onChange={e => { setDate(e.target.value); setSaved(false); }}
          />
        </div>

      </div>

      {selectedClass && !loading && students.length > 0 && (
        <div className="ta-summary-strip">
          {Object.entries(STATUS_CONFIG).map(([key, cfg]) => {
            const Icon = cfg.icon;
            return (
              <div key={key} className="ta-summary-chip"
                style={{ '--chip-color': cfg.color, '--chip-bg': cfg.bg, '--chip-border': cfg.border }}>
                <Icon size={14} />
                <span className="ta-chip-count">{counts[key] || 0}</span>
                <span className="ta-chip-label">{cfg.label}</span>
              </div>
            );
          })}
          <div className="ta-summary-chip ta-chip-total">
            <Users size={14} />
            <span className="ta-chip-count">{students.length}</span>
            <span className="ta-chip-label">Total</span>
          </div>
        </div>
      )}

      {!selectedClass ? (
        <div className="ta-placeholder">
          <div className="ta-placeholder-icon"><Users size={40} /></div>
          <h3>Select a Class to Begin</h3>
          <p>Choose a grade from the dropdown above to load students and start marking attendance.</p>
        </div>
      ) : loading ? (
        <div className="ta-loading">
          <div className="ta-spinner" />
          <p>Loading students…</p>
        </div>
      ) : students.length === 0 ? (
        <div className="ta-placeholder">
          <div className="ta-placeholder-icon"><Users size={40} /></div>
          <h3>No Students Enrolled</h3>
          <p>This class has no students yet. Add students from the Manage Classes page.</p>
        </div>
      ) : (
        <div className="ta-list-panel">
          <div className="ta-list-toolbar">
            <div className="ta-search-box">
              <Search size={15} className="ta-search-icon" />
              <input
                type="text"
                className="ta-search-input"
                placeholder="Search by name or roll number…"
                value={searchTerm}
                onChange={e => setSearchTerm(e.target.value)}
              />
              {searchTerm && (
                <button className="ta-search-clear" onClick={() => setSearchTerm('')}><X size={12} /></button>
              )}
            </div>
            <div className="ta-toolbar-divider" />
            <div className="ta-filter-wrap">
              <Filter size={14} className="ta-filter-icon" />
              <select
                className="ta-filter-select"
                value={filterStatus}
                onChange={e => setFilterStatus(e.target.value)}
              >
                <option value="all">All Students</option>
                {Object.entries(STATUS_CONFIG).map(([key, cfg]) => (
                  <option key={key} value={key}>{cfg.label}</option>
                ))}
              </select>
            </div>
            <div className="ta-toolbar-divider" />
            <div className="ta-mark-all-wrap">
              <span className="ta-mark-all-label">Mark all:</span>
              {Object.entries(STATUS_CONFIG).map(([key, cfg]) => (
                <button
                  key={key}
                  className="ta-mark-all-btn"
                  style={{ '--btn-color': cfg.color, '--btn-bg': cfg.bg, '--btn-border': cfg.border }}
                  onClick={() => markAll(key)}
                  title={`Mark all as ${cfg.label}`}
                >
                  {cfg.label}
                </button>
              ))}
            </div>
          </div>

          <div className="ta-results-line">
            Showing <strong>{filteredStudents.length}</strong> of <strong>{students.length}</strong> students
          </div>

          <div className="ta-students-list">
            {filteredStudents.length === 0 ? (
              <div className="ta-no-results">No students match your search.</div>
            ) : filteredStudents.map((student, idx) => {
              const currentStatus = (attendance[student.studentId] || 'present').toLowerCase();
              const cfg = STATUS_CONFIG[currentStatus] || STATUS_CONFIG['present'];
              return (
                <div key={student.studentId} className="ta-student-row" style={{ animationDelay: `${idx * 30}ms` }}>
                  <div className="ta-avatar">
                    {((student.firstName?.[0] || '') + (student.lastName?.[0] || '')).toUpperCase() || '?'}
                  </div>
                  <div className="ta-student-info">
                    <span className="ta-student-name">{student.firstName} {student.lastName}</span>
                    <span className="ta-student-roll">{student.rollNumber}</span>
                  </div>
                  <div className="ta-status-btns">
                    {Object.entries(STATUS_CONFIG).map(([key, scfg]) => {
                      const Icon     = scfg.icon;
                      const isActive = currentStatus === key;
                      return (
                        <button
                          key={key}
                          className={`ta-status-btn ${isActive ? 'ta-status-active' : ''}`}
                          style={isActive ? { '--sb-color': scfg.color, '--sb-bg': scfg.bg, '--sb-border': scfg.border } : {}}
                          onClick={() => setStatus(student.studentId, key)}
                          title={scfg.label}
                        >
                          <Icon size={15} />
                          <span>{scfg.label}</span>
                        </button>
                      );
                    })}
                  </div>
                  <div className="ta-current-badge" style={{ '--cb-color': cfg.color, '--cb-bg': cfg.bg, '--cb-border': cfg.border }}>
                    {cfg.label}
                  </div>
                </div>
              );
            })}
          </div>

          <div className="ta-list-footer">
            <span className="ta-footer-info">
              {todayLabel} · {selectedGrade}
              {selectedSection && ` — ${selectedSection}`}
              {selectedSubject && ` · ${selectedSubject}`}
            </span>
            <button
              className={`ta-save-btn ${saved ? 'ta-saved' : ''}`}
              onClick={handleSave}
              disabled={saving}
            >
              {saved
                ? <><CheckCheck size={17} /> Saved!</>
                : saving
                  ? <><span className="ta-btn-spinner" /> Saving…</>
                  : <><Save size={17} /> Save Attendance</>
              }
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default TakeAttendance;