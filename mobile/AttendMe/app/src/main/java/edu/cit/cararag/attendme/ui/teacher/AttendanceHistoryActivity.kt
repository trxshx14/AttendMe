package edu.cit.cararag.attendme.ui.teacher

import android.app.AlertDialog
import android.app.DatePickerDialog
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
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import edu.cit.cararag.attendme.R
import edu.cit.cararag.attendme.data.model.Attendance
import edu.cit.cararag.attendme.data.model.SchoolClass
import edu.cit.cararag.attendme.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*

class AttendanceHistoryActivity : AppCompatActivity() {

    private val httpClient = OkHttpClient()
    private val gson       = Gson()
    private val jsonType   = "application/json".toMediaType()
    private lateinit var token: String
    private lateinit var sessionManager: SessionManager

    private lateinit var etSearch: EditText
    private lateinit var spinnerClass: Spinner
    private lateinit var spinnerStatus: Spinner
    private lateinit var etDateFrom: EditText
    private lateinit var etDateTo: EditText
    private lateinit var tvError: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var rvRecords: RecyclerView
    private lateinit var tvResultCount: TextView
    private lateinit var layoutPagination: LinearLayout
    private lateinit var btnPrev: MaterialButton
    private lateinit var btnNext: MaterialButton
    private lateinit var tvPageInfo: TextView

    private var classes = listOf<SchoolClass>()
    private var allRecords = listOf<Attendance>()
    private var filteredRecords = listOf<Attendance>()
    private lateinit var adapter: AttendanceHistoryAdapter

    private val PAGE_SIZE = 10
    private var currentPage = 1
    private var totalPages  = 1

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance_history)

        sessionManager = SessionManager(this)
        token = sessionManager.getAccessToken() ?: ""

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // Bind views
        etSearch        = findViewById(R.id.etSearch)
        spinnerClass    = findViewById(R.id.spinnerClass)
        spinnerStatus   = findViewById(R.id.spinnerStatus)
        etDateFrom      = findViewById(R.id.etDateFrom)
        etDateTo        = findViewById(R.id.etDateTo)
        tvError         = findViewById(R.id.tvError)
        progressBar     = findViewById(R.id.progressBar)
        layoutEmpty     = findViewById(R.id.layoutEmpty)
        rvRecords       = findViewById(R.id.rvRecords)
        tvResultCount   = findViewById(R.id.tvResultCount)
        layoutPagination = findViewById(R.id.layoutPagination)
        btnPrev         = findViewById(R.id.btnPrevPage)
        btnNext         = findViewById(R.id.btnNextPage)
        tvPageInfo      = findViewById(R.id.tvPageInfo)

        // Status spinner
        val statuses = listOf("All Statuses", "Present", "Absent", "Late", "Excused")
        spinnerStatus.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, statuses)

        // Default date range: last 30 days
        val today = sdf.format(Date())
        val thirtyAgo = sdf.format(Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000))
        etDateFrom.setText(thirtyAgo)
        etDateTo.setText(today)
        etDateFrom.setOnClickListener { showDatePicker(etDateFrom) }
        etDateTo.setOnClickListener   { showDatePicker(etDateTo) }

        // Adapter
        adapter = AttendanceHistoryAdapter(emptyList(),
            onEdit   = { record -> showEditDialog(record) },
            onDelete = { record -> confirmDelete(record) }
        )
        rvRecords.layoutManager = LinearLayoutManager(this)
        rvRecords.adapter = adapter

        // Search
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilters() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Buttons
        findViewById<MaterialButton>(R.id.btnApplyFilters).setOnClickListener { loadRecords() }
        findViewById<MaterialButton>(R.id.btnClearFilters).setOnClickListener { clearFilters() }
        btnPrev.setOnClickListener { if (currentPage > 1) { currentPage--; renderPage() } }
        btnNext.setOnClickListener { if (currentPage < totalPages) { currentPage++; renderPage() } }

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
                    spinnerClass.adapter = ArrayAdapter(
                        this@AttendanceHistoryActivity,
                        android.R.layout.simple_spinner_dropdown_item, names
                    )
                    loadRecords()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { showError("Failed to load classes") }
            }
        }
    }

    private fun loadRecords() {
        showLoading(true)
        val from = etDateFrom.text.toString().ifBlank { sdf.format(Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)) }
        val to   = etDateTo.text.toString().ifBlank { sdf.format(Date()) }

        // Build date list
        val dates = mutableListOf<String>()
        var cur = sdf.parse(from) ?: Date()
        val end = sdf.parse(to) ?: Date()
        while (!cur.after(end)) {
            dates.add(sdf.format(cur))
            cur = Date(cur.time + 24 * 60 * 60 * 1000)
        }

        // Which classes to query
        val classIdx = spinnerClass.selectedItemPosition
        val classesToQuery = if (classIdx == 0) classes else listOfNotNull(classes.getOrNull(classIdx - 1))

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
                                    val type = object : TypeToken<List<Attendance>>() {}.type
                                    gson.fromJson<List<Attendance>>(json.get("data").asJsonArray, type)
                                } else emptyList()
                            } catch (e: Exception) { emptyList<Attendance>() }
                        }
                    }
                }.awaitAll()

                val raw = fetches.flatten().map { r ->
                    r.status?.lowercase()?.let { r.copy(status = it) }
                }.sortedByDescending { it?.date }

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    allRecords = raw as List<Attendance>
                    applyFilters()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    showError("Failed to load records: ${e.message}")
                }
            }
        }
    }

    private fun applyFilters() {
        val query      = etSearch.text.toString().lowercase()
        val statusIdx  = spinnerStatus.selectedItemPosition
        val statusFilter = when (statusIdx) {
            1 -> "present"; 2 -> "absent"; 3 -> "late"; 4 -> "excused"; else -> "all"
        }

        filteredRecords = allRecords.filter { r ->
            val nameMatch = query.isBlank() ||
                    (r.studentName ?: "").lowercase().contains(query)
            val statusMatch = statusFilter == "all" || r.status == statusFilter
            nameMatch && statusMatch
        }

        currentPage = 1
        totalPages  = maxOf(1, Math.ceil(filteredRecords.size.toDouble() / PAGE_SIZE).toInt())
        tvResultCount.text = "${filteredRecords.size} record${if (filteredRecords.size != 1) "s" else ""}"
        renderPage()
    }

    private fun renderPage() {
        val start  = (currentPage - 1) * PAGE_SIZE
        val end    = minOf(start + PAGE_SIZE, filteredRecords.size)
        val page   = filteredRecords.subList(start, end)

        adapter.updateData(page)

        if (filteredRecords.isEmpty()) {
            layoutEmpty.visibility = View.VISIBLE
            rvRecords.visibility   = View.GONE
            layoutPagination.visibility = View.GONE
        } else {
            layoutEmpty.visibility = View.GONE
            rvRecords.visibility   = View.VISIBLE
            layoutPagination.visibility = if (totalPages > 1) View.VISIBLE else View.GONE
            tvPageInfo.text = "$currentPage / $totalPages"
            btnPrev.isEnabled = currentPage > 1
            btnNext.isEnabled = currentPage < totalPages
        }
    }

    private fun showEditDialog(record: Attendance) {
        val statuses = arrayOf("present", "absent", "late", "excused")
        val labels   = arrayOf("Present", "Absent", "Late", "Excused")
        var selected = statuses.indexOf(record.status?.lowercase()).takeIf { it >= 0 } ?: 0

        AlertDialog.Builder(this)
            .setTitle("Edit Attendance — ${record.studentName}")
            .setSingleChoiceItems(labels, selected) { _, which -> selected = which }
            .setPositiveButton("Update") { _, _ ->
                updateRecord(record, statuses[selected])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateRecord(record: Attendance, newStatus: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val payload = mapOf(
                    "studentId"  to record.studentId,
                    "classId"    to record.classId,
                    "date"       to record.date,
                    "status"     to newStatus,
                    "markedById" to sessionManager.getUserId()
                )
                val req = Request.Builder()
                    .url("http://10.0.2.2:8888/api/attendance/${record.attendanceId}")
                    .header("Authorization", "Bearer $token")
                    .put(gson.toJson(payload).toRequestBody(jsonType))
                    .build()
                val success = httpClient.newCall(req).execute().isSuccessful
                withContext(Dispatchers.Main) {
                    if (success) {
                        allRecords = allRecords.map {
                            if (it.attendanceId == record.attendanceId) it.copy(status = newStatus) else it
                        }
                        applyFilters()
                        Toast.makeText(this@AttendanceHistoryActivity,
                            "Record updated", Toast.LENGTH_SHORT).show()
                    } else showError("Failed to update record")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { showError("Error: ${e.message}") }
            }
        }
    }

    private fun confirmDelete(record: Attendance) {
        AlertDialog.Builder(this)
            .setTitle("Delete Record")
            .setMessage("Delete attendance record for ${record.studentName}? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> deleteRecord(record) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteRecord(record: Attendance) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url("http://10.0.2.2:8888/api/attendance/${record.attendanceId}")
                    .header("Authorization", "Bearer $token")
                    .delete().build()
                val success = httpClient.newCall(req).execute().isSuccessful
                withContext(Dispatchers.Main) {
                    if (success) {
                        allRecords = allRecords.filter { it.attendanceId != record.attendanceId }
                        applyFilters()
                        Toast.makeText(this@AttendanceHistoryActivity,
                            "Record deleted", Toast.LENGTH_SHORT).show()
                    } else showError("Failed to delete record")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { showError("Error: ${e.message}") }
            }
        }
    }

    private fun clearFilters() {
        etSearch.setText("")
        spinnerClass.setSelection(0)
        spinnerStatus.setSelection(0)
        val today    = sdf.format(Date())
        val thirtyAgo = sdf.format(Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000))
        etDateFrom.setText(thirtyAgo)
        etDateTo.setText(today)
        loadRecords()
    }

    private fun showDatePicker(field: EditText) {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            field.setText(String.format("%04d-%02d-%02d", y, m + 1, d))
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            rvRecords.visibility = View.GONE
            layoutEmpty.visibility = View.GONE
        }
    }

    private fun showError(msg: String) {
        tvError.text = msg
        tvError.visibility = View.VISIBLE
    }
}