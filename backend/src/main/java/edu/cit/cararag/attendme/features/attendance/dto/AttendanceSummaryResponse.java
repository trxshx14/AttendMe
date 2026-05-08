package edu.cit.cararag.attendme.features.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryResponse {
    private Long studentId;
    private String studentName;
    private String rollNumber;
    private String className;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDays;
    private Integer presentCount;
    private Integer absentCount;
    private Integer lateCount;
    private Integer excusedCount;
    private Double attendancePercentage;
    private Map<LocalDate, String> dailyAttendance;
}