package edu.cit.cararag.attendme.repository;

import edu.cit.cararag.attendme.entity.Attendance;
import edu.cit.cararag.attendme.entity.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

// IT342 Phase 3 – Web Main Feature Completed //

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findBySchoolClass_ClassIdAndDate(Long classId, LocalDate date);

    List<Attendance> findByStudent_StudentIdAndDateBetween(Long studentId, LocalDate startDate, LocalDate endDate);

    // ✅ NEW — used for weekly class report
    List<Attendance> findBySchoolClass_ClassIdAndDateBetween(Long classId, LocalDate startDate, LocalDate endDate);

    List<Attendance> findBySchoolClass_ClassIdAndStudent_StudentIdAndDate(Long classId, Long studentId, LocalDate date);

    @Query("SELECT a FROM Attendance a WHERE a.schoolClass.classId = :classId AND a.date = :date ORDER BY a.student.rollNumber")
    List<Attendance> findClassAttendanceByDate(@Param("classId") Long classId, @Param("date") LocalDate date);

    @Query("SELECT a.status, COUNT(a) FROM Attendance a WHERE a.schoolClass.classId = :classId AND a.date = :date GROUP BY a.status")
    List<Object[]> getAttendanceSummaryByClassAndDate(@Param("classId") Long classId, @Param("date") LocalDate date);

    @Query("SELECT a.student.studentId, a.status FROM Attendance a WHERE a.schoolClass.classId = :classId AND a.date = :date")
    List<Object[]> getStudentAttendanceStatus(@Param("classId") Long classId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.studentId = :studentId AND a.date BETWEEN :startDate AND :endDate AND a.status = :status")
    Long countAttendanceByStudentAndDateRangeAndStatus(
            @Param("studentId") Long studentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") AttendanceStatus status);

    boolean existsBySchoolClass_ClassIdAndStudent_StudentIdAndDate(Long classId, Long studentId, LocalDate date);

    @Modifying
    @Query("UPDATE Attendance a SET a.markedBy = null WHERE a.markedBy.userId = :userId")
    void nullifyMarkedBy(@Param("userId") Long userId);
}