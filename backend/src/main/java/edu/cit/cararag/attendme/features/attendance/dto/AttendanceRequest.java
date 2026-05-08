package edu.cit.cararag.attendme.features.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AttendanceRequest {
    
    @NotNull(message = "Class ID is required")
    private Long classId;
    
    @NotNull(message = "Student ID is required")
    private Long studentId;
    
    @NotNull(message = "Date is required")
    private LocalDate date;
    
    @NotNull(message = "Status is required")
    private String status;
    
    private String remarks;
    
    @NotNull(message = "Marked by user ID is required")
    private Long markedById;
}
