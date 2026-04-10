package edu.cit.cararag.attendme.service;

import edu.cit.cararag.attendme.dto.requestdto.AttendanceRequest;
import edu.cit.cararag.attendme.dto.requestdto.BulkAttendanceRequest;
import edu.cit.cararag.attendme.dto.response.AttendanceResponse;
import edu.cit.cararag.attendme.dto.response.DailyReportResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// IT342 Phase 3 – Web Main Feature Completed //

public interface AttendanceService {

    AttendanceResponse markAttendance(AttendanceRequest request);

    List<AttendanceResponse> markBulkAttendance(BulkAttendanceRequest request);

    AttendanceResponse updateAttendance(Long attendanceId, AttendanceRequest request);

    void deleteAttendance(Long attendanceId);

    AttendanceResponse getAttendanceById(Long attendanceId);

    List<AttendanceResponse> getAttendanceByClassAndDate(Long classId, LocalDate date);

    List<AttendanceResponse> getAttendanceByStudentAndDateRange(Long studentId, LocalDate startDate, LocalDate endDate);

    // ✅ NEW — for weekly report
    List<AttendanceResponse> getAttendanceByClassAndDateRange(Long classId, LocalDate startDate, LocalDate endDate);

    Map<String, Long> getAttendanceSummary(Long classId, LocalDate date);

    DailyReportResponse generateDailyReport(Long classId, LocalDate date);

    boolean hasAttendanceBeenMarked(Long classId, Long studentId, LocalDate date);
}