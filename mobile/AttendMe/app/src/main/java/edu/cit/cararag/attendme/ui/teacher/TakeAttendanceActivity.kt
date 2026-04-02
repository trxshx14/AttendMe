package edu.cit.cararag.attendme.ui.teacher

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import edu.cit.cararag.attendme.R
import edu.cit.cararag.attendme.data.model.SchoolClass
import edu.cit.cararag.attendme.data.model.Student
import edu.cit.cararag.attendme.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*

class TakeAttendanceActivity : AppCompatActivity() {

    private val httpClient = OkHttpClient()
    private val gson       = Gson()
    private val jsonType   = "application/json".toMediaType()
    private lateinit var token: String
    private lateinit var sessionManager: SessionManager

    private lateinit var spinnerClass: Spinner
    private lateinit var etDate: EditText
    private lateinit var etSearch: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutPlaceholder: LinearLayout
    private lateinit var tvPlaceholderTitle: TextView
    private lateinit var tvPlaceholderSub: TextView
    private lateinit var layoutSummaryStrip: LinearLayout
    private lateinit var layoutMarkAll: LinearLayout
    private lateinit var rvStudents: RecyclerView
    private lateinit var btnSave: MaterialButton
    private lateinit var tvError: TextView

    private lateinit var chipPresent: TextView
    private lateinit var chipAbsent: TextView
    private lateinit var chipLate: TextView
    private lateinit var chipExcused: TextView

    private var classes = listOf<SchoolClass>()
    private var allStudents = listOf<Student>()
    private var selectedClass: SchoolClass? = null
    private var selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    private val attendanceMap = mutableMapOf<Long, String>()
    private lateinit var studentAdapter: StudentAttendanceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_take_attendance)

        sessionManager = SessionManager(this)
        token = sessionManager.getAccessToken() ?: ""

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // Bind views
        spinnerClass       = findViewById(R.id.spinnerClass)
        etDate             = findViewById(R.id.etDate)
        etSearch           = findViewById(R.id.etSearch)
        progressBar        = findViewById(R.id.progressBar)
        layoutPlaceholder  = findViewById(R.id.layoutPlaceholder)
        tvPlaceholderTitle = findViewById(R.id.tvPlaceholderTitle)
        tvPlaceholderSub   = findViewById(R.id.tvPlaceholderSub)
        layoutSummaryStrip = findViewById(R.id.layoutSummaryStrip)
        layoutMarkAll      = findViewById(R.id.layoutMarkAll)
        rvStudents         = findViewById(R.id.rvStudents)
        btnSave            = findViewById(R.id.btnSave)
        tvError            = findViewById(R.id.tvError)
        chipPresent        = findViewById(R.id.chipPresent)
        chipAbsent         = findViewById(R.id.chipAbsent)
        chipLate           = findViewById(R.id.chipLate)
        chipExcused        = findViewById(R.id.chipExcused)

        // Set default date
        etDate.setText(selectedDate)
        etDate.setOnClickListener { showDatePicker() }

        // Setup RecyclerView
        studentAdapter = StudentAttendanceAdapter(emptyList(), attendanceMap) { _, _ ->
            updateSummaryChips()
        }
        rvStudents.layoutManager = LinearLayoutManager(this)
        rvStudents.adapter = studentAdapter

        // Class spinner
        spinnerClass.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (pos == 0) {
                    selectedClass = null
                    showPlaceholder("Select a Class to Begin",
                        "Choose a class above to load students and start marking attendance.")
                    return
                }
                selectedClass = classes[pos - 1]
                loadStudents()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Search
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filterStudents(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Mark all buttons
        findViewById<MaterialButton>(R.id.btnMarkAllPresent).setOnClickListener { markAll("present") }
        findViewById<MaterialButton>(R.id.btnMarkAllAbsent).setOnClickListener  { markAll("absent") }
        findViewById<MaterialButton>(R.id.btnMarkAllLate).setOnClickListener    { markAll("late") }
        findViewById<MaterialButton>(R.id.btnMarkAllExcused).setOnClickListener { markAll("excused") }

        // Save
        btnSave.setOnClickListener { saveAttendance() }

        // Check if launched from dashboard with pre-selected class
        val preClassId = intent.getLongExtra("classId", -1L)
        loadClasses(preClassId)
    }

    private fun loadClasses(preSelectId: Long = -1L) {
        val teacherId = sessionManager.getUserId()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url("http://10.0.2.2:8888/api/classes/teacher/$teacherId")
                    .header("Authorization", "Bearer $token").get().build()
                val body = httpClient.newCall(req).execute().body?.string() ?: ""
                val json = gson.fromJson(body, com.google.gson.JsonObject::class.java)
                val type = object : TypeToken<List<SchoolClass>>() {}.type
                val list: List<SchoolClass> = if (json.get("success")?.asBoolean == true)
                    gson.fromJson(json.get("data").asJsonArray, type) else emptyList()

                withContext(Dispatchers.Main) {
                    classes = list
                    val names = listOf("Select a class") + list.map {
                        "${it.className ?: ""}${it.section?.let { s -> " — $s" } ?: ""} · ${it.subject ?: ""}"
                    }
                    spinnerClass.adapter = ArrayAdapter(
                        this@TakeAttendanceActivity,
                        android.R.layout.simple_spinner_dropdown_item, names
                    )
                    // Pre-select if passed from dashboard
                    if (preSelectId != -1L) {
                        val idx = list.indexOfFirst { it.classId == preSelectId }
                        if (idx >= 0) spinnerClass.setSelection(idx + 1)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { showError("Failed to load classes: ${e.message}") }
            }
        }
    }

    private fun loadStudents() {
        val cls = selectedClass ?: return
        showLoading(true)
        attendanceMap.clear()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Fetch students in class
                val studReq = Request.Builder()
                    .url("http://10.0.2.2:8888/api/classes/${cls.classId}/students")
                    .header("Authorization", "Bearer $token").get().build()
                val studBody = httpClient.newCall(studReq).execute().body?.string() ?: ""
                val studJson = gson.fromJson(studBody, com.google.gson.JsonObject::class.java)
                val studType = object : TypeToken<List<Student>>() {}.type
                val students: List<Student> = if (studJson.get("success")?.asBoolean == true)
                    gson.fromJson(studJson.get("data").asJsonArray, studType) else emptyList()

                // Fetch existing attendance for this date
                val attReq = Request.Builder()
                    .url("http://10.0.2.2:8888/api/attendance/class/${cls.classId}/date/$selectedDate")
                    .header("Authorization", "Bearer $token").get().build()
                val attBody = httpClient.newCall(attReq).execute().body?.string() ?: ""
                val attJson = gson.fromJson(attBody, com.google.gson.JsonObject::class.java)
                val existingMap = mutableMapOf<Long, String>()
                attJson.get("data")?.asJsonArray?.forEach { el ->
                    val obj = el.asJsonObject
                    val sid = obj.get("studentId")?.asLong ?: return@forEach
                    val st  = obj.get("status")?.asString?.lowercase() ?: "present"
                    existingMap[sid] = st
                }

                // Initialize attendance map — existing or default present
                students.forEach { s ->
                    attendanceMap[s.studentId] = existingMap[s.studentId] ?: "present"
                }

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    allStudents = students
                    if (students.isEmpty()) {
                        showPlaceholder("No Students Enrolled",
                            "This class has no students yet. Add students from Manage Classes.")
                        layoutSummaryStrip.visibility = View.GONE
                        layoutMarkAll.visibility      = View.GONE
                        btnSave.visibility            = View.GONE
                    } else {
                        layoutPlaceholder.visibility  = View.GONE
                        layoutSummaryStrip.visibility = View.VISIBLE
                        layoutMarkAll.visibility      = View.VISIBLE
                        rvStudents.visibility         = View.VISIBLE
                        btnSave.visibility            = View.VISIBLE
                        studentAdapter.updateStudents(students)
                        updateSummaryChips()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    showError("Failed to load students: ${e.message}")
                }
            }
        }
    }

    private fun filterStudents(query: String) {
        val filtered = if (query.isBlank()) allStudents
        else allStudents.filter {
            val name = "${it.firstName} ${it.lastName}".lowercase()
            name.contains(query.lowercase()) ||
                    it.rollNumber?.contains(query, ignoreCase = true) == true
        }
        studentAdapter.updateStudents(filtered)
    }

    private fun markAll(status: String) {
        allStudents.forEach { attendanceMap[it.studentId] = status }
        studentAdapter.updateStudents(allStudents) // rebind to refresh UI
        updateSummaryChips()
    }

    private fun updateSummaryChips() {
        val present = attendanceMap.values.count { it == "present" }
        val absent  = attendanceMap.values.count { it == "absent" }
        val late    = attendanceMap.values.count { it == "late" }
        val excused = attendanceMap.values.count { it == "excused" }
        chipPresent.text = "✓ $present\nPresent"
        chipAbsent.text  = "✗ $absent\nAbsent"
        chipLate.text    = "⏰ $late\nLate"
        chipExcused.text = "📄 $excused\nExcused"
    }

    private fun saveAttendance() {
        val cls = selectedClass ?: return
        btnSave.isEnabled = false
        btnSave.text      = "Saving..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Build attendanceData map: { "studentId": "status" }
                val attendanceData = mutableMapOf<String, String>()
                allStudents.forEach { s ->
                    attendanceData[s.studentId.toString()] = attendanceMap[s.studentId] ?: "present"
                }

                val payload = mapOf(
                    "classId"        to cls.classId,
                    "date"           to selectedDate,
                    "attendanceData" to attendanceData,
                    "markedById"     to sessionManager.getUserId(),
                    "remarks"        to ""
                )

                val req = Request.Builder()
                    .url("http://10.0.2.2:8888/api/attendance/bulk")
                    .header("Authorization", "Bearer $token")
                    .post(gson.toJson(payload).toRequestBody(jsonType))
                    .build()
                val response = httpClient.newCall(req).execute()
                val success  = response.isSuccessful

                withContext(Dispatchers.Main) {
                    if (success) {
                        btnSave.text = "✅ Saved!"
                        btnSave.backgroundTintList =
                            android.content.res.ColorStateList.valueOf(
                                android.graphics.Color.parseColor("#10B981"))
                        // Re-enable after 2 seconds
                        btnSave.postDelayed({
                            btnSave.isEnabled = true
                            btnSave.text = "💾  Save Attendance"
                            btnSave.backgroundTintList =
                                android.content.res.ColorStateList.valueOf(
                                    android.graphics.Color.parseColor("#0F2D5E"))
                        }, 2000)
                        Toast.makeText(this@TakeAttendanceActivity,
                            "Attendance saved!", Toast.LENGTH_SHORT).show()
                    } else {
                        btnSave.isEnabled = true
                        btnSave.text = "💾  Save Attendance"
                        showError("Failed to save attendance")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnSave.isEnabled = true
                    btnSave.text = "💾  Save Attendance"
                    showError("Error: ${e.message}")
                }
            }
        }
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            selectedDate = String.format("%04d-%02d-%02d", y, m + 1, d)
            etDate.setText(selectedDate)
            // Reload attendance for new date if class already selected
            if (selectedClass != null) loadStudents()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showPlaceholder(title: String, sub: String) {
        layoutPlaceholder.visibility  = View.VISIBLE
        rvStudents.visibility         = View.GONE
        layoutSummaryStrip.visibility = View.GONE
        layoutMarkAll.visibility      = View.GONE
        btnSave.visibility            = View.GONE
        tvPlaceholderTitle.text = title
        tvPlaceholderSub.text   = sub
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility    = if (show) View.VISIBLE else View.GONE
        layoutPlaceholder.visibility = if (show) View.GONE else layoutPlaceholder.visibility
    }

    private fun showError(msg: String) {
        tvError.text = msg
        tvError.visibility = View.VISIBLE
    }
}