package edu.cit.cararag.attendme.ui.teacher

import android.content.Intent
import android.graphics.Color
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
import edu.cit.cararag.attendme.ui.login.LoginActivity
import edu.cit.cararag.attendme.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class TeacherDashboardActivity : AppCompatActivity() {

    private val httpClient = OkHttpClient()
    private val gson       = Gson()
    private lateinit var token: String
    private lateinit var sessionManager: SessionManager
    private lateinit var classAdapter: TeacherClassAdapter

    // Weather views
    private lateinit var layoutWeatherLoading: LinearLayout
    private lateinit var layoutWeatherContent: LinearLayout
    private lateinit var layoutWeatherError: LinearLayout
    private lateinit var tvWeatherEmoji: TextView
    private lateinit var tvWeatherTemp: TextView
    private lateinit var tvWeatherCondition: TextView
    private lateinit var tvWeatherLocation: TextView
    private lateinit var tvWeatherHumidity: TextView
    private lateinit var tvWeatherWind: TextView
    private lateinit var tvWeatherFeels: TextView
    private lateinit var layoutWeatherTip: LinearLayout
    private lateinit var tvTipIcon: TextView
    private lateinit var tvTipTitle: TextView
    private lateinit var tvTipLevel: TextView
    private lateinit var tvTipText: TextView
    private lateinit var tvWeatherUpdated: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_dashboard)

        sessionManager = SessionManager(this)

        // ✅ Guard: if not logged in, go back to login
        val rawToken = sessionManager.getAccessToken()
        if (rawToken.isNullOrBlank()) {
            android.util.Log.e("Dashboard", "No token found — redirecting to login")
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }
        token = rawToken

        // ✅ Guard: if userId is invalid, go back to login
        val teacherId = sessionManager.getUserId()
        if (teacherId == -1L) {
            android.util.Log.e("Dashboard", "Invalid userId (-1) — redirecting to login")
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }

        android.util.Log.d("Dashboard", "Token: ${token.take(30)}... TeacherId: $teacherId")

        val name      = sessionManager.getFullName() ?: "Teacher"
        val firstName = name.split(" ").firstOrNull() ?: name
        findViewById<TextView>(R.id.tvGreeting).text    = getGreeting()
        findViewById<TextView>(R.id.tvTeacherName).text = firstName
        findViewById<TextView>(R.id.tvCurrentDate).text = getCurrentDate()

        // Weather views
        layoutWeatherLoading = findViewById(R.id.layoutWeatherLoading)
        layoutWeatherContent = findViewById(R.id.layoutWeatherContent)
        layoutWeatherError   = findViewById(R.id.layoutWeatherError)
        tvWeatherEmoji       = findViewById(R.id.tvWeatherEmoji)
        tvWeatherTemp        = findViewById(R.id.tvWeatherTemp)
        tvWeatherCondition   = findViewById(R.id.tvWeatherCondition)
        tvWeatherLocation    = findViewById(R.id.tvWeatherLocation)
        tvWeatherHumidity    = findViewById(R.id.tvWeatherHumidity)
        tvWeatherWind        = findViewById(R.id.tvWeatherWind)
        tvWeatherFeels       = findViewById(R.id.tvWeatherFeels)
        layoutWeatherTip     = findViewById(R.id.layoutWeatherTip)
        tvTipIcon            = findViewById(R.id.tvTipIcon)
        tvTipTitle           = findViewById(R.id.tvTipTitle)
        tvTipLevel           = findViewById(R.id.tvTipLevel)
        tvTipText            = findViewById(R.id.tvTipText)
        tvWeatherUpdated     = findViewById(R.id.tvWeatherUpdated)

        // RecyclerView
        classAdapter = TeacherClassAdapter(emptyList()) { cls -> openTakeAttendance(cls) }
        val rvClasses = findViewById<RecyclerView>(R.id.rvMyClasses)
        rvClasses.layoutManager = LinearLayoutManager(this)
        rvClasses.adapter = classAdapter

        // Quick action buttons
        findViewById<MaterialButton>(R.id.btnTakeAttendance).setOnClickListener {
            startActivity(Intent(this, TakeAttendanceActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnViewHistory).setOnClickListener {
            startActivity(Intent(this, AttendanceHistoryActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnViewReports).setOnClickListener {
            startActivity(Intent(this, TeacherReportsActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnTakeAttendanceClasses).setOnClickListener {
            startActivity(Intent(this, TakeAttendanceActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnTakeAttendanceNext).setOnClickListener {
            startActivity(Intent(this, TakeAttendanceActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            sessionManager.clearSession()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }

        // Weather buttons
        findViewById<MaterialButton>(R.id.btnWeatherRefresh).setOnClickListener { loadWeather() }
        findViewById<MaterialButton>(R.id.btnWeatherRetry).setOnClickListener { loadWeather() }

        loadDashboard()
        loadWeather()
    }

    /* ── Dashboard Data ──────────────────────────────── */
    private fun loadDashboard() {
        val teacherId = sessionManager.getUserId()
        val today     = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val baseUrl   = "http://10.0.2.2:8888"

        lifecycleScope.launch {
            // ── Step 1: Fetch classes ──────────────────
            val classResult = withContext(Dispatchers.IO) {
                try {
                    val req = Request.Builder()
                        .url("$baseUrl/api/classes/teacher/$teacherId")
                        .header("Authorization", "Bearer $token")
                        .get().build()

                    val response = httpClient.newCall(req).execute()
                    val statusCode = response.code
                    val body = response.body?.string() ?: ""

                    android.util.Log.d("Dashboard", "Classes HTTP: $statusCode")
                    android.util.Log.d("Dashboard", "Classes body: $body")

                    if (body.isBlank()) {
                        android.util.Log.e("Dashboard", "Empty body — HTTP $statusCode. Token may be expired.")
                        return@withContext emptyList<SchoolClass>()
                    }

                    val json = try {
                        gson.fromJson(body, com.google.gson.JsonObject::class.java)
                    } catch (e: Exception) {
                        android.util.Log.e("Dashboard", "JSON parse error: ${e.message}")
                        return@withContext emptyList<SchoolClass>()
                    }

                    if (json.get("success")?.asBoolean == true) {
                        val type = object : com.google.gson.reflect.TypeToken<List<SchoolClass>>() {}.type
                        val list = gson.fromJson<List<SchoolClass>>(json.get("data").asJsonArray, type)
                        android.util.Log.d("Dashboard", "Classes loaded: ${list.size}")
                        list
                    } else {
                        android.util.Log.e("Dashboard", "API success=false: $body")
                        emptyList()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Dashboard", "Classes fetch exception: ${e.message}", e)
                    emptyList<SchoolClass>()
                }
            }

            // ── Step 2: Fetch attendance for each class (single pass) ──
            var sumPresent = 0
            var sumAbsent  = 0
            var sumLate    = 0
            var sumExcused = 0
            val lock = Any()

            val classItems = classResult.map { cls ->
                async(Dispatchers.IO) {
                    try {
                        val req = Request.Builder()
                            .url("$baseUrl/api/attendance/class/${cls.classId}/date/$today")
                            .header("Authorization", "Bearer $token")
                            .get().build()

                        val response = httpClient.newCall(req).execute()
                        val body = response.body?.string() ?: ""

                        android.util.Log.d("Dashboard", "Attendance [${cls.classId}] HTTP: ${response.code}")
                        android.util.Log.d("Dashboard", "Attendance [${cls.classId}] body: $body")

                        if (body.isBlank()) return@async TeacherClassItem(cls)

                        val json    = gson.fromJson(body, com.google.gson.JsonObject::class.java)
                        val records = json.get("data")?.asJsonArray ?: com.google.gson.JsonArray()

                        var present = 0; var absent = 0; var late = 0; var excused = 0
                        records.forEach { r ->
                            when (r.asJsonObject.get("status")?.asString?.lowercase()) {
                                "present" -> present++
                                "absent"  -> absent++
                                "late"    -> late++
                                "excused" -> excused++
                            }
                        }

                        // Thread-safe accumulation
                        synchronized(lock) {
                            sumPresent += present
                            sumAbsent  += absent
                            sumLate    += late
                            sumExcused += excused
                        }

                        val total = cls.studentCount ?: 0
                        val rate  = if (total > 0) (present * 100 / total) else 0
                        TeacherClassItem(cls, present, rate)

                    } catch (e: Exception) {
                        android.util.Log.e("Dashboard", "Attendance fetch error [${cls.classId}]: ${e.message}", e)
                        TeacherClassItem(cls)
                    }
                }
            }.awaitAll()

            val totalStudents = classResult.sumOf { it.studentCount ?: 0 }

            // ── Step 3: Update UI ──────────────────────
            withContext(Dispatchers.Main) {
                // Hero mini-stats
                findViewById<TextView>(R.id.tvMiniStudents).text = totalStudents.toString()
                findViewById<TextView>(R.id.tvMiniClasses).text  = classResult.size.toString()

                // Stat cards
                findViewById<TextView>(R.id.tvStatClasses).text  = classResult.size.toString()
                findViewById<TextView>(R.id.tvStatStudents).text = totalStudents.toString()
                findViewById<TextView>(R.id.tvStatPresent).text  = sumPresent.toString()
                findViewById<TextView>(R.id.tvStatAbsent).text   = sumAbsent.toString()
                findViewById<TextView>(R.id.tvStatLate).text     = sumLate.toString()
                findViewById<TextView>(R.id.tvStatExcused).text  = sumExcused.toString()

                // Today's summary card
                findViewById<TextView>(R.id.tvSummaryPresent).text = sumPresent.toString()
                findViewById<TextView>(R.id.tvSummaryAbsent).text  = sumAbsent.toString()
                findViewById<TextView>(R.id.tvSummaryLate).text    = sumLate.toString()
                findViewById<TextView>(R.id.tvSummaryExcused).text = sumExcused.toString()
                val totalMarked = sumPresent + sumAbsent + sumLate + sumExcused
                findViewById<TextView>(R.id.tvSummaryTotal).text = totalMarked.toString()

                // Classes RecyclerView
                if (classItems.isEmpty()) {
                    findViewById<LinearLayout>(R.id.layoutNoClasses).visibility = View.VISIBLE
                    findViewById<RecyclerView>(R.id.rvMyClasses).visibility     = View.GONE
                } else {
                    findViewById<LinearLayout>(R.id.layoutNoClasses).visibility = View.GONE
                    findViewById<RecyclerView>(R.id.rvMyClasses).visibility     = View.VISIBLE
                    classAdapter.updateData(classItems)
                }

                showNextClass(classItems)
            }
        }
    }

    /* ── Weather Widget ──────────────────────────────── */
    private fun loadWeather() {
        layoutWeatherLoading.visibility = View.VISIBLE
        layoutWeatherContent.visibility = View.GONE
        layoutWeatherError.visibility   = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val lat = 10.3157
                val lon = 123.8854

                // Reverse geocode
                val geoReq = Request.Builder()
                    .url("https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lon&format=json")
                    .header("User-Agent", "AttendMe/1.0")
                    .header("Accept-Language", "en")
                    .get().build()
                val geoBody = httpClient.newCall(geoReq).execute().body?.string() ?: ""
                val geoJson = JSONObject(geoBody)
                val address = geoJson.optJSONObject("address")
                val city = address?.optString("city")?.takeIf { it.isNotBlank() }
                    ?: address?.optString("town")?.takeIf { it.isNotBlank() }
                    ?: address?.optString("municipality")?.takeIf { it.isNotBlank() }
                    ?: "Cebu City"

                // Fetch weather
                val weatherReq = Request.Builder()
                    .url(
                        "https://api.open-meteo.com/v1/forecast" +
                                "?latitude=$lat&longitude=$lon" +
                                "&current=temperature_2m,relative_humidity_2m,apparent_temperature," +
                                "weather_code,wind_speed_10m,is_day" +
                                "&wind_speed_unit=kmh&temperature_unit=celsius&timezone=auto"
                    ).get().build()
                val weatherBody = httpClient.newCall(weatherReq).execute().body?.string() ?: ""
                val weatherJson = JSONObject(weatherBody)
                val current     = weatherJson.getJSONObject("current")

                val temp      = current.getDouble("temperature_2m").toInt()
                val feelsLike = current.getDouble("apparent_temperature").toInt()
                val humidity  = current.getInt("relative_humidity_2m")
                val wind      = current.getDouble("wind_speed_10m").toInt()
                val code      = current.getInt("weather_code")
                val isDay     = current.getInt("is_day")

                val (emoji, condition) = getWeatherInfo(code, isDay)
                val tip      = getAttendanceTip(code, temp)
                val updated  = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

                withContext(Dispatchers.Main) {
                    layoutWeatherLoading.visibility = View.GONE
                    layoutWeatherContent.visibility = View.VISIBLE

                    tvWeatherEmoji.text     = emoji
                    tvWeatherTemp.text      = "$temp°C"
                    tvWeatherCondition.text = condition
                    tvWeatherLocation.text  = city
                    tvWeatherHumidity.text  = "$humidity%"
                    tvWeatherWind.text      = "$wind km/h"
                    tvWeatherFeels.text     = "$feelsLike°C"
                    tvTipIcon.text          = tip[0]
                    tvTipTitle.text         = tip[1]
                    tvTipLevel.text         = tip[2]
                    tvTipText.text          = tip[3]
                    tvWeatherUpdated.text   = "Updated $updated"

                    tvTipTitle.setTextColor(Color.parseColor(tip[4]))
                    tvTipLevel.setTextColor(Color.parseColor(tip[4]))
                    layoutWeatherTip.setBackgroundColor(Color.parseColor(tip[5]))
                }
            } catch (e: Exception) {
                android.util.Log.e("Dashboard", "Weather fetch error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    layoutWeatherLoading.visibility = View.GONE
                    layoutWeatherError.visibility   = View.VISIBLE
                }
            }
        }
    }

    private fun getWeatherInfo(code: Int, isDay: Int): Pair<String, String> = when {
        code == 0                          -> Pair(if (isDay == 1) "☀️" else "🌙", "Clear Sky")
        code in listOf(1, 2)               -> Pair("⛅", "Partly Cloudy")
        code == 3                          -> Pair("☁️", "Overcast")
        code in listOf(45, 48)             -> Pair("🌫️", "Foggy")
        code in listOf(51, 53, 55)         -> Pair("🌦️", "Drizzle")
        code in listOf(61, 63, 65)         -> Pair("🌧️", "Rain")
        code in listOf(80, 81, 82)         -> Pair("🌧️", "Rain Showers")
        code in listOf(95, 96, 99)         -> Pair("⛈️", "Thunderstorm")
        else                               -> Pair("🌡️", "Unknown")
    }

    private fun getAttendanceTip(code: Int, temp: Int): Array<String> = when {
        code in listOf(95, 96, 99) -> arrayOf(
            "⛈️", "ATTENDANCE OUTLOOK", "High Impact",
            "Thunderstorm warning — expect significant absences today. Consider marking weather-related absences as excused.",
            "#DC2626", "#FFF5F5"
        )
        code in listOf(61, 63, 65, 80, 81, 82) -> arrayOf(
            "🌧️", "ATTENDANCE OUTLOOK", "Moderate Impact",
            "Rainy day — some students may be absent due to flooding or transportation issues.",
            "#D97706", "#FFFBEB"
        )
        code in listOf(51, 53, 55) -> arrayOf(
            "🌦️", "ATTENDANCE OUTLOOK", "Low Impact",
            "Light drizzle expected — minor impact on attendance. Monitor late arrivals.",
            "#0369A1", "#F0F9FF"
        )
        code in listOf(45, 48) -> arrayOf(
            "🌫️", "ATTENDANCE OUTLOOK", "Low Impact",
            "Foggy conditions — low visibility may cause transport delays. Expect some late arrivals.",
            "#6B7280", "#F9FAFB"
        )
        temp >= 35 -> arrayOf(
            "🌡️", "ATTENDANCE OUTLOOK", "High Impact",
            "Extreme heat today — watch for heat-related absences. Ensure classrooms are well-ventilated.",
            "#DC2626", "#FFF5F5"
        )
        temp >= 30 -> arrayOf(
            "☀️", "ATTENDANCE OUTLOOK", "Moderate Impact",
            "Hot weather — some students may feel unwell. Stay hydrated and monitor attendance closely.",
            "#D97706", "#FFFBEB"
        )
        else -> arrayOf(
            "✅", "ATTENDANCE OUTLOOK", "No Impact",
            "Good weather today — expect normal to high attendance rates. Great day for your classes!",
            "#059669", "#F0FDF4"
        )
    }

    /* ── Next Class Banner ───────────────────────────── */
    private fun showNextClass(items: List<TeacherClassItem>) {
        val nowMins = Calendar.getInstance().let {
            it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
        }
        val dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date()).lowercase()

        val scheduled = items.filter { item ->
            val schedule = item.cls.schedule ?: return@filter false
            val day      = schedule.split(" ").firstOrNull()?.lowercase() ?: return@filter false
            dayName.startsWith(day.take(3))
        }.sortedBy { item ->
            val timePart = item.cls.schedule?.split(" ")?.getOrNull(1) ?: ""
            timeToMinutes(timePart) ?: 9999
        }

        val next = scheduled.firstOrNull { item ->
            val timePart = item.cls.schedule?.split(" ")?.getOrNull(1) ?: ""
            (timeToMinutes(timePart) ?: return@firstOrNull false) > nowMins
        } ?: scheduled.lastOrNull()

        if (next != null) {
            val cls      = next.cls
            val grade    = cls.className ?: ""
            val section  = cls.section?.let { " — $it" } ?: ""
            val timePart = cls.schedule?.split(" ")?.getOrNull(1) ?: ""
            val timeStr  = formatTime(timePart)

            val isUpcoming = scheduled.any { item ->
                val tp = item.cls.schedule?.split(" ")?.getOrNull(1) ?: ""
                (timeToMinutes(tp) ?: 9999) > nowMins
            }

            findViewById<LinearLayout>(R.id.layoutNextClass).visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvNextClassLabel).text =
                if (isUpcoming) "YOUR NEXT CLASS" else "LAST CLASS TODAY"
            findViewById<TextView>(R.id.tvNextClassName).text = "$grade$section"
            val meta = buildString {
                cls.subject?.let { append(it); append(" · ") }
                if (timeStr.isNotBlank()) append("$timeStr · ")
                append("${cls.studentCount ?: 0} students")
            }
            findViewById<TextView>(R.id.tvNextClassMeta).text = meta
        }
    }

    /* ── Helpers ─────────────────────────────────────── */
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

    private fun getGreeting() = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11  -> "GOOD MORNING"
        in 12..17 -> "GOOD AFTERNOON"
        else      -> "GOOD EVENING"
    }

    private fun getCurrentDate() =
        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
}