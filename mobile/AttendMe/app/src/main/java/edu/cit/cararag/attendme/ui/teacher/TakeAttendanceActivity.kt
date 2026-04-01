package edu.cit.cararag.attendme.ui.teacher

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import edu.cit.cararag.attendme.R
import edu.cit.cararag.attendme.data.model.ClassDetail
import edu.cit.cararag.attendme.data.model.StudentAttendance
import edu.cit.cararag.attendme.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class TakeAttendanceActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private val httpClient = OkHttpClient()
    private val gson = Gson()

    // Data State
    private var allTeacherClasses = mutableListOf<ClassDetail>()
    private var filteredSections = mutableListOf<ClassDetail>()
    private var studentList = mutableListOf<StudentAttendance>()
    private var selectedClass: ClassDetail? = null
    private var selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // UI Components
    private lateinit var spinnerGrade: Spinner
    private lateinit var spinnerSection: Spinner
    private lateinit var btnDatePicker: Button
    private lateinit var rvStudents: RecyclerView
    private lateinit var adapter: StudentAttendanceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_take_attendance)

        sessionManager = SessionManager(this)
        initUI()
        fetchTeacherClasses()
    }

    private fun initUI() {
        spinnerGrade = findViewById(R.id.spinnerGrade)
        spinnerSection = findViewById(R.id.spinnerSection)
        btnDatePicker = findViewById(R.id.btnDatePicker)
        rvStudents = findViewById(R.id.rvStudents)

        btnDatePicker.text = selectedDate
        btnDatePicker.setOnClickListener { showDatePicker() }

        adapter = StudentAttendanceAdapter(studentList) { updateCounts() }
        rvStudents.layoutManager = LinearLayoutManager(this)
        rvStudents.adapter = adapter

        findViewById<Button>(R.id.btnSaveAttendance).setOnClickListener { handleSave() }
        findViewById<Button>(R.id.btnMarkPresentAll).setOnClickListener { markAll("present") }
        findViewById<Button>(R.id.btnMarkAbsentAll).setOnClickListener { markAll("absent") }
    }

    private fun fetchTeacherClasses() {
        val teacherId = sessionManager.getUserId()
        val token = sessionManager.getAccessToken()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url("http://10.0.2.2:8888/api/classes/teacher/$teacherId")
                    .header("Authorization", "Bearer $token").get().build()

                val res = httpClient.newCall(req).execute().body?.string() ?: ""
                val json = JSONObject(res)
                val dataArray = json.getJSONArray("data")

                val classes = mutableListOf<ClassDetail>()
                for (i in 0 until dataArray.length()) {
                    val obj = dataArray.getJSONObject(i)

                    // FIXED: Properly closed the ClassDetail constructor and the loop
                    classes.add(ClassDetail(
                        classId = obj.getInt("classId"),
                        className = obj.optString("className"),       // ADDED
                        gradeLevel = obj.optString("gradeLevel"),
                        section = obj.optString("section"),
                        subject = obj.optString("subject"),
                        scheduleDay = obj.optString("scheduleDay"),   // ADDED
                        scheduleTime = obj.optString("scheduleTime"), // ADDED
                        scheduleTimeEnd = obj.optString("scheduleTimeEnd"), // ADDED
                        studentCount = obj.optInt("studentCount")
                    ))
                }

                withContext(Dispatchers.Main) {
                    allTeacherClasses = classes
                    setupGradeSpinner()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupGradeSpinner() {
        val grades = allTeacherClasses.map { "Grade ${it.gradeLevel}" }.distinct().sorted()
        val gradeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, grades)
        spinnerGrade.adapter = gradeAdapter

        spinnerGrade.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                val selectedGrade = grades[pos].replace("Grade ", "")
                setupSectionSpinner(selectedGrade)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun setupSectionSpinner(grade: String) {
        filteredSections = allTeacherClasses.filter { it.gradeLevel == grade }.toMutableList()
        val sections = filteredSections.map { "${it.section} (${it.subject})" }
        val sectionAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sections)
        spinnerSection.adapter = sectionAdapter

        spinnerSection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                selectedClass = filteredSections[pos]
                fetchStudents(selectedClass!!.classId)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun fetchStudents(classId: Int) {
        val token = sessionManager.getAccessToken()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val studReq = Request.Builder()
                    .url("http://10.0.2.2:8888/api/classes/$classId/students")
                    .header("Authorization", "Bearer $token").get().build()
                val studRes = httpClient.newCall(studReq).execute().body?.string() ?: ""
                val studArray = JSONObject(studRes).getJSONArray("data")

                val attReq = Request.Builder()
                    .url("http://10.0.2.2:8888/api/attendance/class/$classId/date/$selectedDate")
                    .header("Authorization", "Bearer $token").get().build()
                val attRes = httpClient.newCall(attReq).execute().body?.string() ?: ""
                val attData = JSONObject(attRes).optJSONArray("data")

                val existingMap = mutableMapOf<Int, String>()
                attData?.let {
                    for (i in 0 until it.length()) {
                        val a = it.getJSONObject(i)
                        existingMap[a.getInt("studentId")] = a.getString("status").lowercase()
                    }
                }

                val list = mutableListOf<StudentAttendance>()
                for (i in 0 until studArray.length()) {
                    val s = studArray.getJSONObject(i)
                    val id = s.getInt("studentId")
                    list.add(StudentAttendance(
                        id, s.getString("firstName"), s.getString("lastName"), s.optString("rollNumber"),
                        status = existingMap[id] ?: "present"
                    ))
                }

                withContext(Dispatchers.Main) {
                    studentList.clear()
                    studentList.addAll(list)
                    adapter.notifyDataSetChanged()
                    updateCounts()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val calendar = Calendar.getInstance()
            calendar.set(y, m, d)
            selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            btnDatePicker.text = selectedDate
            selectedClass?.let { fetchStudents(it.classId) }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun markAll(status: String) {
        studentList.forEach { it.status = status }
        adapter.notifyDataSetChanged()
        updateCounts()
    }

    private fun updateCounts() {
        val p = studentList.count { it.status == "present" }
        val a = studentList.count { it.status == "absent" }
        val l = studentList.count { it.status == "late" }

        // Added a null check for safety
        val tvCounts = findViewById<TextView>(R.id.tvCounts)
        tvCounts?.text = "P: $p | A: $a | L: $l | Total: ${studentList.size}"
    }

    private fun handleSave() {
        if (selectedClass == null) return
        val token = sessionManager.getAccessToken()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val attData = JSONObject()
                studentList.forEach { attData.put(it.studentId.toString(), it.status) }

                val payload = JSONObject().apply {
                    put("classId", selectedClass!!.classId)
                    put("date", selectedDate)
                    put("attendanceData", attData)
                    put("markedById", sessionManager.getUserId())
                }

                val body = payload.toString().toRequestBody("application/json".toMediaType())
                val req = Request.Builder().url("http://10.0.2.2:8888/api/attendance/bulk")
                    .header("Authorization", "Bearer $token").post(body).build()

                val response = httpClient.newCall(req).execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@TakeAttendanceActivity, "Attendance Saved!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@TakeAttendanceActivity, "Failed to save", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}