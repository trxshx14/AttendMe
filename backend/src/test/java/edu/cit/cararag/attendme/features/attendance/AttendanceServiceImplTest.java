package edu.cit.cararag.attendme.features.attendance;

import edu.cit.cararag.attendme.features.attendance.dto.AttendanceRequest;
import edu.cit.cararag.attendme.features.attendance.dto.AttendanceResponse;
import edu.cit.cararag.attendme.shared.entity.*;
import edu.cit.cararag.attendme.shared.exception.DuplicateResourceException;
import edu.cit.cararag.attendme.shared.exception.ResourceNotFoundException;
import edu.cit.cararag.attendme.features.schoolclass.SchoolClassRepository;
import edu.cit.cararag.attendme.features.student.StudentRepository;
import edu.cit.cararag.attendme.features.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceImplTest {

    @Mock private AttendanceRepository attendanceRepository;
    @Mock private SchoolClassRepository schoolClassRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    private User testUser;
    private Student testStudent;
    private SchoolClass testClass;
    private Attendance testAttendance;
    private AttendanceRequest testRequest;

    @BeforeEach
void setUp() {
    testUser = new User();
    testUser.setEmail("teacher@test.com");
    testUser.setFullName("Test Teacher");
    testUser.setRole(Role.TEACHER);

    testStudent = new Student();
    testStudent.setRollNumber("STU001");
    testStudent.setFirstName("Test");
    testStudent.setLastName("Student");

    testClass = new SchoolClass();
    testClass.setClassName("Grade 9");
    testClass.setSection("Integrity");
    testClass.setSubject("Math");
    testClass.setAcademicYear("2025-2026");
    testClass.setTeacher(testUser);

    testAttendance = new Attendance();
    testAttendance.setSchoolClass(testClass);
    testAttendance.setStudent(testStudent);
    testAttendance.setDate(LocalDate.now());
    testAttendance.setStatus(AttendanceStatus.PRESENT);
    testAttendance.setMarkedBy(testUser);
    testAttendance.setCreatedAt(LocalDateTime.now());
    testAttendance.setUpdatedAt(LocalDateTime.now());

    testRequest = new AttendanceRequest();
    testRequest.setClassId(1L);
    testRequest.setStudentId(1L);
    testRequest.setMarkedById(1L);
    testRequest.setDate(LocalDate.now());
    testRequest.setStatus("PRESENT");
    testRequest.setRemarks("");
}

    // ── TC-ATT-01: Mark attendance successfully ──────────────────────────────
    @Test
    void markAttendance_withValidRequest_returnsResponse() {
        when(attendanceRepository.existsBySchoolClass_ClassIdAndStudent_StudentIdAndDate(
                any(), any(), any())).thenReturn(false);
        when(schoolClassRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(attendanceRepository.save(any())).thenReturn(testAttendance);

        AttendanceResponse response = attendanceService.markAttendance(testRequest);

        assertNotNull(response);
        assertEquals("PRESENT", response.getStatus());
        assertEquals("Test Student", response.getStudentName());
        verify(attendanceRepository, times(1)).save(any());
    }

    // ── TC-ATT-02: Duplicate attendance throws exception ──────────────────────
    @Test
    void markAttendance_whenAlreadyMarked_throwsDuplicateException() {
        when(attendanceRepository.existsBySchoolClass_ClassIdAndStudent_StudentIdAndDate(
                any(), any(), any())).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> attendanceService.markAttendance(testRequest));
        verify(attendanceRepository, never()).save(any());
    }

    // ── TC-ATT-03: Get attendance by class and date ───────────────────────────
    @Test
    void getAttendanceByClassAndDate_returnsListOfResponses() {
        when(attendanceRepository.findBySchoolClass_ClassIdAndDate(1L, LocalDate.now()))
                .thenReturn(List.of(testAttendance));

        List<AttendanceResponse> result =
                attendanceService.getAttendanceByClassAndDate(1L, LocalDate.now());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("PRESENT", result.get(0).getStatus());
    }

    // ── TC-ATT-04: Update attendance status ───────────────────────────────────
    @Test
    void updateAttendance_withValidId_updatesStatus() {
        AttendanceRequest updateRequest = new AttendanceRequest();
        updateRequest.setStatus("ABSENT");

        when(attendanceRepository.findById(1L)).thenReturn(Optional.of(testAttendance));
        testAttendance.setStatus(AttendanceStatus.ABSENT);
        when(attendanceRepository.save(any())).thenReturn(testAttendance);

        AttendanceResponse response = attendanceService.updateAttendance(1L, updateRequest);

        assertNotNull(response);
        assertEquals("ABSENT", response.getStatus());
    }

    // ── TC-ATT-05: Delete attendance successfully ─────────────────────────────
    @Test
    void deleteAttendance_withValidId_deletesSuccessfully() {
        when(attendanceRepository.existsById(1L)).thenReturn(true);
        doNothing().when(attendanceRepository).deleteById(1L);

        assertDoesNotThrow(() -> attendanceService.deleteAttendance(1L));
        verify(attendanceRepository, times(1)).deleteById(1L);
    }

    // ── TC-ATT-06: Delete non-existent attendance throws exception ────────────
    @Test
void deleteAttendance_withInvalidId_throwsNotFoundException() {
    when(attendanceRepository.existsById(99L)).thenReturn(false);

    assertThrows(ResourceNotFoundException.class,
            () -> attendanceService.deleteAttendance(99L));
}

    // ── TC-ATT-07: Check if attendance marked returns true ────────────────────
    @Test
    void hasAttendanceBeenMarked_whenMarked_returnsTrue() {
        when(attendanceRepository.existsBySchoolClass_ClassIdAndStudent_StudentIdAndDate(
                1L, 1L, LocalDate.now())).thenReturn(true);

        boolean result = attendanceService.hasAttendanceBeenMarked(1L, 1L, LocalDate.now());

        assertTrue(result);
    }

    // ── TC-ATT-08: Get attendance by invalid ID throws exception ──────────────
    @Test
void getAttendanceById_withInvalidId_throwsNotFoundException() {
    when(attendanceRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class,
            () -> attendanceService.getAttendanceById(99L));
}
}