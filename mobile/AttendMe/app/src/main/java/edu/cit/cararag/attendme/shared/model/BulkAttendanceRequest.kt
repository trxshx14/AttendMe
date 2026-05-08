package edu.cit.cararag.attendme.shared.model

data class BulkAttendanceRequest(
    val classId: Long,
    val date: String,
    val attendanceList: List<AttendanceItem>
)

data class AttendanceItem(
    val studentId: Long,
    val status: String,
    val remarks: String? = null
)