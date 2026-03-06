package edu.cit.cararag.attendme.service;

import edu.cit.cararag.attendme.dto.requestdto.ClassRequest;
import edu.cit.cararag.attendme.dto.response.SchoolClassResponse;
import edu.cit.cararag.attendme.dto.response.StudentResponse;

import java.util.List;

public interface SchoolClassService {

    SchoolClassResponse createClass(ClassRequest request);

    SchoolClassResponse getClassById(Long id);

    List<SchoolClassResponse> getAllClasses();

    List<SchoolClassResponse> getClassesByTeacher(Long teacherId);

    List<SchoolClassResponse> getClassesByAcademicYear(String academicYear);

    SchoolClassResponse updateClass(Long id, ClassRequest request);

    void deleteClass(Long id);

    SchoolClassResponse addStudentToClass(Long classId, Long studentId);

    SchoolClassResponse removeStudentFromClass(Long classId, Long studentId);

    List<StudentResponse> getStudentsInClass(Long classId);

    Long getStudentCountInClass(Long classId);
}