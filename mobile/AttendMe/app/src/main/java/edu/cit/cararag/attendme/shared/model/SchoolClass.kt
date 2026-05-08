package edu.cit.cararag.attendme.shared.model

data class SchoolClass(
    val classId: Long,
    val className: String,
    val subject: String?,
    val section: String?,
    val academicYear: String?,
    val schedule: String?,
    val teacherId: Long?,
    val teacherName: String?,
    val studentCount: Int?
)