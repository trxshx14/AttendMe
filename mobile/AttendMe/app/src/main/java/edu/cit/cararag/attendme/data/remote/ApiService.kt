package edu.cit.cararag.attendme.data.remote

import edu.cit.cararag.attendme.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Auth ──────────────────────────────────────────────
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/google")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): Response<LoginResponse>

    // ── Users ─────────────────────────────────────────────
    @GET("users")
    suspend fun getAllUsers(): Response<ApiResponse<List<User>>>

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: Long): Response<ApiResponse<User>>

    @GET("users/role/{role}")
    suspend fun getUsersByRole(@Path("role") role: String): Response<ApiResponse<List<User>>>

    // ── Classes ───────────────────────────────────────────
    @GET("classes")
    suspend fun getAllClasses(): Response<ApiResponse<List<SchoolClass>>>

    @GET("classes/{id}")
    suspend fun getClassById(@Path("id") id: Long): Response<ApiResponse<SchoolClass>>

    @GET("classes/teacher/{teacherId}")
    suspend fun getClassesByTeacher(@Path("teacherId") teacherId: Long): Response<ApiResponse<List<SchoolClass>>>

    @GET("classes/{id}/students")
    suspend fun getStudentsInClass(@Path("id") classId: Long): Response<ApiResponse<List<Student>>>

    @GET("classes/{id}/student-count")
    suspend fun getStudentCount(@Path("id") classId: Long): Response<ApiResponse<Long>>

    // ── Students ──────────────────────────────────────────
    @GET("students")
    suspend fun getAllStudents(): Response<ApiResponse<List<Student>>>

    @GET("students/{id}")
    suspend fun getStudentById(@Path("id") id: Long): Response<ApiResponse<Student>>

    @GET("students/class/{classId}")
    suspend fun getStudentsByClass(@Path("classId") classId: Long): Response<ApiResponse<List<Student>>>

    // ── Attendance ────────────────────────────────────────
    @POST("attendance")
    suspend fun markAttendance(@Body request: AttendanceRequest): Response<ApiResponse<Attendance>>

    @POST("attendance/bulk")
    suspend fun markBulkAttendance(@Body request: BulkAttendanceRequest): Response<ApiResponse<List<Attendance>>>

    @GET("attendance/class/{classId}/date/{date}")
    suspend fun getAttendanceByClassAndDate(
        @Path("classId") classId: Long,
        @Path("date") date: String
    ): Response<ApiResponse<List<Attendance>>>

    @GET("attendance/class/{classId}/summary/{date}")
    suspend fun getAttendanceSummary(
        @Path("classId") classId: Long,
        @Path("date") date: String
    ): Response<ApiResponse<Map<String, Long>>>

    @GET("attendance/class/{classId}/report/{date}")
    suspend fun getDailyReport(
        @Path("classId") classId: Long,
        @Path("date") date: String
    ): Response<ApiResponse<Any>>

    @GET("attendance/student/{studentId}")
    suspend fun getAttendanceByStudent(
        @Path("studentId") studentId: Long,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<ApiResponse<List<Attendance>>>

    @PUT("attendance/{id}")
    suspend fun updateAttendance(
        @Path("id") id: Long,
        @Body request: AttendanceRequest
    ): Response<ApiResponse<Attendance>>

    @GET("attendance/check")
    suspend fun checkAttendanceMarked(
        @Query("classId") classId: Long,
        @Query("studentId") studentId: Long,
        @Query("date") date: String
    ): Response<ApiResponse<Boolean>>
}