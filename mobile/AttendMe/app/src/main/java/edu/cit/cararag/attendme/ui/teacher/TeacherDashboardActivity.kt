package edu.cit.cararag.attendme.ui.teacher

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import edu.cit.cararag.attendme.R
import edu.cit.cararag.attendme.data.model.SchoolClass
import edu.cit.cararag.attendme.data.repository.ClassRepository
import edu.cit.cararag.attendme.ui.login.LoginActivity
import edu.cit.cararag.attendme.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.*

class TeacherDashboardActivity : AppCompatActivity() {

    private val classRepo  = ClassRepository()
    private val httpClient = OkHttpClient()
    private val gson       = Gson()
    private lateinit var token: String
    private lateinit var sessionManager: SessionManager
    private lateinit var classAdapter: TeacherClassAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_dashboard)

        sessionManager = SessionManager(this)
        token = sessionManager.getAccessToken() ?: ""

        // Greeting + name
        val name = sessionManager.getFullName() ?: "Teacher"
        val firstName = name.split(" ").firstOrNull() ?: name
        findViewById<TextView>(R.id.tvTeacherName).text = firstName
        findViewById<TextView>(R.id.tvGreeting).text    = getGreeting()
        findViewById<TextView>(R.id.tvCurrentDate).text = getCurrentDate()

        // Setup RecyclerView
        classAdapter = TeacherClassAdapter(emptyList()) { cls ->
            openTakeAttendance(cls)
        }
        val rvClasses = findViewById<RecyclerView>(R.id.rvMyClasses)
        rvClasses.layoutManager = LinearLayoutManager(this)
        rvClasses.adapter = classAdapter

        // Quick actions
        findViewById<MaterialButton>(R.id.btnTakeAttendance).setOnClickListener {
            startActivity(Intent(this, TakeAttendanceActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnTakeAttendanceClasses).setOnClickListener {
            startActivity(Intent(this, TakeAttendanceActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnTakeAttendanceNext).setOnClickListener {
            startActivity(Intent(this, TakeAttendanceActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnViewHistory).setOnClickListener {
            startActivity(Intent(this, AttendanceHistoryActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnViewReports).setOnClickListener {
            startActivity(Intent(this, TeacherReportsActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            sessionManager.clearSession()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }

        loadDashboard()
    }

    private fun loadDashboard() {
        val teacherId = sessionManager.getUserId() ?: return
        val today     = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        lifecycleScope.launch {
            // Fetch teacher's classes
            val classResult = withContext(Dispatchers.IO) {
                try {
                    val req = Request.Builder()
                        .url("http://10.0.2.2:8888/api/classes/teacher/$teacherId")
                        .header("Authorization", "Bearer $token").get().build()
                    val body = httpClient.newCall(req).execute().body?.string() ?: ""
                    val json = gson.fromJson(body, com.google.gson.JsonObject::class.java)
                    if (json.get("success")?.asBoolean == true) {
                        val type = object : com.google.gson.reflect.TypeToken<List<SchoolClass>>() {}.type
                        gson.fromJson<List<SchoolClass>>(json.get("data").asJsonArray, type)
                    } else emptyList()
                } catch (e: Exception) { emptyList<SchoolClass>() }
            }

            // Fetch today's attendance for each class in parallel
            val classItems = classResult.map { cls ->
                async(Dispatchers.IO) {
                    try {
                        val req = Request.Builder()
                            .url("http://10.0.2.2:8888/api/attendance/class/${cls.classId}/date/$today")
                            .header("Authorization", "Bearer $token").get().build()
                        val body = httpClient.newCall(req).execute().body?.string() ?: ""
                        val json = gson.fromJson(body, com.google.gson.JsonObject::class.java)
                        val records = json.get("data")?.asJsonArray ?: com.google.gson.JsonArray()
                        val present = records.count {
                            it.asJsonObject.get("status")?.asString?.lowercase() == "present"
                        }
                        val total = cls.studentCount ?: 0
                        val rate  = if (total > 0) (present * 100 / total) else 0
                        TeacherClassItem(cls, present, rate)
                    } catch (e: Exception) {
                        TeacherClassItem(cls)
                    }
                }
            }.awaitAll()

            // Compute totals
            val totalPresent = classItems.sumOf { it.presentCount }
            val totalAbsent  = classItems.sumOf { cls ->
                try {
                    val req = Request.Builder()
                        .url("http://10.0.2.2:8888/api/attendance/class/${cls.cls.classId}/date/$today")
                        .header("Authorization", "Bearer $token").get().build()
                    val body = httpClient.newCall(req).execute().body?.string() ?: ""
                    val json = gson.fromJson(body, com.google.gson.JsonObject::class.java)
                    val records = json.get("data")?.asJsonArray ?: com.google.gson.JsonArray()
                    records.count { it.asJsonObject.get("status")?.asString?.lowercase() == "absent" }
                } catch (e: Exception) { 0 }
            }

            // Simpler: fetch all attendance once per class and aggregate
            var sumPresent = 0; var sumAbsent = 0; var sumLate = 0; var sumExcused = 0
            val attendanceFetches = classResult.map { cls ->
                async(Dispatchers.IO) {
                    try {
                        val req = Request.Builder()
                            .url("http://10.0.2.2:8888/api/attendance/class/${cls.classId}/date/$today")
                            .header("Authorization", "Bearer $token").get().build()
                        val body = httpClient.newCall(req).execute().body?.string() ?: ""
                        val json = gson.fromJson(body, com.google.gson.JsonObject::class.java)
                        json.get("data")?.asJsonArray ?: com.google.gson.JsonArray()
                    } catch (e: Exception) { com.google.gson.JsonArray() }
                }
            }.awaitAll()

            attendanceFetches.forEach { records ->
                records.forEach { r ->
                    when (r.asJsonObject.get("status")?.asString?.lowercase()) {
                        "present" -> sumPresent++
                        "absent"  -> sumAbsent++
                        "late"    -> sumLate++
                        "excused" -> sumExcused++
                    }
                }
            }

            val totalStudents = classResult.sumOf { it.studentCount ?: 0 }

            withContext(Dispatchers.Main) {
                // Stats
                findViewById<TextView>(R.id.tvStatClasses).text  = classResult.size.toString()
                findViewById<TextView>(R.id.tvStatStudents).text = totalStudents.toString()
                findViewById<TextView>(R.id.tvStatPresent).text  = sumPresent.toString()
                findViewById<TextView>(R.id.tvStatAbsent).text   = sumAbsent.toString()
                findViewById<TextView>(R.id.tvStatLate).text     = sumLate.toString()
                findViewById<TextView>(R.id.tvStatExcused).text  = sumExcused.toString()

                // Summary
                findViewById<TextView>(R.id.tvSummaryPresent).text = sumPresent.toString()
                findViewById<TextView>(R.id.tvSummaryAbsent).text  = sumAbsent.toString()
                findViewById<TextView>(R.id.tvSummaryLate).text    = sumLate.toString()
                findViewById<TextView>(R.id.tvSummaryExcused).text = sumExcused.toString()
                val totalMarked = sumPresent + sumAbsent + sumLate + sumExcused
                findViewById<TextView>(R.id.tvSummaryTotal).text = totalMarked.toString()

                // Classes list
                if (classItems.isEmpty()) {
                    findViewById<LinearLayout>(R.id.layoutNoClasses).visibility = View.VISIBLE
                    findViewById<RecyclerView>(R.id.rvMyClasses).visibility     = View.GONE
                } else {
                    findViewById<LinearLayout>(R.id.layoutNoClasses).visibility = View.GONE
                    findViewById<RecyclerView>(R.id.rvMyClasses).visibility     = View.VISIBLE
                    classAdapter.updateData(classItems)
                }

                // Next class banner
                showNextClass(classItems)
            }
        }
    }

    private fun showNextClass(items: List<TeacherClassItem>) {
        val nowMins = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
        val dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date()).lowercase()

        val scheduled = items.filter { item ->
            val cls = item.cls
            val schedule = cls.schedule ?: return@filter false
            val parts = schedule.split(" ")
            if (parts.isNotEmpty()) {
                val day = parts[0].lowercase()
                dayName.startsWith(day.take(3))
            } else true
        }.sortedBy { item ->
            val schedule = item.cls.schedule ?: ""
            val timePart = schedule.split(" ").getOrNull(1) ?: ""
            timeToMinutes(timePart) ?: 9999
        }

        val next = scheduled.firstOrNull { item ->
            val schedule = item.cls.schedule ?: ""
            val timePart = schedule.split(" ").getOrNull(1) ?: ""
            val mins = timeToMinutes(timePart) ?: return@firstOrNull false
            mins > nowMins
        } ?: scheduled.lastOrNull()

        if (next != null) {
            val cls     = next.cls
            val grade   = cls.className ?: ""
            val section = cls.section?.let { " — $it" } ?: ""
            val schedule = cls.schedule ?: ""
            val timePart = schedule.split(" ").getOrNull(1) ?: ""
            val timeStr  = formatTime(timePart)

            findViewById<LinearLayout>(R.id.layoutNextClass).visibility = View.VISIBLE
            val isNext = scheduled.firstOrNull {
                val sp = it.cls.schedule?.split(" ")?.getOrNull(1) ?: ""
                (timeToMinutes(sp) ?: 9999) > nowMins
            } == next
            findViewById<TextView>(R.id.tvNextClassLabel).text =
                if (isNext) "YOUR NEXT CLASS" else "LAST CLASS TODAY"
            findViewById<TextView>(R.id.tvNextClassName).text = "$grade$section"
            val meta = buildString {
                cls.subject?.let { append(it); append(" · ") }
                if (timeStr.isNotBlank()) append("$timeStr · ")
                append("${cls.studentCount ?: 0} students")
            }
            findViewById<TextView>(R.id.tvNextClassMeta).text = meta
        }
    }

    private fun openTakeAttendance(cls: SchoolClass) {
        startActivity(Intent(this, TakeAttendanceActivity::class.java).apply {
            putExtra("classId", cls.classId)
            putExtra("className", cls.className)
        })
    }

    private fun timeToMinutes(time: String): Int? {
        if (time.isBlank()) return null
        return try {
            val parts = time.split(":")
            parts[0].toInt() * 60 + (parts.getOrNull(1)?.toInt() ?: 0)
        } catch (e: Exception) { null }
    }

    private fun formatTime(time: String): String {
        if (time.isBlank()) return ""
        return try {
            val parts = time.split(":")
            val h = parts[0].toInt()
            val m = parts.getOrNull(1)?.toInt() ?: 0
            val ampm = if (h >= 12) "PM" else "AM"
            "${if (h % 12 == 0) 12 else h % 12}:${String.format("%02d", m)} $ampm"
        } catch (e: Exception) { time }
    }

    private fun getGreeting(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11  -> "GOOD MORNING"
            in 12..17 -> "GOOD AFTERNOON"
            else      -> "GOOD EVENING"
        }
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
    }
}