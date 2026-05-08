package edu.cit.cararag.attendme.shared.model

data class Attendance(
    val attendanceId: Long,
    val studentId: Long,
    val studentName: String?,
    val classId: Long,
    val className: String?,
    val date: String,
    val status: String,   // PRESENT, ABSENT, LATE, EXCUSED
    val remarks: String?,
    val markedBy: String?,
    val createdAt: String?
)