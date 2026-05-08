package edu.cit.cararag.attendme.features.student;

import edu.cit.cararag.attendme.features.student.dto.StudentRequest;
import edu.cit.cararag.attendme.shared.dto.ApiResponse;
import edu.cit.cararag.attendme.features.attendance.dto.AttendanceSummaryResponse;
import edu.cit.cararag.attendme.features.student.dto.StudentResponse;
import edu.cit.cararag.attendme.features.student.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<StudentResponse>> createStudent(@Valid @RequestBody StudentRequest request) {
        StudentResponse response = studentService.createStudent(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Student created successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<StudentResponse>>> getAllStudents() {
        List<StudentResponse> responses = studentService.getAllStudents();
        return ResponseEntity.ok(ApiResponse.success("Students retrieved successfully", responses));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudentById(@PathVariable Long id) {
        StudentResponse response = studentService.getStudentById(id);
        return ResponseEntity.ok(ApiResponse.success("Student found", response));
    }

    @GetMapping("/roll-number/{rollNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudentByRollNumber(@PathVariable String rollNumber) {
        StudentResponse response = studentService.getStudentByRollNumber(rollNumber);
        return ResponseEntity.ok(ApiResponse.success("Student found", response));
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("hasRole('ADMIN') or @classSecurity.isTeacherOfClass(#classId)")
    public ResponseEntity<ApiResponse<List<StudentResponse>>> getStudentsByClass(@PathVariable Long classId) {
        List<StudentResponse> responses = studentService.getStudentsByClass(classId);
        return ResponseEntity.ok(ApiResponse.success("Students by class retrieved", responses));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<StudentResponse>>> searchStudents(@RequestParam String name) {
        List<StudentResponse> responses = studentService.searchStudents(name);
        return ResponseEntity.ok(ApiResponse.success("Search results", responses));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<StudentResponse>> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentRequest request) {
        StudentResponse response = studentService.updateStudent(id, request);
        return ResponseEntity.ok(ApiResponse.success("Student updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.ok(ApiResponse.success("Student deleted successfully", null));
    }

    @PostMapping("/{studentId}/assign-to-class/{classId}")
    @PreAuthorize("hasRole('ADMIN') or @classSecurity.isTeacherOfClass(#classId)")
    public ResponseEntity<ApiResponse<StudentResponse>> assignToClass(
            @PathVariable Long studentId,
            @PathVariable Long classId) {
        StudentResponse response = studentService.assignToClass(studentId, classId);
        return ResponseEntity.ok(ApiResponse.success("Student assigned to class successfully", response));
    }

    @DeleteMapping("/{studentId}/remove-from-class")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<StudentResponse>> removeFromClass(@PathVariable Long studentId) {
        StudentResponse response = studentService.removeFromClass(studentId);
        return ResponseEntity.ok(ApiResponse.success("Student removed from class successfully", response));
    }

    @GetMapping("/{studentId}/attendance-summary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AttendanceSummaryResponse>> getStudentAttendanceSummary(
            @PathVariable Long studentId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        AttendanceSummaryResponse response = studentService.getStudentAttendanceSummary(studentId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Attendance summary retrieved", response));
    }
}
