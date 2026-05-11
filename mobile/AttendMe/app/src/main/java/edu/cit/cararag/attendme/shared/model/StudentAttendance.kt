package edu.cit.cararag.attendme.shared.model

data class StudentAttendance(
    val attendanceId: Int? = null,
    val classId: Int? = null,
    val className: String? = null,
    val studentId: Int = 0,
    val studentName: String? = null,   // ✅ matches API field "studentName"
    val rollNumber: String? = null,
    val date: String? = null,
    var status: String = "present",
    val remarks: String? = null,
    val markedBy: String? = null
) {
    // ✅ Split studentName into firstName/lastName for the breakdown adapter
    val firstName: String
        get() {
            val parts = studentName?.trim()?.split("\\s+".toRegex()) ?: return ""
            return parts.dropLast(1).joinToString(" ").ifBlank { studentName ?: "" }
        }

    val lastName: String
        get() {
            val parts = studentName?.trim()?.split("\\s+".toRegex()) ?: return ""
            return if (parts.size > 1) parts.last() else ""
        }

    val fullName: String get() = studentName ?: ""
}