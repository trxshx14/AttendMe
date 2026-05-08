package edu.cit.cararag.attendme.features.student;

import edu.cit.cararag.attendme.features.student.dto.StudentRequest;
import edu.cit.cararag.attendme.features.attendance.dto.AttendanceSummaryResponse;
import edu.cit.cararag.attendme.features.student.dto.StudentResponse;
import edu.cit.cararag.attendme.shared.entity.SchoolClass;
import edu.cit.cararag.attendme.shared.entity.Student;
import edu.cit.cararag.attendme.shared.exception.DuplicateResourceException;
import edu.cit.cararag.attendme.shared.exception.ResourceNotFoundException;
import edu.cit.cararag.attendme.features.schoolclass.SchoolClassRepository;
import edu.cit.cararag.attendme.features.student.StudentRepository;
import edu.cit.cararag.attendme.features.student.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentServiceImpl implements StudentService {
    
    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;
    
    @Override
    public StudentResponse createStudent(StudentRequest request) {
        
        if (studentRepository.existsByRollNumber(request.getRollNumber())) {
            throw new DuplicateResourceException("Student", "roll number", request.getRollNumber());
        }
        
        Student student = new Student();
        student.setRollNumber(request.getRollNumber());
        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setEmail(request.getEmail());
        student.setPhone(request.getPhone());
        
        // Assign to class if classId is provided
        if (request.getClassId() != null) {
            SchoolClass schoolClass = findClassById(request.getClassId());
            student.setSchoolClass(schoolClass);
        }
        
        Student savedStudent = studentRepository.save(student);
        return mapToResponse(savedStudent);
    }
    
    @Override
    public StudentResponse getStudentById(Long id) {
        Student student = findStudentById(id);
        return mapToResponse(student);
    }
    
    @Override
    public StudentResponse getStudentByRollNumber(String rollNumber) {
        Student student = studentRepository.findByRollNumber(rollNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "roll number", rollNumber));
        return mapToResponse(student);
    }
    
    @Override
    public List<StudentResponse> getAllStudents() {
        return studentRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<StudentResponse> getStudentsByClass(Long classId) {
        
        findClassById(classId);
        
        return studentRepository.findBySchoolClass_ClassId(classId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<StudentResponse> searchStudents(String name) {
        return studentRepository.searchByName(name).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public StudentResponse updateStudent(Long id, StudentRequest request) {
        Student student = findStudentById(id);
        
        // Update roll number if provided and not taken
        if (request.getRollNumber() != null && !request.getRollNumber().isEmpty()) {
            if (!request.getRollNumber().equals(student.getRollNumber()) && 
                studentRepository.existsByRollNumber(request.getRollNumber())) {
                throw new DuplicateResourceException("Student", "roll number", request.getRollNumber());
            }
            student.setRollNumber(request.getRollNumber());
        }
        
        
        if (request.getFirstName() != null && !request.getFirstName().isEmpty()) {
            student.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null && !request.getLastName().isEmpty()) {
            student.setLastName(request.getLastName());
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            student.setEmail(request.getEmail());
        }
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            student.setPhone(request.getPhone());
        }
        
        
        if (request.getClassId() != null) {
            SchoolClass schoolClass = findClassById(request.getClassId());
            student.setSchoolClass(schoolClass);
        }
        
        Student updatedStudent = studentRepository.save(student);
        return mapToResponse(updatedStudent);
    }
    
    @Override
    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Student", "id", id);
        }
        studentRepository.deleteById(id);
    }
    
    @Override
    public StudentResponse assignToClass(Long studentId, Long classId) {
        Student student = findStudentById(studentId);
        SchoolClass schoolClass = findClassById(classId);
        
        student.setSchoolClass(schoolClass);
        Student updatedStudent = studentRepository.save(student);
        
        return mapToResponse(updatedStudent);
    }
    
    @Override
    public StudentResponse removeFromClass(Long studentId) {
        Student student = findStudentById(studentId);
        
        student.setSchoolClass(null);
        Student updatedStudent = studentRepository.save(student);
        
        return mapToResponse(updatedStudent);
    }
    
    @Override
    public AttendanceSummaryResponse getStudentAttendanceSummary(Long studentId, String startDate, String endDate) {
        
        Student student = findStudentById(studentId);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate start = LocalDate.parse(startDate, formatter);
        LocalDate end = LocalDate.parse(endDate, formatter);
        
        return AttendanceSummaryResponse.builder()
                .studentId(student.getStudentId())
                .studentName(student.getFullName())
                .rollNumber(student.getRollNumber())
                .className(student.getSchoolClass() != null ? student.getSchoolClass().getClassName() : "Not Assigned")
                .startDate(start)
                .endDate(end)
                .totalDays(0)
                .presentCount(0)
                .absentCount(0)
                .lateCount(0)
                .excusedCount(0)
                .attendancePercentage(0.0)
                .dailyAttendance(null)
                .build();
    }
    
    private Student findStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));
    }
    
    private SchoolClass findClassById(Long id) {
        return schoolClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", id));
    }
    
    private StudentResponse mapToResponse(Student student) {
        return StudentResponse.builder()
                .studentId(student.getStudentId())
                .rollNumber(student.getRollNumber())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .fullName(student.getFullName())
                .email(student.getEmail())
                .phone(student.getPhone())
                .classId(student.getSchoolClass() != null ? student.getSchoolClass().getClassId() : null)
                .className(student.getSchoolClass() != null ? student.getSchoolClass().getClassName() : null)
                .createdAt(student.getCreatedAt())
                .updatedAt(student.getUpdatedAt())
                .build();
    }
}
