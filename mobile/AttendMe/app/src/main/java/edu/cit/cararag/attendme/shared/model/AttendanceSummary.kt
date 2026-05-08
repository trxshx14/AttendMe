package edu.cit.cararag.attendme.shared.model

data class AttendanceSummary(
    val present: Long,
    val absent: Long,
    val late: Long,
    val excused: Long
)