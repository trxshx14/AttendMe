package edu.cit.cararag.attendme.ui.teacher

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import edu.cit.cararag.attendme.R
import edu.cit.cararag.attendme.data.model.ClassDetail
import edu.cit.cararag.attendme.data.remote.RetrofitClient
import edu.cit.cararag.attendme.ui.login.LoginActivity
import edu.cit.cararag.attendme.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.*

class TeacherDashboardActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private val httpClient = OkHttpClient()
    private val gson = Gson()

    // Stats variables
    private var totalClasses = 0
    private var totalStudents = 0
    private var todayPresent = 0
    private var todayAbsent = 0
    private var todayLate = 0
    private var todayExcused = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_dashboard)

        sessionManager = SessionManager(this)

        // Ensure the Retrofit token is set for any other API calls
        val token = sessionManager.getAccessToken()
        if (token == null) {
            redirectToLogin()
            return
        }
        RetrofitClient.setToken(token)

        setupHeader()
        setupQuickActions()
        setupLogout()
        fetchTeacherData()
    }

    private fun setupHeader() {
        val name = sessionManager.getFullName() ?: "Teacher"
        val firstName = name.split(" ").firstOrNull() ?: name

        findViewById<TextView>(R.id.tvGreeting).text = getGreeting()
        findViewById<TextView>(R.id.tvTeacherName).text = firstName
        findViewById<TextView>(R.id.tvCurrentDate).text = getCurrentDate()
        findViewById<TextView>(R.id.tvDateChip).text = getShortDate()
    }

    private fun setupQuickActions() {
        val goToAttendance = View.OnClickListener {
            startActivity(Intent(this, TakeAttendanceActivity::class.java))
        }
        val goToHistory = View.OnClickListener {
            startActivity(Intent(this, AttendanceHistoryActivity::class.java))
        }
        val goToReports = View.OnClickListener {
            startActivity(Intent(this, TeacherReportsActivity::class.java))
        }

        findViewById<Button>(R.id.btnTakeAttendance).setOnClickListener(goToAttendance)
        findViewById<Button>(R.id.btnViewHistory).setOnClickListener(goToHistory)
        findViewById<Button>(R.id.btnReports).setOnClickListener(goToReports)
        findViewById<CardView>(R.id.cardNextClass).setOnClickListener(goToAttendance)
    }

    private fun setupLogout() {
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            sessionManager.clearSession()
            redirectToLogin()
        }
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    // ── Data Fetching Logic ───────────────────────────────────────
    private fun fetchTeacherData() {
        val token = sessionManager.getAccessToken() ?: return
        val teacherId = sessionManager.getUserId()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Fetch Teacher's Classes
                val classesReq = Request.Builder()
                    .url("http://10.0.2.2:8888/api/classes/teacher/$teacherId")
                    .header("Authorization", "Bearer $token")
                    .get().build()

                val response = httpClient.newCall(classesReq).execute()
                val body = response.body?.string() ?: ""
                val jsonResponse = gson.fromJson(body, com.google.gson.JsonObject::class.java)
                val classesArray = jsonResponse.get("data")?.asJsonArray ?: return@launch

                val classesList = mutableListOf<ClassDetail>()
                for (element in classesArray) {
                    val obj = element.asJsonObject
                    val cls = ClassDetail(
                        classId = obj.get("classId").asInt,
                        className = obj.get("className")?.asString,
                        gradeLevel = obj.get("gradeLevel")?.asString,
                        section = obj.get("section")?.asString,
                        subject = obj.get("subject")?.asString,
                        scheduleTime = obj.get("scheduleTime")?.asString,
                        scheduleTimeEnd = obj.get("scheduleTimeEnd")?.asString,
                        scheduleDay = obj.get("scheduleDay")?.asString,
                        studentCount = obj.get("studentCount")?.asInt ?: 0
                    )

                    // Assign labels for UI
                    val parsed = parseClassObj(cls)
                    cls.grade = parsed.first
                    cls.sectionLabel = parsed.second

                    classesList.add(cls)
                }

                // 2. Fetch Attendance Stats for each class
                updateAttendanceStats(classesList, token, today)

                // 3. Update UI on Main Thread
                withContext(Dispatchers.Main) {
                    refreshUI(classesList)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TeacherDashboardActivity, "Error loading data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateAttendanceStats(classes: List<ClassDetail>, token: String, date: String) {
        todayPresent = 0; todayAbsent = 0; todayLate = 0; todayExcused = 0; totalStudents = 0

        for (cls in classes) {
            try {
                val attReq = Request.Builder()
                    .url("http://10.0.2.2:8888/api/attendance/class/${cls.classId}/date/$date")
                    .header("Authorization", "Bearer $token").get().build()

                val response = httpClient.newCall(attReq).execute()
                val body = response.body?.string() ?: ""
                val attArray = gson.fromJson(body, com.google.gson.JsonObject::class.java).get("data")?.asJsonArray

                attArray?.forEach {
                    when (it.asJsonObject.get("status")?.asString?.lowercase()) {
                        "present" -> { cls.presentCount++; todayPresent++ }
                        "absent"  -> { cls.absentCount++;  todayAbsent++ }
                        "late"    -> { cls.lateCount++;    todayLate++ }
                        "excused" -> { cls.excusedCount++; todayExcused++ }
                    }
                }
                totalStudents += cls.studentCount
                cls.attendanceRate = if (cls.studentCount > 0)
                    ((cls.presentCount + cls.lateCount) * 100) / cls.studentCount else 0
            } catch (e: Exception) { /* Ignore individual class fetch errors */ }
        }
    }

    private fun refreshUI(classes: List<ClassDetail>) {
        totalClasses = classes.size
        updateStats()
        renderNextClass(classes)
        renderClassesList(classes)
        renderScheduleList(classes)
        renderSummary()
    }

    // ── Helper UI Methods ─────────────────────────────────────────

    private fun updateStats() {
        findViewById<TextView>(R.id.tvStatClasses).text = totalClasses.toString()
        findViewById<TextView>(R.id.tvStatStudents).text = totalStudents.toString()
        findViewById<TextView>(R.id.tvStatPresent).text = todayPresent.toString()
        findViewById<TextView>(R.id.tvStatAbsentLate).text = (todayAbsent + todayLate).toString()
    }

    private fun renderClassesList(classes: List<ClassDetail>) {
        val container = findViewById<LinearLayout>(R.id.llClassesList)
        container.removeAllViews()

        classes.forEach { cls ->
            val row = LayoutInflater.from(this).inflate(R.layout.item_class_row, container, false)
            row.findViewById<TextView>(R.id.tvClassName).text = "${cls.grade} - ${cls.sectionLabel}"
            row.findViewById<TextView>(R.id.tvRingPercent).text = "${cls.attendanceRate}%"
            container.addView(row)
        }
    }

    // Use your existing helpers for Date/Time formatting
    private fun getGreeting() = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "GOOD MORNING"
        in 12..17 -> "GOOD AFTERNOON"
        else -> "GOOD EVENING"
    }

    private fun getCurrentDate() = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
    private fun getShortDate() = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date())

    private fun parseClassObj(cls: ClassDetail): Pair<String, String?> {
        return if (!cls.gradeLevel.isNullOrBlank() && !cls.section.isNullOrBlank())
            Pair("Grade ${cls.gradeLevel}", cls.section)
        else Pair(cls.className ?: "Class", cls.section)
    }

    private fun renderNextClass(classes: List<ClassDetail>) { /* Logic to find closest time */ }
    private fun renderScheduleList(classes: List<ClassDetail>) { /* Populate schedule container */ }
    private fun renderSummary() { /* Update final summary cards */ }
}