package edu.cit.cararag.attendme.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import edu.cit.cararag.attendme.R
import edu.cit.cararag.attendme.data.repository.ClassRepository
import edu.cit.cararag.attendme.data.repository.UserRepository
import edu.cit.cararag.attendme.ui.login.LoginActivity
import edu.cit.cararag.attendme.utils.SessionManager
import kotlinx.coroutines.launch
import java.util.Calendar

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private val classRepository = ClassRepository()
    private val userRepository  = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        sessionManager = SessionManager(this)

        // Set greeting and name
        val name = sessionManager.getFullName() ?: "Administrator"
        findViewById<TextView>(R.id.tvGreeting).text   = getGreeting()
        findViewById<TextView>(R.id.tvAdminName).text  = name.split(" ").firstOrNull() ?: name

        // Quick action clicks
        findViewById<androidx.cardview.widget.CardView>(R.id.cardManageUsers).setOnClickListener {
            startActivity(Intent(this, ManageUsersActivity::class.java))
        }
        findViewById<androidx.cardview.widget.CardView>(R.id.cardManageClasses).setOnClickListener {
            startActivity(Intent(this, ManageClassesActivity::class.java))
        }
        findViewById<androidx.cardview.widget.CardView>(R.id.cardReports).setOnClickListener {
            // TODO: ReportsActivity
        }

        // Logout
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            sessionManager.clearSession()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }

        // Load dashboard data
        loadDashboardData()
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            // Load teachers
            val teachersResult = userRepository.getUsersByRole("TEACHER")
            val teachers = teachersResult.getOrElse { emptyList() }

            // Load classes
            val classesResult = classRepository.getAllClasses()
            val classes = classesResult.getOrElse { emptyList() }

            // Calculate stats
            val totalTeachers = teachers.size
            val totalClasses  = classes.size
            val totalStudents = classes.sumOf { it.studentCount ?: 0 }
            val avgClassSize  = if (totalClasses > 0) totalStudents / totalClasses else 0

            // Update UI
            runOnUiThread {
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