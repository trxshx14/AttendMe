package edu.cit.cararag.attendme.shared.model

data class Student(
    val studentId: Long,
    val firstName: String,
    val lastName: String,
    val fullName: String?,
    val rollNumber: String?,
    val email: String?,
    val classId: Long?,
    val className: String?
)