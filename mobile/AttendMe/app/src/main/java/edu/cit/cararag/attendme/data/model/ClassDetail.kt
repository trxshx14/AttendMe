package edu.cit.cararag.attendme.data.model

data class ClassDetail(
    val classId: Int,
    val className: String?,
    val gradeLevel: String?,
    val section: String?,
    val subject: String?,
    val scheduleTime: String?,
    val scheduleTimeEnd: String?,
    val scheduleDay: String?,
    val studentCount: Int = 0,
    var presentCount: Int = 0,
    var absentCount: Int = 0,
    var lateCount: Int = 0,
    var excusedCount: Int = 0,
    var attendanceRate: Int = 0,
    var grade: String = "",
    var sectionLabel: String? = null,
)