import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { GoogleOAuthProvider } from '@react-oauth/google';
import { AuthProvider } from './context/AuthContext';
import PrivateRoute from './components/PrivateRoute';
import Layout from './components/layout/Layout';

// Login
import Login from './features/auth/Login';

// Admin Pages
import AdminDashboard from './features/dashboard/AdminDashboard';
import ManageUsers from './features/user/ManageUsers';
import ManageClasses from './features/class/ManageClasses';
import AdminReports from './features/report/AdminReports';

// Teacher Pages
import TeacherDashboard from './features/dashboard/TeacherDashboard';
import TakeAttendance from './features/attendance/TakeAttendance';
import AttendanceHistory from './features/attendance/AttendanceHistory';
import TeacherReports from './features/report/TeacherReports';

import './App.css';

const GOOGLE_CLIENT_ID = '119199394548-v3gge1t3omphebmr8p8d8mlcjrib2ubs.apps.googleusercontent.com';

function App() {
  return (
    <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
      <AuthProvider>
        <Router>
          <Routes>
            {/* Public Routes */}
            <Route path="/login" element={<Login />} />

            {/* Protected Routes - Admin */}
            <Route path="/admin" element={
              <PrivateRoute allowedRoles={['admin']}>
                <Layout />
              </PrivateRoute>
            }>
              <Route index element={<Navigate to="/admin/dashboard" replace />} />
              <Route path="dashboard" element={<AdminDashboard />} />
              <Route path="users" element={<ManageUsers />} />
              <Route path="classes" element={<ManageClasses />} />
              <Route path="reports" element={<AdminReports />} />
            </Route>

            {/* Protected Routes - Teacher */}
            <Route path="/teacher" element={
              <PrivateRoute allowedRoles={['teacher']}>
                <Layout />
              </PrivateRoute>
            }>
              <Route index element={<Navigate to="/teacher/dashboard" replace />} />
              <Route path="dashboard" element={<TeacherDashboard />} />
              <Route path="take-attendance" element={<TakeAttendance />} />
              <Route path="history" element={<AttendanceHistory />} />
              <Route path="reports" element={<TeacherReports />} />
            </Route>

            {/* Root redirect */}
            <Route path="/" element={<Navigate to="/login" replace />} />

            {/* Catch all */}
            <Route path="*" element={<Navigate to="/login" replace />} />
          </Routes>
        </Router>
      </AuthProvider>
    </GoogleOAuthProvider>
  );
}

export default App;