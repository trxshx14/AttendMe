package edu.cit.cararag.attendme.ui.teacher

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.cit.cararag.attendme.R

data class StudentReportRow(
    val studentId: Long,
    val firstName: String,
    val lastName: String,
    val rollNumber: String,
    val className: String,
    val present: Int,
    val absent: Int,
    val late: Int,
    val excused: Int
) {
    val total: Int get() = present + absent + late + excused
    val attendanceRate: Int get() = if (total > 0) ((present + late) * 100 / total) else 0
}

class StudentBreakdownAdapter(
    private var rows: List<StudentReportRow>
) : RecyclerView.Adapter<StudentBreakdownAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInitials: TextView  = view.findViewById(R.id.tvInitials)
        val tvName: TextView      = view.findViewById(R.id.tvStudentName)
        val tvClass: TextView     = view.findViewById(R.id.tvClassName)
        val tvStanding: TextView  = view.findViewById(R.id.tvStanding)
        val tvPresent: TextView   = view.findViewById(R.id.tvPresent)
        val tvAbsent: TextView    = view.findViewById(R.id.tvAbsent)
        val tvLate: TextView      = view.findViewById(R.id.tvLate)
        val tvExcused: TextView   = view.findViewById(R.id.tvExcused)
        val tvRate: TextView      = view.findViewById(R.id.tvRate)
        val viewRateBar: View     = view.findViewById(R.id.viewRateBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_breakdown, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row  = rows[position]
        val name = "${row.firstName} ${row.lastName}".trim()
        val rate = row.attendanceRate

        holder.tvName.text  = name
        holder.tvClass.text = row.className
        holder.tvInitials.text = name.split(" ")
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2).joinToString("").uppercase()

        holder.tvPresent.text = "P\n${row.present}"
        holder.tvAbsent.text  = "A\n${row.absent}"
        holder.tvLate.text    = "L\n${row.late}"
        holder.tvExcused.text = "E\n${row.excused}"
        holder.tvRate.text    = "$rate%"

        // Rate bar
        val parent = holder.viewRateBar.parent as FrameLayout
        holder.viewRateBar.post {
            val params = holder.viewRateBar.layoutParams
            params.width = (parent.width * rate / 100)
            holder.viewRateBar.layoutParams = params
        }

        // Color
        val (rateColor, bgRes, standingText) = when {
            rate >= 80 -> Triple("#10B981", R.drawable.bg_status_active, "Good")
            rate >= 60 -> Triple("#F59E0B", R.drawable.bg_stat_late_badge, "At Risk")
            else       -> Triple("#EF4444", R.drawable.bg_status_inactive, "Critical")
        }
        holder.tvRate.setTextColor(Color.parseColor(rateColor))
        holder.viewRateBar.setBackgroundColor(Color.parseColor(rateColor))
        holder.tvStanding.text = standingText
        holder.tvStanding.setTextColor(Color.parseColor(rateColor))
        holder.tvStanding.setBackgroundResource(bgRes)
    }

    override fun getItemCount() = rows.size

    fun updateData(newRows: List<StudentReportRow>) {
        rows = newRows
        notifyDataSetChanged()
    }
}