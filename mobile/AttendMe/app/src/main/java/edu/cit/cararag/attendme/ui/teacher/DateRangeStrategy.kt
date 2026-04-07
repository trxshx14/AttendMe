package edu.cit.cararag.attendme.ui.teacher

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

interface DateRangeStrategy {
    fun getRange(): Pair<String, String>
}

private fun getBaseCalendar(): Calendar = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

class ThisWeekStrategy(private val sdf: SimpleDateFormat) : DateRangeStrategy {
    override fun getRange(): Pair<String, String> {
        val cal = getBaseCalendar()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val start = sdf.format(cal.time)

        cal.add(Calendar.DAY_OF_WEEK, 6)
        return Pair(start, sdf.format(cal.time))
    }
}

class LastWeekStrategy(private val sdf: SimpleDateFormat) : DateRangeStrategy {
    override fun getRange(): Pair<String, String> {
        val cal = getBaseCalendar()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.add(Calendar.WEEK_OF_YEAR, -1)
        val start = sdf.format(cal.time)

        cal.add(Calendar.DAY_OF_WEEK, 6)
        return Pair(start, sdf.format(cal.time))
    }
}

class LastTwoWeeksStrategy(private val sdf: SimpleDateFormat) : DateRangeStrategy {
    override fun getRange(): Pair<String, String> {
        val cal = getBaseCalendar()
        val end = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -14)
        val start = sdf.format(cal.time)
        return Pair(start, end)
    }
}

class ThisMonthStrategy(private val sdf: SimpleDateFormat) : DateRangeStrategy {
    override fun getRange(): Pair<String, String> {
        val cal = getBaseCalendar()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val start = sdf.format(cal.time)

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        return Pair(start, sdf.format(cal.time))
    }
}

class CustomRangeStrategy(
    private val from: String,
    private val to: String
) : DateRangeStrategy {
    override fun getRange() = Pair(from, to)
}