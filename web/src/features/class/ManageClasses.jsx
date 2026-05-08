import React, { useState, useEffect } from 'react';
import { classService } from '../class/classService';
import { userService } from '../user/userService';
import { studentService } from '../../shared/studentService';
import {
  School, GraduationCap, Users, Search, Clock,
  Pencil, Trash2, X, Plus, UserCircle2
} from 'lucide-react';
import './ManageClasses.css';

const ManageClasses = () => {
  const [classes, setClasses] = useState([]);
  const [filteredClasses, setFilteredClasses] = useState([]);
  const [teachers, setTeachers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editingClass, setEditingClass] = useState(null);
  const [showStudentsModal, setShowStudentsModal] = useState(false);
  const [selectedClass, setSelectedClass] = useState(null);
  const [classStudents, setClassStudents] = useState([]);

  // Search and Filter states
  const [searchTerm, setSearchTerm] = useState('');
  const [filterTeacher, setFilterTeacher] = useState('all');
  const [filterYear, setFilterYear] = useState('all');

  // Add Student Modal state
  const [showAddStudentModal, setShowAddStudentModal] = useState(false);
  const [studentForm, setStudentForm] = useState({
    firstName: '',
    lastName: '',
    rollNumber: '',
    email: '',
    phone: '',
  });
  const [studentError, setStudentError] = useState('');
  const [studentLoading, setStudentLoading] = useState(false);

  const [formData, setFormData] = useState({
    className: '',
    subject: '',
    section: '',
    academicYear: '',
    teacherId: '',
    scheduleDay: '',
    scheduleTime: '',
    scheduleTimeEnd: '',
  });

  const currentYear = new Date().getFullYear();
  const nextYear = currentYear + 1;
  const defaultAcademicYear = `${currentYear}-${nextYear}`;

  useEffect(() => { fetchData(); }, []);
  useEffect(() => { filterClasses(); }, [classes, searchTerm, filterTeacher, filterYear]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [classesRes, teachersRes] = await Promise.all([
        classService.getAllClasses(),
        userService.getUsersByRole('TEACHER')
      ]);
      if (classesRes.success) { setClasses(classesRes.data); setFilteredClasses(classesRes.data); }
      if (teachersRes.success) setTeachers(teachersRes.data);
    } catch (err) {
      setError('Failed to load data');
    } finally {
      setLoading(false);
    }
  };

  const filterClasses = () => {
    let filtered = [...classes];
    if (searchTerm) {
      filtered = filtered.filter(cls =>
        cls.className?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        cls.subject?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        cls.section?.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }
    if (filterTeacher !== 'all') filtered = filtered.filter(cls => cls.teacherId === parseInt(filterTeacher));
    if (filterYear !== 'all') filtered = filtered.filter(cls => cls.academicYear === filterYear);
    setFilteredClasses(filtered);
  };

  const clearFilters = () => { setSearchTerm(''); setFilterTeacher('all'); setFilterYear('all'); };
  const academicYears = [...new Set(classes.map(cls => cls.academicYear).filter(Boolean))];
  const totalStudents = classes.reduce((acc, cls) => acc + (cls.studentCount || 0), 0);

  const formatTime = (time) => {
    if (!time) return '';
    try {
      return new Date('1970-01-01T' + time).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch { return time; }
  };

  const handleOpenModal = (classItem = null) => {
    if (classItem) {
      setEditingClass(classItem);
      setFormData({
        className: classItem.className,
        subject: classItem.subject,
        section: classItem.section || '',
        academicYear: classItem.academicYear || defaultAcademicYear,
        teacherId: classItem.teacherId || '',
        scheduleDay: classItem.scheduleDay || '',
        scheduleTime: classItem.scheduleTime || '',
        scheduleTimeEnd: classItem.scheduleTimeEnd || '',
      });
    } else {
      setEditingClass(null);
      setFormData({
        className: '', subject: '', section: '',
        academicYear: defaultAcademicYear, teacherId: '',
        scheduleDay: '', scheduleTime: '', scheduleTimeEnd: '',
      });
    }
    setShowModal(true);
    setError('');
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setEditingClass(null);
    setFormData({
      className: '', subject: '', section: '',
      academicYear: defaultAcademicYear, teacherId: '',
      scheduleDay: '', scheduleTime: '', scheduleTimeEnd: '',
    });
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      if (!formData.className || !formData.subject || !formData.teacherId)
        throw new Error('Please fill in all required fields');

      const classData = {
        className: formData.className,
        subject: formData.subject,
        section: formData.section,
        academicYear: formData.academicYear,
        teacherId: parseInt(formData.teacherId),
        scheduleDay: formData.scheduleDay,
        scheduleTime: formData.scheduleTime,
        scheduleTimeEnd: formData.scheduleTimeEnd,
      };

      const response = editingClass
        ? await classService.updateClass(editingClass.classId, classData)
        : await classService.createClass(classData);

      if (response.success) { handleCloseModal(); fetchData(); }
      else setError(response.message || 'Failed to save class');
    } catch (err) {
      setError(err.message || 'An error occurred');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteClass = async (classId, className) => {
    if (!window.confirm(`Are you sure you want to delete ${className}?`)) return;
    setLoading(true);
    try {
      const response = await classService.deleteClass(classId);
      if (response.success) fetchData();
      else setError('Failed to delete class');
    } catch { setError('Error deleting class'); } finally { setLoading(false); }
  };

  const handleViewStudents = async (classItem) => {
    setSelectedClass(classItem);
    setLoading(true);
    try {
      const response = await classService.getStudentsInClass(classItem.classId);
      if (response.success) { setClassStudents(response.data); setShowStudentsModal(true); }
      else setError('Failed to load students');
    } catch { setError('Error loading students'); } finally { setLoading(false); }
  };

  const handleOpenAddStudent = () => {
    setStudentForm({ firstName: '', lastName: '', rollNumber: '', email: '', phone: '' });
    setStudentError('');
    setShowAddStudentModal(true);
  };
  const handleCloseAddStudent = () => { setShowAddStudentModal(false); setStudentError(''); };

  const handleStudentInputChange = (e) => {
    const { name, value } = e.target;
    setStudentForm(prev => ({ ...prev, [name]: value }));
  };

  const handleAddStudentSubmit = async (e) => {
    e.preventDefault();
    setStudentError('');
    setStudentLoading(true);
    try {
      if (!studentForm.firstName || !studentForm.lastName || !studentForm.rollNumber)
        throw new Error('First name, last name, and roll number are required.');
      const payload = {
        firstName: studentForm.firstName, lastName: studentForm.lastName,
        rollNumber: studentForm.rollNumber, email: studentForm.email || null,
        phone: studentForm.phone || null, classId: selectedClass.classId
      };
      const response = await studentService.createStudent(payload);
      if (response.success) {
        const updated = await classService.getStudentsInClass(selectedClass.classId);
        if (updated.success) setClassStudents(updated.data);
        fetchData();
        handleCloseAddStudent();
      } else { setStudentError(response.message || 'Failed to add student'); }
    } catch (err) { setStudentError(err.message || 'An error occurred'); } finally { setStudentLoading(false); }
  };

  const handleRemoveStudent = async (studentId) => {
    if (!window.confirm('Remove this student from the class?')) return;
    try {
      const response = await studentService.removeFromClass(studentId);
      if (response.success) { setClassStudents(prev => prev.filter(s => s.studentId !== studentId)); fetchData(); }
    } catch { setError('Error removing student'); }
  };

  const getTeacherName = (teacherId) => {
    const teacher = teachers.find(t => t.userId === teacherId);
    return teacher ? teacher.fullName : 'Unassigned';
  };

  if (loading && classes.length === 0) return <div className="loading">Loading classes...</div>;

  return (
    <div className="manage-classes">

      {/* Page Header */}
      <div className="page-header">
        <div className="header-left">
          <h1>Manage Classes</h1>
          <p className="page-description">Create and organize classes, assign teachers, and manage student enrollment</p>
        </div>
        <button className="btn-primary" onClick={() => handleOpenModal()}>
          <Plus size={16} /> New Class
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      {/* Stats */}
      <div className="stats-summary">
        <div className="stat-summary-item">
          <div className="stat-icon-wrap blue">
            <School size={22} color="#0F2D5E" strokeWidth={2} />
          </div>
          <div>
            <span className="stat-summary-value">{classes.length}</span>
            <span className="stat-summary-label">Total Classes</span>
          </div>
        </div>
        <div className="stat-summary-item">
          <div className="stat-icon-wrap green">
            <UserCircle2 size={22} color="#0F2D5E" strokeWidth={2} />
          </div>
          <div>
            <span className="stat-summary-value">{teachers.length}</span>
            <span className="stat-summary-label">Active Teachers</span>
          </div>
        </div>
        <div className="stat-summary-item">
          <div className="stat-icon-wrap purple">
            <GraduationCap size={22} color="#0F2D5E" strokeWidth={2} />
          </div>
          <div>
            <span className="stat-summary-value">{totalStudents}</span>
            <span className="stat-summary-label">Total Students</span>
          </div>
        </div>
      </div>

      {/* Search & Filter Toolbar */}
      <div className="search-filter-bar">
        <div className="search-box">
          <span className="search-icon"><Search size={16} color="#64748B" /></span>
          <input
            type="text"
            placeholder="Search classes by name, subject, or section..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="search-input"
          />
          {searchTerm && <button className="clear-search" onClick={() => setSearchTerm('')}><X size={12} /></button>}
        </div>
        <div className="toolbar-divider" />
        <select className="filter-select" value={filterTeacher} onChange={(e) => setFilterTeacher(e.target.value)}>
          <option value="all">All Teachers</option>
          {teachers.map(teacher => <option key={teacher.userId} value={teacher.userId}>{teacher.fullName}</option>)}
        </select>
        <div className="toolbar-divider" />
        <select className="filter-select" value={filterYear} onChange={(e) => setFilterYear(e.target.value)}>
          <option value="all">All Years</option>
          {academicYears.map(year => <option key={year} value={year}>{year}</option>)}
        </select>
        {(searchTerm || filterTeacher !== 'all' || filterYear !== 'all') && (
          <button className="clear-filters-btn" onClick={clearFilters}>Clear</button>
        )}
      </div>

      <div className="results-count">
        Showing <strong>{filteredClasses.length}</strong> of <strong>{classes.length}</strong> classes
      </div>

      {/* Classes Grid */}
      <div className="classes-grid">
        {filteredClasses.length === 0 ? (
          <div className="empty-state">
            <p>No classes match your filters. Try adjusting your search criteria.</p>
            <button className="btn-outline" onClick={clearFilters}>Clear Filters</button>
          </div>
        ) : (
          filteredClasses.map(cls => (
            <div key={cls.classId} className="class-card">
              <div className="class-card-header">
                <h3>{cls.className}</h3>
                <span className="class-badge">{cls.section || 'No Section'}</span>
              </div>
              <div className="class-details">
                <div className="detail-row">
                  <span className="detail-label">Subject:</span>
                  <span className="detail-value">{cls.subject}</span>
                </div>
                <div className="detail-row">
                  <span className="detail-label">Schedule:</span>
                  <span className="detail-value schedule-value">
                    {cls.scheduleDay || cls.scheduleTime
                      ? <>
                          {cls.scheduleDay && <span className="schedule-day">{cls.scheduleDay}</span>}
                          {cls.scheduleTime && (
                            <span className="schedule-time">
                              <Clock size={12} />
                              {formatTime(cls.scheduleTime)}
                              {cls.scheduleTimeEnd && <> – {formatTime(cls.scheduleTimeEnd)}</>}
                            </span>
                          )}
                        </>
                      : <span style={{ color: 'var(--muted)', fontStyle: 'italic', background: 'none' }}>Not set</span>
                    }
                  </span>
                </div>
                <div className="detail-row">
                  <span className="detail-label">Academic Year:</span>
                  <span className="detail-value">{cls.academicYear || 'N/A'}</span>
                </div>
                <div className="detail-row">
                  <span className="detail-label">Teacher:</span>
                  <span className="detail-value teacher-name"><UserCircle2 size={14} /> {getTeacherName(cls.teacherId)}</span>
                </div>
                <div className="detail-row">
                  <span className="detail-label">Students:</span>
                  <span className="detail-value student-count">{cls.studentCount || 0} enrolled</span>
                </div>
              </div>
              <div className="class-actions">
                <button className="btn-icon" onClick={() => handleViewStudents(cls)} title="View Students"><Users size={16} color="#0F2D5E" /></button>
                <button className="btn-icon" onClick={() => handleOpenModal(cls)} title="Edit Class"><Pencil size={16} color="#0F2D5E" /></button>
                <button className="btn-icon delete" onClick={() => handleDeleteClass(cls.classId, cls.className)} title="Delete Class"><Trash2 size={16} color="#ef4444" /></button>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Add/Edit Class Modal */}
      {showModal && (
        <div className="modal-overlay" onClick={handleCloseModal}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h1>{editingClass ? 'Edit Class' : 'Create New Class'}</h1>
              <button className="modal-close" onClick={handleCloseModal}><X size={16} /></button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="modal-body">
                <div className="form-group">
                  <label className="form-label">Grade *</label>
                  <input type="text" name="className" className="form-input" value={formData.className} onChange={handleInputChange} required placeholder="e.g., Grade 7 - Sampaguita" />
                </div>
                <div className="form-group">
                  <label className="form-label">Subject *</label>
                  <input type="text" name="subject" className="form-input" value={formData.subject} onChange={handleInputChange} required placeholder="e.g., Mathematics" />
                </div>

                {/* Schedule - Day, Start Time, End Time */}
                <div className="form-row-3col">
                  <div className="form-group">
                    <label className="form-label">Day</label>
                    <select name="scheduleDay" className="form-input" value={formData.scheduleDay} onChange={handleInputChange}>
                      <option value="">Select day</option>
                      <option>Monday</option>
                      <option>Tuesday</option>
                      <option>Wednesday</option>
                      <option>Thursday</option>
                      <option>Friday</option>
                      <option>Saturday</option>
                    </select>
                  </div>
                  <div className="form-group">
                    <label className="form-label">Start Time</label>
                    <input
                      type="time"
                      name="scheduleTime"
                      className="form-input"
                      value={formData.scheduleTime}
                      onChange={handleInputChange}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">End Time</label>
                    <input
                      type="time"
                      name="scheduleTimeEnd"
                      className="form-input"
                      value={formData.scheduleTimeEnd}
                      onChange={handleInputChange}
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label">Section</label>
                  <input type="text" name="section" className="form-input" value={formData.section} onChange={handleInputChange} placeholder="e.g., A, B, C (optional)" />
                </div>
                <div className="form-group">
                  <label className="form-label">Academic Year</label>
                  <select name="academicYear" className="form-input" value={formData.academicYear} onChange={handleInputChange}>
                    <option value={`${currentYear}-${nextYear}`}>{currentYear}-{nextYear}</option>
                    <option value={`${currentYear - 1}-${currentYear}`}>{currentYear - 1}-{currentYear}</option>
                    <option value={`${currentYear + 1}-${currentYear + 2}`}>{currentYear + 1}-{currentYear + 2}</option>
                  </select>
                </div>
                <div className="form-group">
                  <label className="form-label">Assign Teacher *</label>
                  <select name="teacherId" className="form-input" value={formData.teacherId} onChange={handleInputChange} required>
                    <option value="">Select a teacher</option>
                    {teachers.map(teacher => <option key={teacher.userId} value={teacher.userId}>{teacher.fullName} ({teacher.email})</option>)}
                  </select>
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn-outline" onClick={handleCloseModal}>Cancel</button>
                <button type="submit" className="btn-primary" disabled={loading}>{loading ? 'Saving...' : (editingClass ? 'Update Class' : 'Create Class')}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* View Students Modal */}
      {showStudentsModal && selectedClass && (
        <div className="modal-overlay" onClick={() => setShowStudentsModal(false)}>
          <div className="modal modal-lg" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Students in {selectedClass.className}</h3>
              <button className="modal-close" onClick={() => setShowStudentsModal(false)}><X size={16} /></button>
            </div>
            <div className="modal-body">
              {classStudents.length === 0 ? (
                <div className="empty-state"><p>No students enrolled in this class yet.</p></div>
              ) : (
                <table className="students-table">
                  <thead>
                    <tr><th>Roll No.</th><th>Name</th><th>Email</th><th>Actions</th></tr>
                  </thead>
                  <tbody>
                    {classStudents.map(student => (
                      <tr key={student.studentId}>
                        <td>{student.rollNumber}</td>
                        <td>{student.fullName}</td>
                        <td>{student.email || '—'}</td>
                        <td><button className="btn-icon delete" title="Remove from class" onClick={() => handleRemoveStudent(student.studentId)}><Trash2 size={14} color="#ef4444" /></button></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
            <div className="modal-footer">
              <button className="btn-primary" onClick={handleOpenAddStudent}><Plus size={16} /> Add Student</button>
              <button className="btn-outline" onClick={() => setShowStudentsModal(false)}>Close</button>
            </div>
          </div>
        </div>
      )}

      {/* Add Student Modal */}
      {showAddStudentModal && (
        <div className="modal-overlay" onClick={handleCloseAddStudent}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Add Student to {selectedClass?.className}</h3>
              <button className="modal-close" onClick={handleCloseAddStudent}><X size={16} /></button>
            </div>
            <form onSubmit={handleAddStudentSubmit}>
              <div className="modal-body">
                {studentError && <div className="error-message">{studentError}</div>}
                <div className="form-group">
                  <label className="form-label">First Name *</label>
                  <input type="text" name="firstName" className="form-input" value={studentForm.firstName} onChange={handleStudentInputChange} required placeholder="e.g., Juan" />
                </div>
                <div className="form-group">
                  <label className="form-label">Last Name *</label>
                  <input type="text" name="lastName" className="form-input" value={studentForm.lastName} onChange={handleStudentInputChange} required placeholder="e.g., dela Cruz" />
                </div>
                <div className="form-group">
                  <label className="form-label">Roll Number *</label>
                  <input type="text" name="rollNumber" className="form-input" value={studentForm.rollNumber} onChange={handleStudentInputChange} required placeholder="e.g., 2024-0001" />
                </div>
                <div className="form-group">
                  <label className="form-label">Email</label>
                  <input type="email" name="email" className="form-input" value={studentForm.email} onChange={handleStudentInputChange} placeholder="student@email.com (optional)" />
                </div>
                <div className="form-group">
                  <label className="form-label">Phone</label>
                  <input type="text" name="phone" className="form-input" value={studentForm.phone} onChange={handleStudentInputChange} placeholder="09XX-XXX-XXXX (optional)" />
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn-outline" onClick={handleCloseAddStudent}>Cancel</button>
                <button type="submit" className="btn-primary" disabled={studentLoading}>{studentLoading ? 'Adding...' : 'Add Student'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default ManageClasses;