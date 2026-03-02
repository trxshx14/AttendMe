import api from './api';

export const attendanceService = {
  async markAttendance(data) {
    const response = await api.post('/attendance', {
      classId: data.classId,
      studentId: data.studentId,
      date: data.date,
      status: data.status,
      remarks: data.remarks || '',
      markedById: data.markedById,
    });
    return response.data;
  },

  async markBulkAttendance(data) {
    const response = await api.post('/attendance/bulk', {
      classId: data.classId,
      date: data.date,
      attendanceData: data.attendanceData,
      remarks: data.remarks,
      markedById: data.markedById,
    });
    return response.data;
  },

  async getByClassAndDate(classId, date) {
    const response = await api.get(`/attendance/class/${classId}/date/${date}`);
    return response.data;
  },

  async getByStudentAndDateRange(studentId, startDate, endDate) {
    const response = await api.get(`/attendance/student/${studentId}`, {
      params: { startDate, endDate }
    });
    return response.data;
  },

  async updateAttendance(id, data) {
    const response = await api.put(`/attendance/${id}`, data);
    return response.data;
  },

  async deleteAttendance(id) {
    const response = await api.delete(`/attendance/${id}`);
    return response.data;
  },

  async getSummary(classId, date) {
    const response = await api.get(`/attendance/class/${classId}/summary/${date}`);
    return response.data;
  },

  async getDailyReport(classId, date) {
    const response = await api.get(`/attendance/class/${classId}/report/${date}`);
    return response.data;
  },

  async checkIfMarked(classId, studentId, date) {
    const response = await api.get('/attendance/check', {
      params: { classId, studentId, date }
    });
    return response.data;
  },
};