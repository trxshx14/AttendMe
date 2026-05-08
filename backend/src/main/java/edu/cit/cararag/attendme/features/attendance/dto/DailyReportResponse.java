package edu.cit.cararag.attendme.features.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyReportResponse {
    private LocalDate date;
    private String className;
    private String subject;
    private String section;
    private String academicYear;
    private String teacherName;
    private Integer totalStudents;
    private Map<String, Integer> summary;
    private List<StudentAttendanceResponse> attendanceList;
}


