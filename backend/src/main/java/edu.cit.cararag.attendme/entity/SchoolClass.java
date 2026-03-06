package edu.cit.cararag.attendme.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "classes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchoolClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_id")
    private Long classId;

    @Column(name = "class_name", nullable = false, length = 100)
    private String className;

    @Column(nullable = false, length = 100)
    private String subject;

    @Column(length = 50)
    private String section;

    @Column(name = "academic_year", length = 20)
    private String academicYear;

    @Column(name = "schedule_day", length = 20)
    private String scheduleDay;

    @Column(name = "schedule_time", length = 10)
    private String scheduleTime;

    @Column(name = "schedule_time_end", length = 10)  // ✅ NEW
    private String scheduleTimeEnd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @OneToMany(mappedBy = "schoolClass", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Student> students = new ArrayList<>();

    @OneToMany(mappedBy = "schoolClass", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Attendance> attendances = new ArrayList<>();

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void addStudent(Student student) {
        students.add(student);
        student.setSchoolClass(this);
    }

    public void addAttendance(Attendance attendance) {
        attendances.add(attendance);
        attendance.setSchoolClass(this);
    }
}