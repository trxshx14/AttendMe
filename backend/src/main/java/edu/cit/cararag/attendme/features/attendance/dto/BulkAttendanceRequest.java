package edu.cit.cararag.attendme.features.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class BulkAttendanceRequest {
    
    @NotNull(message = "Class ID is required")
    private Long classId;
    
    @NotNull(message = "Date is required")
    private LocalDate date;
    
    @NotNull(message = "Attendance data is required")
    private Map<Long, String> attendanceData;
    
    private String remarks;
    
    @NotNull(message = "Marked by user ID is required")
    private Long markedById;
}
