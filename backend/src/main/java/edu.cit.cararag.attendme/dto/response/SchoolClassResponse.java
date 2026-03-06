package edu.cit.cararag.attendme.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolClassResponse {
    private Long classId;
    private String className;
    private String subject;
    private String section;
    private String academicYear;
    private Long teacherId;
    private String teacherName;
    private String scheduleDay;
    private String scheduleTime;
    private String scheduleTimeEnd;   // ✅ NEW
    private Integer studentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}