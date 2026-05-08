package edu.cit.cararag.attendme.features.schoolclass.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SchoolClassRequest {

    @NotBlank(message = "Class name is required")
    private String className;

    @NotBlank(message = "Subject is required")
    private String subject;

    private String section;

    @NotBlank(message = "Academic year is required")
    private String academicYear;

    @NotNull(message = "Teacher ID is required")
    private Long teacherId;

    private String scheduleDay;    // ✅ NEW — e.g. "Monday"

    private String scheduleTime;   // ✅ NEW — e.g. "08:30"
}