package edu.cit.cararag.attendme.ui.admin

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
import edu.cit.cararag.attendme.data.model.Attendance
import edu.cit.cararag.attendme.data.model.SchoolClass
import edu.cit.cararag.attendme.data.repository.ClassRepository
import edu.cit.cararag.attendme.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.*

class AdminReportsActivity : AppCompatActivity() {

    private val classRepo  = ClassRepository()
    private val httpClient = OkHttpClient()
    private val gson       = Gson()
    private lateinit var token: String

    private lateinit var btnDaily: MaterialButton
    private lateinit var btnWeekly: MaterialButton
    private lateinit var spinnerClass: Spinner
    private lateinit var tvDateLabel: TextView
    private lateinit var etStartDate: EditText
    private lateinit var tvEndDateLabel: TextView
    private lateinit var etEndDate: EditText
    private lateinit var btnGenerate: MaterialButton
    private lateinit var tvError: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutSummary: LinearLayout
    private lateinit var cardRecords: androidx.cardview.widget.CardView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var rvRecords: RecyclerView
    private lateinit var tvRecordsTitle: TextView

    private lateinit var tvTotal: TextView
    private lateinit var tvPresent: TextView
    private lateinit var tvAbsent: TextView
    private lateinit var tvLate: TextView
    private lateinit var tvExcused: TextView
    private lateinit var tvPresentRate: TextView
    private lateinit var tvAbsentRate: TextView
    private lateinit var tvLateRate: TextView
    private lateinit var tvExcusedRate: TextView

    private var allClasses  = listOf<SchoolClass>()
    private var isDaily     = true
    private val dateFormat  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private lateinit var recordAdapter: AttendanceRecordAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_reports)

        token = SessionManager(this).getAccessToken() ?: ""

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // Bind views
        btnDaily       = findViewById(R.id.btnDaily)
        btnWeekly      = findViewById(R.id.btnWeekly)
        spinnerClass   = findViewById(R.id.spinnerClass)
        tvDateLabel    = findViewById(R.id.tvDateLabel)
        etStartDate    = findViewById(R.id.etStartDate)
        tvEndDateLabel = findViewById(R.id.tvEndDateLabel)
        etEndDate      = findViewById(R.id.etEndDate)
        btnGenerate    = findViewById(R.id.btnGenerate)
        tvError        = findViewById(R.id.tvError)
        progressBar    = findViewById(R.id.progressBar)
        layoutSummary  = findViewById(R.id.layoutSummary)
        cardRecords    = findViewById(R.id.cardRecords)
        layoutEmpty    = findViewById(R.id.layoutEmpty)
        rvRecords      = findViewById(R.id.rvRecords)
        tvRecordsTitle = findViewById(R.id.tvRecordsTitle)

        tvTotal       = findViewById(R.id.tvSummaryTotal)
        tvPresent     = findViewById(R.id.tvSummaryPresent)
        tvAbsent      = findViewById(R.id.tvSummaryAbsent)
        tvLate        = findViewById(R.id.tvSummaryLate)
        tvExcused     = findViewById(R.id.tvSummaryExcused)
        tvPresentRate = findViewById(R.id.tvPresentRate)
        tvAbsentRate  = findViewById(R.id.tvAbsentRate)
        tvLateRate    = findViewById(R.id.tvLateRate)
        tvExcusedRate = findViewById(R.id.tvExcusedRate)

        // RecyclerView
        recordAdapter = AttendanceRecordAdapter(emptyList())
        rvRecords.layoutManager = LinearLayoutManager(this)
        rvRecords.adapter = recordAdapter

        // Default dates
        val today = dateFormat.format(Date())
        val weekAgo = dateFormat.format(Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000))
        etStartDate.setText(today)
        etEndDate.setText(today)

        // Date pickers
        etStartDate.setOnClickListener { showDatePicker { date -> etStartDate.setText(date) } }
        etEndDate.setOnClickListener   { showDatePicker { date -> etEndDate.setText(date) } }

        // Toggle buttons
        btnDaily.setOnClickListener  { setMode(true) }
        btnWeekly.setOnClickListener { setMode(false) }

        btnGenerate.setOnClickListener { generateReport() }

        loadClasses()
    }

    private fun setMode(daily: Boolean) {
        isDaily = daily
        if (daily) {
            btnDaily.backgroundTintList  = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#0F2D5E"))
            btnDaily.setTextColor(android.graphics.Color.WHITE)
            btnWeekly.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#EBF2FF"))
            btnWeekly.setTextColor(android.graphics.Color.parseColor("#0F2D5E"))
            tvDateLabel.text = "DATE"
            tvEndDateLabel.visibility = View.GONE
            etEndDate.visibility      = View.GONE
        } else {
            btnWeekly.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#0F2D5E"))
            btnWeekly.setTextColor(android.graphics.Color.WHITE)
            btnDaily.backgroundTintList  = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#EBF2FF"))
            btnDaily.setTextColor(android.graphics.Color.parseColor("#0F2D5E"))
            tvDateLabel.text = "START DATE"
            tvEndDateLabel.visibility = View.VISIBLE
            etEndDate.visibility      = View.VISIBLE
        }
        // Reset results
        layoutSummary.visibility = View.GONE
        cardRecords.visibility   = View.GONE
        layoutEmpty.visibility   = View.VISIBLE
    }

    private fun loadClasses() {
        lifecycleScope.launch {
            val result = classRepo.getAllClasses()
            allClasses = result.getOrElse { emptyList() }
            withContext(Dispatchers.Main) {
                val classNames = listOf("Choose a class") + allClasses.map { "${it.className} — ${it.subject ?: ""}" }
                spinnerClass.adapter = ArrayAdapter(this@AdminReportsActivity,
                    android.R.layout.simple_spinner_dropdown_item, classNames)
            }
        }
    }

    private fun generateReport() {
        val classIdx = spinnerClass.selectedItemPosition
        if (classIdx == 0) {
            showError("Please select a class")
            return
        }
        val selectedClass = allClasses[classIdx - 1]
        val startDate = etStartDate.text.toString()
        val endDate   = etEndDate.text.toString()

        if (startDate.isBlank()) { showError("Please select a date"); return }

        showLoading(true)
        tvError.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = if (isDaily)
                    "http://10.0.2.2:8888/api/attendance/class/${selectedClass.classId}/date/$startDate"
                else
                    "http://10.0.2.2:8888/api/attendance/class/${selectedClass.classId}/date/$startDate"
                // For weekly use range endpoint when available

                val req = Request.Builder().url(url)
                    .header("Authorization", "Bearer $token").get().build()
                val response = httpClient.newCall(req).execute()
                val body = response.body?.string() ?: ""

                // Parse response
                val jsonObj = gson.fromJson(body, com.google.gson.JsonObject::class.java)
                val success = jsonObj.get("success")?.asBoolean ?: false

                if (success) {
                    val dataArray = jsonObj.get("data")?.asJsonArray
                    val type = object : TypeToken<List<Attendance>>() {}.type
                    val records: List<Attendance> = if (dataArray != null)
                        gson.fromJson(dataArray, type) else emptyList()

                    val present = records.count { it.status?.lowercase() == "present" }
                    val absent  = records.count { it.status?.lowercase() == "absent" }
                    val late    = records.count { it.status?.lowercase() == "late" }
                    val excused = records.count { it.status?.lowercase() == "excused" }
                    val total   = records.size

                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        if (records.isEmpty()) {
                            layoutSummary.visibility = View.GONE
                            cardRecords.visibility   = View.GONE
                            layoutEmpty.visibility   = View.VISIBLE
                            (layoutEmpty.getChildAt(1) as? TextView)?.text = "No Records Found"
                            (layoutEmpty.getChildAt(2) as? TextView)?.text = "No attendance records for this date"
                        } else {
                            // Summary
                            tvTotal.text   = total.toString()
                            tvPresent.text = present.toString()
                            tvAbsent.text  = absent.toString()
                            tvLate.text    = late.toString()
                            tvExcused.text = excused.toString()
                            tvPresentRate.text = "${if (total > 0) (present * 100 / total) else 0}%"
                            tvAbsentRate.text  = "${if (total > 0) (absent  * 100 / total) else 0}%"
                            tvLateRate.text    = "${if (total > 0) (late    * 100 / total) else 0}%"
                            tvExcusedRate.text = "${if (total > 0) (excused * 100 / total) else 0}%"

                            layoutSummary.visibility = View.VISIBLE
                            cardRecords.visibility   = View.VISIBLE
                            layoutEmpty.visibility   = View.GONE

                            tvRecordsTitle.text = if (isDaily) "Daily Report — $startDate"
                            else "Weekly Report — $startDate to $endDate"

                            recordAdapter.updateData(records)
                        }
                    }
                } else {
                    val msg = jsonObj.get("message")?.asString ?: "Failed to generate report"
                    withContext(Dispatchers.Main) { showLoading(false); showError(msg) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    showError("Error: ${e.message}")
                }
            }
        }
    }

    private fun showDatePicker(onDate: (String) -> Unit) {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            onDate(String.format("%04d-%02d-%02d", y, m + 1, d))
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnGenerate.isEnabled  = !show
        btnGenerate.text       = if (show) "Generating..." else "Generate Report"
    }

    private fun showError(msg: String) {
        tvError.text = msg
        tvError.visibility = View.VISIBLE
    }
}