package edu.cit.cararag.attendme.features.student.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponse {
    private Long studentId;
    private String rollNumber;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private Long classId;
    private String className;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
