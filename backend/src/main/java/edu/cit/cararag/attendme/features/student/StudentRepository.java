package edu.cit.cararag.attendme.features.student;

import edu.cit.cararag.attendme.shared.entity.Student;
import edu.cit.cararag.attendme.shared.entity.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    
    Optional<Student> findByRollNumber(String rollNumber);
    
    List<Student> findBySchoolClass(SchoolClass schoolClass);
    
    List<Student> findBySchoolClass_ClassId(Long classId);
    
    @Query("SELECT s FROM Student s WHERE LOWER(s.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Student> searchByName(@Param("name") String name);
    
    @Query("SELECT s FROM Student s WHERE s.schoolClass.classId = :classId ORDER BY s.rollNumber")
    List<Student> findByClassIdOrderByRollNumber(@Param("classId") Long classId);
    
    Boolean existsByRollNumber(String rollNumber);
    
    @Query("SELECT COUNT(s) FROM Student s WHERE s.schoolClass.classId = :classId")
    Long countByClassId(@Param("classId") Long classId);
}
