package com.attendme.service.impl;

import com.attendme.dto.requestdto.SchoolClassRequest;
import com.attendme.dto.response.SchoolClassResponse;
import com.attendme.dto.response.StudentResponse;
import com.attendme.entity.SchoolClass;
import com.attendme.entity.User;
import com.attendme.entity.Role;
import com.attendme.exception.DuplicateResourceException;
import com.attendme.exception.ResourceNotFoundException;
import com.attendme.repository.SchoolClassRepository;
import com.attendme.repository.StudentRepository;
import com.attendme.repository.UserRepository;
import com.attendme.service.SchoolClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SchoolClassServiceImpl implements SchoolClassService {
    
    private final SchoolClassRepository schoolClassRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    
    @Override
    public SchoolClassResponse createClass(SchoolClassRequest request) {
        
        if (schoolClassRepository.existsByClassNameAndSectionAndAcademicYear(
                request.getClassName(), request.getSection(), request.getAcademicYear())) {
            throw new DuplicateResourceException("Class already exists with same name, section and academic year");
        }
        
        User teacher = findTeacherById(request.getTeacherId());
        
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setClassName(request.getClassName());
        schoolClass.setSubject(request.getSubject());
        schoolClass.setSection(request.getSection());
        schoolClass.setAcademicYear(request.getAcademicYear());
        schoolClass.setTeacher(teacher);
        
        SchoolClass savedClass = schoolClassRepository.save(schoolClass);
        return mapToResponse(savedClass);
    }
    
    @Override
    public SchoolClassResponse getClassById(Long id) {
        SchoolClass schoolClass = findClassById(id);
        return mapToResponse(schoolClass);
    }
    
    @Override
    public List<SchoolClassResponse> getAllClasses() {
        return schoolClassRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<SchoolClassResponse> getClassesByTeacher(Long teacherId) {
        findTeacherById(teacherId);
        return schoolClassRepository.findByTeacher_UserId(teacherId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<SchoolClassResponse> getClassesByAcademicYear(String academicYear) {
        return schoolClassRepository.findByAcademicYear(academicYear).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public SchoolClassResponse updateClass(Long id, SchoolClassRequest request) {
        SchoolClass schoolClass = findClassById(id);
        
        
        if ((request.getClassName() != null && !request.getClassName().equals(schoolClass.getClassName())) ||
            (request.getSection() != null && !request.getSection().equals(schoolClass.getSection())) ||
            (request.getAcademicYear() != null && !request.getAcademicYear().equals(schoolClass.getAcademicYear()))) {
            
            String className = request.getClassName() != null ? request.getClassName() : schoolClass.getClassName();
            String section = request.getSection() != null ? request.getSection() : schoolClass.getSection();
            String academicYear = request.getAcademicYear() != null ? request.getAcademicYear() : schoolClass.getAcademicYear();
            
            if (schoolClassRepository.existsByClassNameAndSectionAndAcademicYear(className, section, academicYear)) {
                throw new DuplicateResourceException("Class already exists with same name, section and academic year");
            }
        }
        
        
        if (request.getClassName() != null) {
            schoolClass.setClassName(request.getClassName());
        }
        if (request.getSubject() != null) {
            schoolClass.setSubject(request.getSubject());
        }
        if (request.getSection() != null) {
            schoolClass.setSection(request.getSection());
        }
        if (request.getAcademicYear() != null) {
            schoolClass.setAcademicYear(request.getAcademicYear());
        }
        if (request.getTeacherId() != null && !request.getTeacherId().equals(schoolClass.getTeacher().getUserId())) {
            User teacher = findTeacherById(request.getTeacherId());
            schoolClass.setTeacher(teacher);
        }
        
        SchoolClass updatedClass = schoolClassRepository.save(schoolClass);
        return mapToResponse(updatedClass);
    }
    
    @Override
    public void deleteClass(Long id) {
        if (!schoolClassRepository.existsById(id)) {
            throw new ResourceNotFoundException("Class", "id", id);
        }
        schoolClassRepository.deleteById(id);
    }
    
    @Override
    public SchoolClassResponse addStudentToClass(Long classId, Long studentId) {
        
        SchoolClass schoolClass = findClassById(classId);
        return mapToResponse(schoolClass);
    }
    
    @Override
    public SchoolClassResponse removeStudentFromClass(Long classId, Long studentId) {
        
        SchoolClass schoolClass = findClassById(classId);
        return mapToResponse(schoolClass);
    }
    
    @Override
public List<StudentResponse> getStudentsInClass(Long classId) {
    findClassById(classId); // validates class exists
    return studentRepository.findBySchoolClass_ClassId(classId).stream()
            .map(student -> StudentResponse.builder()
                    .studentId(student.getStudentId())
                    .rollNumber(student.getRollNumber())
                    .firstName(student.getFirstName())
                    .lastName(student.getLastName())
                    .fullName(student.getFullName())
                    .email(student.getEmail())
                    .phone(student.getPhone())
                    .classId(classId)
                    .className(student.getSchoolClass().getClassName())
                    .createdAt(student.getCreatedAt())
                    .updatedAt(student.getUpdatedAt())
                    .build())
            .collect(Collectors.toList());
}
    
    @Override
    public Long getStudentCountInClass(Long classId) {
        return schoolClassRepository.countStudentsInClass(classId);
    }
    
    private SchoolClass findClassById(Long id) {
        return schoolClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", id));
    }
    
    private User findTeacherById(Long id) {
        User teacher = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        if (teacher.getRole() != Role.TEACHER) {
            throw new ResourceNotFoundException("User with id " + id + " is not a teacher");
        }
        
        return teacher;
    }
    
    private SchoolClassResponse mapToResponse(SchoolClass schoolClass) {
        return SchoolClassResponse.builder()
                .classId(schoolClass.getClassId())
                .className(schoolClass.getClassName())
                .subject(schoolClass.getSubject())
                .section(schoolClass.getSection())
                .academicYear(schoolClass.getAcademicYear())
                .teacherId(schoolClass.getTeacher().getUserId())
                .teacherName(schoolClass.getTeacher().getFullName())
                .studentCount(schoolClass.getStudents() != null ? schoolClass.getStudents().size() : 0)
                .createdAt(schoolClass.getCreatedAt())
                .updatedAt(schoolClass.getUpdatedAt())
                .build();
    }
}
