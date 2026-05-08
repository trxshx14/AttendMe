package edu.cit.cararag.attendme.shared.model

data class AttendanceRequest(
    val studentId: Long,
    val classId: Long,
    val date: String,       // format: yyyy-MM-dd
    val status: String,     // PRESENT, ABSENT, LATE, EXCUSED
    val remarks: String? = null
)