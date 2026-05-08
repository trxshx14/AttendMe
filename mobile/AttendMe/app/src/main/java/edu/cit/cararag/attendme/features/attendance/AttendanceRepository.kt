package edu.cit.cararag.attendme.features

import edu.cit.cararag.attendme.shared.model.Attendance
import edu.cit.cararag.attendme.shared.model.AttendanceRequest
import edu.cit.cararag.attendme.shared.model.BulkAttendanceRequest
import edu.cit.cararag.attendme.shared.network.RetrofitClient

class AttendanceRepository {

    private val api = RetrofitClient.instance

    suspend fun getAttendanceByClassAndDate(
        classId: Long,
        date: String
    ): Result<List<Attendance>> {
        return try {
            val response = api.getAttendanceByClassAndDate(classId, date)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception("Failed to load attendance"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cannot connect to server"))
        }
    }

    suspend fun markBulkAttendance(request: BulkAttendanceRequest): Result<List<Attendance>> {
        return try {
            val response = api.markBulkAttendance(request)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception("Failed to save attendance"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cannot connect to server"))
        }
    }

    suspend fun getAttendanceSummary(
        classId: Long,
        date: String
    ): Result<Map<String, Long>> {
        return try {
            val response = api.getAttendanceSummary(classId, date)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyMap())
            } else {
                Result.failure(Exception("Failed to load summary"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cannot connect to server"))
        }
    }

    suspend fun getAttendanceByStudent(
        studentId: Long,
        startDate: String,
        endDate: String
    ): Result<List<Attendance>> {
        return try {
            val response = api.getAttendanceByStudent(studentId, startDate, endDate)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception("Failed to load attendance history"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cannot connect to server"))
        }
    }

    suspend fun updateAttendance(id: Long, request: AttendanceRequest): Result<Attendance> {
        return try {
            val response = api.updateAttendance(id, request)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to update attendance"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cannot connect to server"))
        }
    }
}