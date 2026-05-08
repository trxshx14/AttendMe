package edu.cit.cararag.attendme.features.schoolclass;

import edu.cit.cararag.attendme.shared.entity.SchoolClass;
import edu.cit.cararag.attendme.shared.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {
    
    List<SchoolClass> findByTeacher(User teacher);
    
    List<SchoolClass> findByTeacher_UserId(Long teacherId);
    
    List<SchoolClass> findByAcademicYear(String academicYear);
    
    @Query("SELECT c FROM SchoolClass c WHERE c.teacher.userId = :teacherId AND c.academicYear = :academicYear")
    List<SchoolClass> findClassesByTeacherAndYear(@Param("teacherId") Long teacherId, @Param("academicYear") String academicYear);
    
    @Query("SELECT COUNT(s) FROM SchoolClass c JOIN c.students s WHERE c.classId = :classId")
    Long countStudentsInClass(@Param("classId") Long classId);
    
    boolean existsByClassNameAndSectionAndAcademicYear(String className, String section, String academicYear);
}