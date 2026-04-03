package edu.cit.cararag.attendme.ui.teacher

import android.app.DatePickerDialog
import android.os.Bundle
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
import edu.cit.cararag.attendme.data.model.StudentAttendance
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

class TeacherReportsActivity : AppCompatActivity() {

    private val httpClient = OkHttpClient()
    private val gson       = Gson()
    private lateinit var token: String
    private lateinit var sessionManager: SessionManager
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private lateinit var spinnerClass: Spinner
    private lateinit var spinnerPeriod: Spinner
    private lateinit var layoutCustomDates: LinearLayout
    private lateinit var etCustomFrom: EditText
    private lateinit var etCustomTo: EditText
    private lateinit var tvDateRangePreview: TextView
    private lateinit var btnGenerate: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var layoutPlaceholder: LinearLayout
    private lateinit var layoutReport: LinearLayout
    private lateinit var rvStudentBreakdown: RecyclerView

    private lateinit var tvSumTotal: TextView
    private lateinit var tvSumPresent: TextView
    private lateinit var tvSumAbsent: TextView
    private lateinit var tvSumLate: TextView
    private lateinit var tvSumExcused: TextView
    private lateinit var tvSumPresentPct: TextView
    private lateinit var tvSumAbsentPct: TextView
    private lateinit var tvSumLatePct: TextView
    private lateinit var tvSumExcusedPct: TextView
    private lateinit var tvAvgAttendance: TextView
    private lateinit var viewRateBar: View

    private var classes = listOf<SchoolClass>()
    private lateinit var breakdownAdapter: StudentBreakdownAdapter

    private val periodOptions = listOf(
        "This Week", "Last Week", "Last 2 Weeks", "This Month", "Custom Range"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_reports)

        sessionManager = SessionManager(this)
        token = sessionManager.getAccessToken() ?: ""

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // Bind views
        spinnerClass       = findViewById(R.id.spinnerClass)
        spinnerPeriod      = findViewById(R.id.spinnerPeriod)
        layoutCustomDates  = findViewById(R.id.layoutCustomDates)
        etCustomFrom       = findViewById(R.id.etCustomFrom)
        etCustomTo         = findViewById(R.id.etCustomTo)
        tvDateRangePreview = findViewById(R.id.tvDateRangePreview)
        btnGenerate        = findViewById(R.id.btnGenerate)
        progressBar        = findViewById(R.id.progressBar)
        tvError            = findViewById(R.id.tvError)
        layoutPlaceholder  = findViewById(R.id.layoutPlaceholder)
        layoutReport       = findViewById(R.id.layoutReport)
        rvStudentBreakdown = findViewById(R.id.rvStudentBreakdown)
        tvSumTotal         = findViewById(R.id.tvSumTotal)
        tvSumPresent       = findViewById(R.id.tvSumPresent)
        tvSumAbsent        = findViewById(R.id.tvSumAbsent)
        tvSumLate          = findViewById(R.id.tvSumLate)
        tvSumExcused       = findViewById(R.id.tvSumExcused)
        tvSumPresentPct    = findViewById(R.id.tvSumPresentPct)
        tvSumAbsentPct     = findViewById(R.id.tvSumAbsentPct)
        tvSumLatePct       = findViewById(R.id.tvSumLatePct)
        tvSumExcusedPct    = findViewById(R.id.tvSumExcusedPct)
        tvAvgAttendance    = findViewById(R.id.tvAvgAttendance)
        viewRateBar        = findViewById(R.id.viewRateBar)

        // Period spinner
        spinnerPeriod.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item, periodOptions)
        spinnerPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val isCustom = pos == 4
                layoutCustomDates.visibility = if (isCustom) View.VISIBLE else View.GONE
                updateDatePreview()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        // Custom date pickers
        etCustomFrom.setOnClickListener { showDatePicker(etCustomFrom) { updateDatePreview() } }
        etCustomTo.setOnClickListener   { showDatePicker(etCustomTo)   { updateDatePreview() } }

        // RecyclerView
        breakdownAdapter = StudentBreakdownAdapter(emptyList())
        rvStudentBreakdown.layoutManager = LinearLayoutManager(this)
        rvStudentBreakdown.adapter = breakdownAdapter

        // Generate
        btnGenerate.setOnClickListener { generateReport() }

        // Set default dates
        etCustomFrom.setText(sdf.format(Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000)))
        etCustomTo.setText(sdf.format(Date()))
        updateDatePreview()

        loadClasses()
    }

    private fun loadClasses() {
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
                    val names = listOf("All Classes") + list.map {
                        "${it.className ?: ""}${it.section?.let { s -> " — $s" } ?: ""}"
                    }
                    spinnerClass.adapter = ArrayAdapter(this@TeacherReportsActivity,
                        android.R.layout.simple_spinner_dropdown_item, names)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { showError("Failed to load classes") }
            }
        }
    }

    private fun generateReport() {
        val (from, to) = getDateRange()
        if (from.isBlank() || to.isBlank()) {
            showError("Please select a valid date range")
            return
        }

        val classIdx = spinnerClass.selectedItemPosition
        val classesToQuery = if (classIdx == 0) classes else listOfNotNull(classes.getOrNull(classIdx - 1))
        if (classesToQuery.isEmpty()) {
            showError("No classes available")
            return
        }

        showLoading(true)
        tvError.visibility = View.GONE

        // Build date list
        val dates = mutableListOf<String>()
        var cur = sdf.parse(from) ?: Date()
        val end = sdf.parse(to) ?: Date()
        while (!cur.after(end)) {
            dates.add(sdf.format(cur))
            cur = Date(cur.time + 24 * 60 * 60 * 1000)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fetches = classesToQuery.flatMap { cls ->
                    dates.map { date ->
                        async {
                            try {
                                val req = Request.Builder()
                                    .url("http://10.0.2.2:8888/api/attendance/class/${cls.classId}/date/$date")
                                    .header("Authorization", "Bearer $token").get().build()
                                val body = httpClient.newCall(req).execute().body?.string() ?: ""
                                val json = gson.fromJson(body, com.google.gson.JsonObject::class.java)

                                if (json.get("success")?.asBoolean == true) {
                                    // CHANGE 1: Update TypeToken to use StudentAttendance
                                    val type = object : TypeToken<List<StudentAttendance>>() {}.type
                                    gson.fromJson<List<StudentAttendance>>(json.get("data").asJsonArray, type)
                                        .map { it.copy(status = it.status.lowercase()) }
                                } else emptyList()
                            } catch (e: Exception) { emptyList<StudentAttendance>() }
                        }
                    }
                }.awaitAll()

                val allRecords = fetches.flatten()

                // Summary logic
                var present = 0; var absent = 0; var late = 0; var excused = 0
                allRecords.forEach {
                    when (it.status.lowercase()) {
                        "present" -> present++
                        "absent"  -> absent++
                        "late"    -> late++
                        "excused" -> excused++
                    }
                }
                val total = allRecords.size
                val avg   = if (total > 0) ((present + late) * 100 / total) else 0

                // Per-student breakdown
                val studentMap = mutableMapOf<Int, StudentReportRow>()
                allRecords.forEach { r ->
                    val sid = r.studentId
                    val name = r.fullName ?: ""
                    val parts = name.trim().split("\\s+".toRegex())
                    val fn = parts.dropLast(1).joinToString(" ").ifBlank { name }
                    val ln = if (parts.size > 1) parts.last() else ""
                    val existing = studentMap[sid] ?: StudentReportRow(
                        sid.toLong(),
                        fn,
                        ln,
                        r.rollNumber ?: "",
                        r.className ?: "",
                        0, 0, 0, 0
                    )
                    if (r != null) {
                        studentMap[sid.toInt()] = when (r.status) {
                            "present" -> existing.copy(present = existing.present + 1)
                            "absent"  -> existing.copy(absent  = existing.absent  + 1)
                            "late"    -> existing.copy(late    = existing.late    + 1)
                            "excused" -> existing.copy(excused = existing.excused + 1)
                            else      -> existing
                        }
                    }
                }
                val rows = studentMap.values
                    .sortedWith(compareBy({ it.lastName }, { it.firstName }))

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    layoutPlaceholder.visibility = View.GONE
                    layoutReport.visibility      = View.VISIBLE

                    // Summary cards
                    tvSumTotal.text   = total.toString()
                    tvSumPresent.text = present.toString()
                    tvSumAbsent.text  = absent.toString()
                    tvSumLate.text    = late.toString()
                    tvSumExcused.text = excused.toString()
                    tvSumPresentPct.text = "${if (total > 0) present * 100 / total else 0}%"
                    tvSumAbsentPct.text  = "${if (total > 0) absent  * 100 / total else 0}%"
                    tvSumLatePct.text    = "${if (total > 0) late    * 100 / total else 0}%"
                    tvSumExcusedPct.text = "${if (total > 0) excused * 100 / total else 0}%"
                    tvAvgAttendance.text = "$avg%"

                    // Rate bar
                    viewRateBar.post {
                        val parent = viewRateBar.parent as FrameLayout
                        val params = viewRateBar.layoutParams
                        params.width = (parent.width * avg / 100)
                        viewRateBar.layoutParams = params
                    }

                    // Student breakdown
                    breakdownAdapter.updateData(rows)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    showError("Failed to generate report: ${e.message}")
                }
            }
        }
    }

    private fun getDateRange(): Pair<String, String> {
        val today = Calendar.getInstance()
        return when (spinnerPeriod.selectedItemPosition) {
            0 -> { // This week (Mon–Sun)
                val mon = today.clone() as Calendar
                mon.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val sun = mon.clone() as Calendar
                sun.add(Calendar.DAY_OF_WEEK, 6)
                Pair(sdf.format(mon.time), sdf.format(sun.time))
            }
            1 -> { // Last week
                val mon = today.clone() as Calendar
                mon.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                mon.add(Calendar.DAY_OF_WEEK, -7)
                val sun = mon.clone() as Calendar
                sun.add(Calendar.DAY_OF_WEEK, 6)
                Pair(sdf.format(mon.time), sdf.format(sun.time))
            }
            2 -> { // Last 2 weeks
                val mon = today.clone() as Calendar
                mon.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                mon.add(Calendar.DAY_OF_WEEK, -14)
                val sun = today.clone() as Calendar
                sun.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                Pair(sdf.format(mon.time), sdf.format(sun.time))
            }
            3 -> { // This month
                val first = today.clone() as Calendar
                first.set(Calendar.DAY_OF_MONTH, 1)
                val last = today.clone() as Calendar
                last.set(Calendar.DAY_OF_MONTH, last.getActualMaximum(Calendar.DAY_OF_MONTH))
                Pair(sdf.format(first.time), sdf.format(last.time))
            }
            4 -> { // Custom
                Pair(etCustomFrom.text.toString(), etCustomTo.text.toString())
            }
            else -> Pair("", "")
        }
    }

    private fun updateDatePreview() {
        val (from, to) = getDateRange()
        if (from.isNotBlank() && to.isNotBlank()) {
            tvDateRangePreview.text = "📅 ${formatDisplay(from)} — ${formatDisplay(to)}"
        }
    }

    private fun formatDisplay(date: String): String {
        return try {
            val out = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            out.format(sdf.parse(date)!!)
        } catch (e: Exception) { date }
    }

    private fun showDatePicker(field: EditText, onSet: () -> Unit) {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            field.setText(String.format("%04d-%02d-%02d", y, m + 1, d))
            onSet()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnGenerate.isEnabled  = !show
        btnGenerate.text       = if (show) "Generating..." else "📊 Generate Report"
    }

    private fun showError(msg: String) {
        tvError.text = msg
        tvError.visibility = View.VISIBLE
    }
}