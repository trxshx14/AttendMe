package edu.cit.cararag.attendme.service.impl;

import edu.cit.cararag.attendme.dto.requestdto.AttendanceRequest;
import edu.cit.cararag.attendme.dto.requestdto.BulkAttendanceRequest;
import edu.cit.cararag.attendme.dto.response.AttendanceResponse;
import edu.cit.cararag.attendme.dto.response.DailyReportResponse;
import edu.cit.cararag.attendme.dto.response.StudentAttendanceResponse;
import edu.cit.cararag.attendme.entity.Attendance;
import edu.cit.cararag.attendme.entity.AttendanceStatus;
import edu.cit.cararag.attendme.entity.SchoolClass;
import edu.cit.cararag.attendme.entity.Student;
import edu.cit.cararag.attendme.entity.User;
import edu.cit.cararag.attendme.exception.DuplicateResourceException;
import edu.cit.cararag.attendme.exception.ResourceNotFoundException;
import edu.cit.cararag.attendme.repository.AttendanceRepository;
import edu.cit.cararag.attendme.repository.SchoolClassRepository;
import edu.cit.cararag.attendme.repository.StudentRepository;
import edu.cit.cararag.attendme.repository.UserRepository;
import edu.cit.cararag.attendme.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// IT342 Phase 3 – Web Main Feature Completed //

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceServiceImpl implements AttendanceService {
    
    private final AttendanceRepository attendanceRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    
    @Override
    public AttendanceResponse markAttendance(AttendanceRequest request) {
        
        if (attendanceRepository.existsBySchoolClass_ClassIdAndStudent_StudentIdAndDate(
                request.getClassId(), request.getStudentId(), request.getDate())) {
            throw new DuplicateResourceException("Attendance already marked for this student on " + request.getDate());
        }
        
        SchoolClass schoolClass = findClassById(request.getClassId());
        Student student = findStudentById(request.getStudentId());
        User markedBy = findUserById(request.getMarkedById());
        
        Attendance attendance = new Attendance();
        attendance.setSchoolClass(schoolClass);
        attendance.setStudent(student);
        attendance.setDate(request.getDate());
        attendance.setStatus(AttendanceStatus.valueOf(request.getStatus().toUpperCase()));
        attendance.setRemarks(request.getRemarks());
        attendance.setMarkedBy(markedBy);
        
        Attendance savedAttendance = attendanceRepository.save(attendance);
        return mapToResponse(savedAttendance);
    }
    
    @Override
    public List<AttendanceResponse> markBulkAttendance(BulkAttendanceRequest request) {
        List<AttendanceResponse> responses = new ArrayList<>();
        SchoolClass schoolClass = findClassById(request.getClassId());
        User markedBy = findUserById(request.getMarkedById());
        
        for (Map.Entry<Long, String> entry : request.getAttendanceData().entrySet()) {
            Long studentId = entry.getKey();
            String status = entry.getValue();
            
            
            if (attendanceRepository.existsBySchoolClass_ClassIdAndStudent_StudentIdAndDate(
                    request.getClassId(), studentId, request.getDate())) {
                continue;
            }
            
            Student student = findStudentById(studentId);
            
            Attendance attendance = new Attendance();
            attendance.setSchoolClass(schoolClass);
            attendance.setStudent(student);
            attendance.setDate(request.getDate());
            attendance.setStatus(AttendanceStatus.valueOf(status.toUpperCase()));
            attendance.setRemarks(request.getRemarks());
            attendance.setMarkedBy(markedBy);
            
            Attendance savedAttendance = attendanceRepository.save(attendance);
            responses.add(mapToResponse(savedAttendance));
        }
        
        return responses;
    }
    
    @Override
    public AttendanceResponse updateAttendance(Long attendanceId, AttendanceRequest request) {
        Attendance attendance = findAttendanceById(attendanceId);
        
        
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            attendance.setStatus(AttendanceStatus.valueOf(request.getStatus().toUpperCase()));
        }
        if (request.getRemarks() != null) {
            attendance.setRemarks(request.getRemarks());
        }
        
        Attendance updatedAttendance = attendanceRepository.save(attendance);
        return mapToResponse(updatedAttendance);
    }
    
    @Override
    public void deleteAttendance(Long attendanceId) {
        if (!attendanceRepository.existsById(attendanceId)) {
            throw new ResourceNotFoundException("Attendance", "id", attendanceId);
        }
        attendanceRepository.deleteById(attendanceId);
    }
    
    @Override
    public AttendanceResponse getAttendanceById(Long attendanceId) {
        Attendance attendance = findAttendanceById(attendanceId);
        return mapToResponse(attendance);
    }
    
    @Override
    public List<AttendanceResponse> getAttendanceByClassAndDate(Long classId, LocalDate date) {
        return attendanceRepository.findBySchoolClass_ClassIdAndDate(classId, date).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<AttendanceResponse> getAttendanceByStudentAndDateRange(Long studentId, LocalDate startDate, LocalDate endDate) {
        return attendanceRepository.findByStudent_StudentIdAndDateBetween(studentId, startDate, endDate).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public Map<String, Long> getAttendanceSummary(Long classId, LocalDate date) {
        List<Object[]> summary = attendanceRepository.getAttendanceSummaryByClassAndDate(classId, date);
        
        Map<String, Long> result = new HashMap<>();
        result.put("PRESENT", 0L);
        result.put("ABSENT", 0L);
        result.put("LATE", 0L);
        result.put("EXCUSED", 0L);
        
        for (Object[] row : summary) {
            AttendanceStatus status = (AttendanceStatus) row[0];
            Long count = (Long) row[1];
            result.put(status.name(), count);
        }
        
        return result;
    }
    
    @Override
    public DailyReportResponse generateDailyReport(Long classId, LocalDate date) {
        SchoolClass schoolClass = findClassById(classId);
        List<Attendance> attendances = attendanceRepository.findBySchoolClass_ClassIdAndDate(classId, date);
        
        
        Map<String, Long> longSummary = getAttendanceSummary(classId, date);
        
        
        Map<String, Integer> summary = new HashMap<>();
        for (Map.Entry<String, Long> entry : longSummary.entrySet()) {
            summary.put(entry.getKey(), entry.getValue().intValue());
        }
        
        List<StudentAttendanceResponse> attendanceList = attendances.stream()
                .map(a -> StudentAttendanceResponse.builder()
                        .studentId(a.getStudent().getStudentId())
                        .rollNumber(a.getStudent().getRollNumber())
                        .studentName(a.getStudent().getFullName())
                        .status(a.getStatus().name())
                        .remarks(a.getRemarks())
                        .build())
                .collect(Collectors.toList());
        
        return DailyReportResponse.builder()
                .date(date)
                .className(schoolClass.getClassName())
                .subject(schoolClass.getSubject())
                .section(schoolClass.getSection())
                .academicYear(schoolClass.getAcademicYear())
                .teacherName(schoolClass.getTeacher().getFullName())
                .totalStudents(schoolClass.getStudents().size())
                .summary(summary)
                .attendanceList(attendanceList)
                .build();
    }
    
    @Override
    public boolean hasAttendanceBeenMarked(Long classId, Long studentId, LocalDate date) {
        return attendanceRepository.existsBySchoolClass_ClassIdAndStudent_StudentIdAndDate(classId, studentId, date);
    }

    @Override
public List<AttendanceResponse> getAttendanceByClassAndDateRange(Long classId, LocalDate startDate, LocalDate endDate) {
    return attendanceRepository.findBySchoolClass_ClassIdAndDateBetween(classId, startDate, endDate)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
}
    
    private Attendance findAttendanceById(Long id) {
        return attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance", "id", id));
    }
    
    private SchoolClass findClassById(Long id) {
        return schoolClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", id));
    }
    
    private Student findStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));
    }
    
    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }
    
    private AttendanceResponse mapToResponse(Attendance attendance) {
        return AttendanceResponse.builder()
                .attendanceId(attendance.getAttendanceId())
                .classId(attendance.getSchoolClass().getClassId())
                .className(attendance.getSchoolClass().getClassName())
                .studentId(attendance.getStudent().getStudentId())
                .studentName(attendance.getStudent().getFullName())
                .rollNumber(attendance.getStudent().getRollNumber())
                .date(attendance.getDate())
                .status(attendance.getStatus().name())
                .remarks(attendance.getRemarks())
                .markedBy(attendance.getMarkedBy().getFullName())
                .createdAt(attendance.getCreatedAt())
                .updatedAt(attendance.getUpdatedAt())
                .build();
    }
}