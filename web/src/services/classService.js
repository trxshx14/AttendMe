import api from './api';

export const classService = {
  async getAllClasses() {
    const response = await api.get('/classes');
    return response.data;
  },

  async getClassById(id) {
    const response = await api.get(`/classes/${id}`);
    return response.data;
  },

  async getClassesByTeacher(teacherId) {
    const response = await api.get(`/classes/teacher/${teacherId}`);
    return response.data;
  },

  async getClassesByAcademicYear(academicYear) {
    const response = await api.get(`/classes/academic-year/${academicYear}`);
    return response.data;
  },

  async createClass(classData) {
    const response = await api.post('/classes', classData);
    return response.data;
  },

  async updateClass(id, classData) {
    const response = await api.put(`/classes/${id}`, classData);
    return response.data;
  },

  async deleteClass(id) {
    const response = await api.delete(`/classes/${id}`);
    return response.data;
  },

  async getStudentsInClass(classId) {
    const response = await api.get(`/classes/${classId}/students`);
    return response.data;
  },

  async getStudentCount(classId) {
    const response = await api.get(`/classes/${classId}/student-count`);
    return response.data;
  },
};