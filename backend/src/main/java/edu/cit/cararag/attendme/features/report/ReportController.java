package edu.cit.cararag.attendme.features.report;

import edu.cit.cararag.attendme.shared.dto.ApiResponse;
import edu.cit.cararag.attendme.features.attendance.dto.AttendanceResponse;
import edu.cit.cararag.attendme.features.attendance.dto.DailyReportResponse;
import edu.cit.cararag.attendme.features.attendance.AttendanceService;
import edu.cit.cararag.attendme.features.schoolclass.SchoolClassService;
import edu.cit.cararag.attendme.features.student.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final AttendanceService attendanceService;
    private final SchoolClassService schoolClassService;
    private final StudentService studentService;

    @GetMapping("/daily/{classId}/{date}")
    @PreAuthorize("hasRole('ADMIN') or @classSecurity.isTeacherOfClass(#classId)")
    public ResponseEntity<ApiResponse<DailyReportResponse>> getDailyReport(
            @PathVariable Long classId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DailyReportResponse report = attendanceService.generateDailyReport(classId, date);
        return ResponseEntity.ok(ApiResponse.success("Daily report generated", report));
    }

    @GetMapping("/weekly/{classId}")
    @PreAuthorize("hasRole('ADMIN') or @classSecurity.isTeacherOfClass(#classId)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWeeklyReport(
            @PathVariable Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        
        return ResponseEntity.ok(ApiResponse.success("Weekly report generated", null));
    }

    @GetMapping("/monthly/{classId}")
    @PreAuthorize("hasRole('ADMIN') or @classSecurity.isTeacherOfClass(#classId)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMonthlyReport(
            @PathVariable Long classId,
            @RequestParam int year,
            @RequestParam int month) {
        
        return ResponseEntity.ok(ApiResponse.success("Monthly report generated", null));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getStudentReport(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AttendanceResponse> responses = attendanceService.getAttendanceByStudentAndDateRange(studentId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Student report generated", responses));
    }

    @GetMapping("/class/{classId}/summary")
    @PreAuthorize("hasRole('ADMIN') or @classSecurity.isTeacherOfClass(#classId)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getClassSummary(@PathVariable Long classId) {
        Long studentCount = schoolClassService.getStudentCountInClass(classId);
        
        return ResponseEntity.ok(ApiResponse.success("Class summary generated", 
            Map.of("totalStudents", studentCount)));
    }

    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasRole('ADMIN') or #teacherId == authentication.principal.id")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTeacherReport(@PathVariable Long teacherId) {
        
        return ResponseEntity.ok(ApiResponse.success("Teacher report generated", null));
    }

    @GetMapping("/attendance-rate/{classId}")
    @PreAuthorize("hasRole('ADMIN') or @classSecurity.isTeacherOfClass(#classId)")
    public ResponseEntity<ApiResponse<Double>> getAttendanceRate(
            @PathVariable Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        return ResponseEntity.ok(ApiResponse.success("Attendance rate calculated", 0.0));
    }
}
