import api from './api';

export const studentService = {
  async getAllStudents() {
    const response = await api.get('/students');
    return response.data;
  },

  async getStudentById(id) {
    const response = await api.get(`/students/${id}`);
    return response.data;
  },

  async getStudentByRollNumber(rollNumber) {
    const response = await api.get(`/students/roll-number/${rollNumber}`);
    return response.data;
  },

  async getStudentsByClass(classId) {
    const response = await api.get(`/students/class/${classId}`);
    return response.data;
  },

  async searchStudents(name) {
    const response = await api.get('/students/search', {
      params: { name }
    });
    return response.data;
  },

  async createStudent(studentData) {
    const response = await api.post('/students', studentData);
    return response.data;
  },

  async updateStudent(id, studentData) {
    const response = await api.put(`/students/${id}`, studentData);
    return response.data;
  },

  async deleteStudent(id) {
    const response = await api.delete(`/students/${id}`);
    return response.data;
  },

  async assignToClass(studentId, classId) {
    const response = await api.post(`/students/${studentId}/assign-to-class/${classId}`);
    return response.data;
  },

  async removeFromClass(studentId) {
    const response = await api.delete(`/students/${studentId}/remove-from-class`);
    return response.data;
  },
};