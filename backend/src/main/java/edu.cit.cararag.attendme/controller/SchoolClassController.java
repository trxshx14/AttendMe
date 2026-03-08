package edu.cit.cararag.attendme.controller;

import edu.cit.cararag.attendme.dto.requestdto.ClassRequest;
import edu.cit.cararag.attendme.dto.response.ApiResponse;
import edu.cit.cararag.attendme.dto.response.SchoolClassResponse;
import edu.cit.cararag.attendme.dto.response.StudentResponse;
import edu.cit.cararag.attendme.service.SchoolClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class SchoolClassController {

    private final SchoolClassService schoolClassService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<SchoolClassResponse>> createClass(@Valid @RequestBody ClassRequest request) {
        SchoolClassResponse response = schoolClassService.createClass(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Class created successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<SchoolClassResponse>>> getAllClasses() {
        List<SchoolClassResponse> responses = schoolClassService.getAllClasses();
        return ResponseEntity.ok(ApiResponse.success("Classes retrieved successfully", responses));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<SchoolClassResponse>> getClassById(@PathVariable Long id) {
        SchoolClassResponse response = schoolClassService.getClassById(id);
        return ResponseEntity.ok(ApiResponse.success("Class found", response));
    }

    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<SchoolClassResponse>>> getClassesByTeacher(@PathVariable Long teacherId) {
        List<SchoolClassResponse> responses = schoolClassService.getClassesByTeacher(teacherId);
        return ResponseEntity.ok(ApiResponse.success("Classes by teacher retrieved", responses));
    }

    @GetMapping("/academic-year/{academicYear}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<SchoolClassResponse>>> getClassesByAcademicYear(@PathVariable String academicYear) {
        List<SchoolClassResponse> responses = schoolClassService.getClassesByAcademicYear(academicYear);
        return ResponseEntity.ok(ApiResponse.success("Classes by academic year retrieved", responses));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<SchoolClassResponse>> updateClass(
            @PathVariable Long id,
            @Valid @RequestBody ClassRequest request) {
        SchoolClassResponse response = schoolClassService.updateClass(id, request);
        return ResponseEntity.ok(ApiResponse.success("Class updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteClass(@PathVariable Long id) {
        schoolClassService.deleteClass(id);
        return ResponseEntity.ok(ApiResponse.success("Class deleted successfully", null));
    }

    @GetMapping("/{id}/students")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<StudentResponse>>> getStudentsInClass(@PathVariable Long id) {
        List<StudentResponse> responses = schoolClassService.getStudentsInClass(id);
        return ResponseEntity.ok(ApiResponse.success("Students in class retrieved", responses));
    }

    @GetMapping("/{id}/student-count")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Long>> getStudentCountInClass(@PathVariable Long id) {
        Long count = schoolClassService.getStudentCountInClass(id);
        return ResponseEntity.ok(ApiResponse.success("Student count retrieved", count));
    }

    @PostMapping("/{classId}/students/{studentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<SchoolClassResponse>> addStudentToClass(
            @PathVariable Long classId,
            @PathVariable Long studentId) {
        SchoolClassResponse response = schoolClassService.addStudentToClass(classId, studentId);
        return ResponseEntity.ok(ApiResponse.success("Student added to class successfully", response));
    }

    @DeleteMapping("/{classId}/students/{studentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<SchoolClassResponse>> removeStudentFromClass(
            @PathVariable Long classId,
            @PathVariable Long studentId) {
        SchoolClassResponse response = schoolClassService.removeStudentFromClass(classId, studentId);
        return ResponseEntity.ok(ApiResponse.success("Student removed from class successfully", response));
    }
}