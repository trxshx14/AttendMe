package edu.cit.cararag.attendme.features.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAttendanceResponse {
    
    private Long studentId;
    private String rollNumber;
    private String studentName;
    private String status;
    private String remarks;
}

