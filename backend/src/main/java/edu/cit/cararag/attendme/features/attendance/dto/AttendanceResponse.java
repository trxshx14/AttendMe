package edu.cit.cararag.attendme.features.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {
    private Long attendanceId;
    private Long classId;
    private String className;
    private Long studentId;
    private String studentName;
    private String rollNumber;
    private LocalDate date;
    private String status;
    private String remarks;
    private String markedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
