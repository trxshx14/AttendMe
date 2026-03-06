package edu.cit.cararag.attendme.dto.requestdto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClassRequest {

    @NotBlank(message = "Class name is required")
    private String className;

    @NotBlank(message = "Subject is required")
    private String subject;

    private String section;

    @NotBlank(message = "Academic year is required")
    private String academicYear;

    @NotNull(message = "Teacher ID is required")
    private Long teacherId;

    private String scheduleDay;       // e.g. "Monday"

    private String scheduleTime;      // e.g. "08:00"

    private String scheduleTimeEnd;   // ✅ NEW — e.g. "10:00"
}