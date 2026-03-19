package edu.cit.cararag.attendme.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import edu.cit.cararag.attendme.R
import edu.cit.cararag.attendme.data.remote.RetrofitClient
import edu.cit.cararag.attendme.data.repository.ClassRepository
import edu.cit.cararag.attendme.data.repository.UserRepository
import edu.cit.cararag.attendme.ui.login.LoginActivity
import edu.cit.cararag.attendme.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Calendar

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private val classRepository = ClassRepository()
    private val userRepository  = UserRepository()
    private val httpClient      = OkHttpClient()
    private val gson            = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        sessionManager = SessionManager(this)
        RetrofitClient.setToken(sessionManager.getAccessToken())

        val name = sessionManager.getFullName() ?: "Administrator"
        findViewById<TextView>(R.id.tvGreeting).text  = getGreeting()
        findViewById<TextView>(R.id.tvAdminName).text = name.split(" ").firstOrNull() ?: name

        findViewById<androidx.cardview.widget.CardView>(R.id.cardManageUsers).setOnClickListener {
            startActivity(Intent(this, ManageUsersActivity::class.java))
        }
        findViewById<androidx.cardview.widget.CardView>(R.id.cardManageClasses).setOnClickListener {
            startActivity(Intent(this, ManageClassesActivity::class.java))
        }
        findViewById<androidx.cardview.widget.CardView>(R.id.cardReports).setOnClickListener {
            startActivity(Intent(this, AdminReportsActivity::class.java))
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            sessionManager.clearSession()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }

        loadDashboardData()
    }

    private fun loadDashboardData() {
        val token = sessionManager.getAccessToken() ?: ""

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Fetch teachers
                val teachersReq = Request.Builder()
                    .url("http://10.0.2.2:8888/api/users/role/TEACHER")
                    .header("Authorization", "Bearer $token").get().build()
                val teachersBody = httpClient.newCall(teachersReq).execute().body?.string() ?: ""
                val teachersJson = gson.fromJson(teachersBody, com.google.gson.JsonObject::class.java)
                val totalTeachers = teachersJson.get("data")?.asJsonArray?.size() ?: 0

                // Fetch students
                val studentsReq = Request.Builder()
                    .url("http://10.0.2.2:8888/api/students")
                    .header("Authorization", "Bearer $token").get().build()
                val studentsBody = httpClient.newCall(studentsReq).execute().body?.string() ?: ""
                val studentsJson = gson.fromJson(studentsBody, com.google.gson.JsonObject::class.java)
                val totalStudents = studentsJson.get("data")?.asJsonArray?.size() ?: 0

                // Fetch classes
                val classesReq = Request.Builder()
                    .url("http://10.0.2.2:8888/api/classes")
                    .header("Authorization", "Bearer $token").get().build()
                val classesBody = httpClient.newCall(classesReq).execute().body?.string() ?: ""
                val classesJson = gson.fromJson(classesBody, com.google.gson.JsonObject::class.java)
                val classesArray = classesJson.get("data")?.asJsonArray
                val totalClasses = classesArray?.size() ?: 0
                val avgClassSize = if (totalClasses > 0 && totalStudents > 0)
                    totalStudents / totalClasses else 0

                withContext(Dispatchers.Main) {
                    // Mini stats in banner
                    findViewById<TextView>(R.id.tvMiniStudents).text  = totalStudents.toString()
                    findViewById<TextView>(R.id.tvMiniClasses).text   = totalClasses.toString()
                    findViewById<TextView>(R.id.tvMiniTeachers).text  = totalTeachers.toString()

                    // Stat cards
                    findViewById<TextView>(R.id.tvStatStudents).text  = totalStudents.toString()
                    findViewById<TextView>(R.id.tvStatTeachers).text  = totalTeachers.toString()
                    findViewById<TextView>(R.id.tvStatClasses).text   = totalClasses.toString()
                    findViewById<TextView>(R.id.tvStatAvgSize).text   = avgClassSize.toString()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getGreeting(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11  -> "GOOD MORNING"
            in 12..17 -> "GOOD AFTERNOON"
            else      -> "GOOD EVENING"
        }
    }
}