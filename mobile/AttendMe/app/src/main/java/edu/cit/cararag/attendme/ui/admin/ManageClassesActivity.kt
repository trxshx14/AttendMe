package edu.cit.cararag.attendme.ui.admin

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import edu.cit.cararag.attendme.R
import edu.cit.cararag.attendme.data.model.SchoolClass
import edu.cit.cararag.attendme.data.model.User
import edu.cit.cararag.attendme.data.repository.ClassRepository
import edu.cit.cararag.attendme.data.repository.UserRepository
import edu.cit.cararag.attendme.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ManageClassesActivity : AppCompatActivity() {

    private val classRepo   = ClassRepository()
    private val userRepo    = UserRepository()
    private val gson        = Gson()
    private val httpClient  = OkHttpClient()
    private val jsonType    = "application/json".toMediaType()

    private lateinit var adapter: ClassAdapter
    private lateinit var rvClasses: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: LinearLayout
    private lateinit var tvTotalClasses: TextView
    private lateinit var tvTotalStudents: TextView
    private lateinit var tvTotalTeachers: TextView
    private lateinit var etSearch: TextInputEditText

    private var allClasses   = listOf<SchoolClass>()
    private var teacherNames = mapOf<Long, String>()
    private var teachers     = listOf<User>()
    private lateinit var token: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_classes)

        val session = SessionManager(this)
        token = session.getAccessToken() ?: ""

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        rvClasses       = findViewById(R.id.rvClasses)
        progressBar     = findViewById(R.id.progressBar)
        emptyState      = findViewById(R.id.emptyState)
        tvTotalClasses  = findViewById(R.id.tvTotalClasses)
        tvTotalStudents = findViewById(R.id.tvTotalStudents)
        tvTotalTeachers = findViewById(R.id.tvTotalTeachers)
        etSearch        = findViewById(R.id.etSearch)

        adapter = ClassAdapter(emptyList(), emptyMap(),
            onEdit         = { showClassDialog(it) },
            onDelete       = { confirmDelete(it) },
            onViewStudents = { showStudentsDialog(it) }
        )
        rvClasses.layoutManager = LinearLayoutManager(this)
        rvClasses.adapter       = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filterClasses(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        findViewById<FloatingActionButton>(R.id.fabAddClass).setOnClickListener {
            showClassDialog(null)
        }

        loadData()
    }

    private fun loadData() {
        showLoading(true)
        lifecycleScope.launch {
            val teachersResult = userRepo.getUsersByRole("TEACHER")
            teachers     = teachersResult.getOrElse { emptyList() }
            teacherNames = teachers.associate { it.userId to (it.fullName ?: it.username ?: "") }

            val classesResult = classRepo.getAllClasses()
            allClasses = classesResult.getOrElse { emptyList() }

            withContext(Dispatchers.Main) {
                showLoading(false)
                tvTotalClasses.text  = allClasses.size.toString()
                tvTotalStudents.text = allClasses.sumOf { it.studentCount ?: 0 }.toString()
                tvTotalTeachers.text = teachers.size.toString()
                filterClasses(etSearch.text.toString())
            }
        }
    }

    private fun filterClasses(query: String) {
        val filtered = if (query.isBlank()) allClasses
        else allClasses.filter {
            it.className.contains(query, ignoreCase = true) ||
                    it.subject?.contains(query, ignoreCase = true) == true
        }
        adapter = ClassAdapter(filtered, teacherNames,
            onEdit         = { showClassDialog(it) },
            onDelete       = { confirmDelete(it) },
            onViewStudents = { showStudentsDialog(it) }
        )
        rvClasses.adapter     = adapter
        emptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        rvClasses.visibility  = if (filtered.isEmpty()) View.GONE    else View.VISIBLE
    }

    private fun showClassDialog(editClass: SchoolClass?) {
        val dialogView     = LayoutInflater.from(this).inflate(R.layout.dialog_class_form, null)
        val etClassName    = dialogView.findViewById<EditText>(R.id.etClassName)
        val etSubject      = dialogView.findViewById<EditText>(R.id.etSubject)
        val etSection      = dialogView.findViewById<EditText>(R.id.etSection)
        val etAcademicYear = dialogView.findViewById<EditText>(R.id.etAcademicYear)
        val etStartTime    = dialogView.findViewById<EditText>(R.id.etStartTime)
        val etEndTime      = dialogView.findViewById<EditText>(R.id.etEndTime)
        val spinnerDay     = dialogView.findViewById<Spinner>(R.id.spinnerDay)
        val spinnerTeacher = dialogView.findViewById<Spinner>(R.id.spinnerTeacher)

        // Days spinner
        val days = listOf("Select day", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        spinnerDay.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, days)

        // Teachers spinner
        val teacherList = listOf("Select a teacher") + teachers.map { it.fullName ?: it.username ?: "" }
        spinnerTeacher.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, teacherList)

        // Time pickers
        etStartTime.setOnClickListener {
            val c = java.util.Calendar.getInstance()
            android.app.TimePickerDialog(this, { _, h, m ->
                etStartTime.setText(String.format("%02d:%02d", h, m))
            }, c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE), true).show()
        }
        etEndTime.setOnClickListener {
            val c = java.util.Calendar.getInstance()
            android.app.TimePickerDialog(this, { _, h, m ->
                etEndTime.setText(String.format("%02d:%02d", h, m))
            }, c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE), true).show()
        }

        // Pre-fill if editing
        editClass?.let {
            etClassName.setText(it.className)
            etSubject.setText(it.subject)
            etSection.setText(it.section)
            etAcademicYear.setText(it.academicYear)
            etStartTime.setText(it.schedule?.split(" ")?.getOrNull(1) ?: "")
            etEndTime.setText(it.schedule?.split(" - ")?.getOrNull(1) ?: "")
            val dayIdx = days.indexOf(it.schedule?.split(" ")?.firstOrNull())
            if (dayIdx > 0) spinnerDay.setSelection(dayIdx)
            val teacherIdx = teachers.indexOfFirst { t -> t.userId == it.teacherId }
            if (teacherIdx >= 0) spinnerTeacher.setSelection(teacherIdx + 1)
        }

        // Default academic year
        if (etAcademicYear.text.isNullOrBlank()) {
            val y = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            etAcademicYear.setText("$y-${y + 1}")
        }

        AlertDialog.Builder(this)
            .setTitle(if (editClass == null) "Create New Class" else "Edit Class")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val className  = etClassName.text.toString().trim()
                val subject    = etSubject.text.toString().trim()
                val teacherIdx = spinnerTeacher.selectedItemPosition

                if (className.isBlank() || subject.isBlank() || teacherIdx == 0) {
                    Toast.makeText(this, "Class name, subject and teacher are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val selectedTeacher = teachers[teacherIdx - 1]
                val selectedDay = if (spinnerDay.selectedItemPosition > 0) days[spinnerDay.selectedItemPosition] else ""

                val data = mapOf(
                    "className"       to className,
                    "subject"         to subject,
                    "section"         to etSection.text.toString().trim(),
                    "academicYear"    to etAcademicYear.text.toString().trim(),
                    "scheduleDay"     to selectedDay,
                    "scheduleTime"    to etStartTime.text.toString().trim(),
                    "scheduleTimeEnd" to etEndTime.text.toString().trim(),
                    "teacherId"       to selectedTeacher.userId
                )
                saveClass(editClass?.classId, data)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveClass(classId: Long?, data: Map<String, Any?>) {
        showLoading(true)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url  = if (classId == null) "http://10.0.2.2:8888/api/classes"
                else "http://10.0.2.2:8888/api/classes/$classId"
                val body = gson.toJson(data).toRequestBody(jsonType)
                val req  = Request.Builder().url(url)
                    .header("Authorization", "Bearer $token")
                    .apply { if (classId == null) post(body) else put(body) }
                    .build()
                val success = httpClient.newCall(req).execute().isSuccessful
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@ManageClassesActivity,
                        if (success) "Saved!" else "Failed to save", Toast.LENGTH_SHORT).show()
                    if (success) loadData()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@ManageClassesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun confirmDelete(cls: SchoolClass) {
        AlertDialog.Builder(this)
            .setTitle("Delete Class")
            .setMessage("Delete ${cls.className}?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val req = Request.Builder()
                            .url("http://10.0.2.2:8888/api/classes/${cls.classId}")
                            .header("Authorization", "Bearer $token")
                            .delete().build()
                        val success = httpClient.newCall(req).execute().isSuccessful
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ManageClassesActivity,
                                if (success) "Deleted" else "Failed", Toast.LENGTH_SHORT).show()
                            if (success) loadData()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ManageClassesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showStudentsDialog(cls: SchoolClass) {
        showLoading(true)
        lifecycleScope.launch {
            val students = classRepo.getStudentsInClass(cls.classId).getOrElse { emptyList() }
            withContext(Dispatchers.Main) {
                showLoading(false)
                val msg = if (students.isEmpty()) "No students enrolled yet."
                else students.joinToString("\n") {
                    "• ${it.fullName ?: "${it.firstName} ${it.lastName}"} (${it.rollNumber ?: "—"})"
                }
                AlertDialog.Builder(this@ManageClassesActivity)
                    .setTitle("Students — ${cls.className}")
                    .setMessage(msg)
                    .setPositiveButton("Add Student") { _, _ -> showAddStudentDialog(cls) }
                    .setNegativeButton("Close", null)
                    .show()
            }
        }
    }

    private fun showAddStudentDialog(cls: SchoolClass) {
        val view         = LayoutInflater.from(this).inflate(R.layout.dialog_add_student, null)
        val etFirstName  = view.findViewById<EditText>(R.id.etFirstName)
        val etLastName   = view.findViewById<EditText>(R.id.etLastName)
        val etRollNumber = view.findViewById<EditText>(R.id.etRollNumber)
        val etEmail      = view.findViewById<EditText>(R.id.etEmail)

        AlertDialog.Builder(this)
            .setTitle("Add Student to ${cls.className}")
            .setView(view)
            .setPositiveButton("Add") { _, _ ->
                val firstName  = etFirstName.text.toString().trim()
                val lastName   = etLastName.text.toString().trim()
                val rollNumber = etRollNumber.text.toString().trim()
                if (firstName.isBlank() || lastName.isBlank() || rollNumber.isBlank()) {
                    Toast.makeText(this, "First name, last name, roll number required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val data = mapOf(
                            "firstName"  to firstName,
                            "lastName"   to lastName,
                            "rollNumber" to rollNumber,
                            "email"      to etEmail.text.toString().trim().ifBlank { null },
                            "classId"    to cls.classId
                        )
                        val body = gson.toJson(data).toRequestBody(jsonType)
                        val req  = Request.Builder().url("http://10.0.2.2:8888/api/students")
                            .header("Authorization", "Bearer $token").post(body).build()
                        val success = httpClient.newCall(req).execute().isSuccessful
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ManageClassesActivity,
                                if (success) "Student added!" else "Failed", Toast.LENGTH_SHORT).show()
                            if (success) loadData()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ManageClassesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        rvClasses.visibility   = if (show) View.GONE    else View.VISIBLE
    }
}