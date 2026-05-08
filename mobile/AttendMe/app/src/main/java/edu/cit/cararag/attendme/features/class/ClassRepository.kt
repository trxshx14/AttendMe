package edu.cit.cararag.attendme.features

import edu.cit.cararag.attendme.shared.model.SchoolClass
import edu.cit.cararag.attendme.shared.model.Student
import edu.cit.cararag.attendme.shared.network.RetrofitClient

class ClassRepository {

    private val api = RetrofitClient.instance

    suspend fun getAllClasses(): Result<List<SchoolClass>> {
        return try {
            val response = api.getAllClasses()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception("Failed to load classes"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cannot connect to server"))
        }
    }

    suspend fun getClassesByTeacher(teacherId: Long): Result<List<SchoolClass>> {
        return try {
            val response = api.getClassesByTeacher(teacherId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception("Failed to load classes"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cannot connect to server"))
        }
    }

    suspend fun getStudentsInClass(classId: Long): Result<List<Student>> {
        return try {
            val response = api.getStudentsInClass(classId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception("Failed to load students"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cannot connect to server"))
        }
    }
}