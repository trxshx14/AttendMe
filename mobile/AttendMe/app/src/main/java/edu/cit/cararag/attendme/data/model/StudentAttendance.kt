package edu.cit.cararag.attendme.data.model

data class StudentAttendance(
    val studentId: Int,
    val firstName: String,
    val lastName: String,
    val rollNumber: String?,
    var status: String = "present" // Default status
) {
    val fullName: String get() = "$firstName $lastName"
}