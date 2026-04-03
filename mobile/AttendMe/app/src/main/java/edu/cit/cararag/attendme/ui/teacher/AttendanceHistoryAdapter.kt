package edu.cit.cararag.attendme.ui.teacher

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import edu.cit.cararag.attendme.R
import edu.cit.cararag.attendme.data.model.Attendance

class AttendanceHistoryAdapter(
    private var records: List<Attendance>,
    private val onEdit: (Attendance) -> Unit,
    private val onDelete: (Attendance) -> Unit
) : RecyclerView.Adapter<AttendanceHistoryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInitials: TextView   = view.findViewById(R.id.tvInitials)
        val tvName: TextView       = view.findViewById(R.id.tvStudentName)
        val tvClass: TextView      = view.findViewById(R.id.tvClassName)
        val tvDate: TextView       = view.findViewById(R.id.tvDate)
        val tvStatus: TextView     = view.findViewById(R.id.tvStatus)
        val btnEdit: MaterialButton   = view.findViewById(R.id.btnEdit)
        val btnDelete: MaterialButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        val name   = record.studentName ?: "Unknown"

        holder.tvName.text  = name
        holder.tvClass.text = record.className ?: "—"
        holder.tvDate.text  = formatDate(record.date)
        holder.tvInitials.text = name.split(" ")
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2).joinToString("").uppercase()

        // Status badge
        val status = record.status?.lowercase() ?: "present"
        val (text, textColor, bgRes) = when (status) {
            "present" -> Triple("Present", "#16A34A", R.drawable.bg_status_active)
            "absent"  -> Triple("Absent",  "#DC2626", R.drawable.bg_status_inactive)
            "late"    -> Triple("Late",    "#D97706", R.drawable.bg_stat_late_badge)
            "excused" -> Triple("Excused", "#7C3AED", R.drawable.bg_stat_excused_badge)
            else      -> Triple("Present", "#16A34A", R.drawable.bg_status_active)
        }
        holder.tvStatus.text = text
        holder.tvStatus.setTextColor(Color.parseColor(textColor))
        holder.tvStatus.setBackgroundResource(bgRes)

        holder.btnEdit.setOnClickListener   { onEdit(record) }
        holder.btnDelete.setOnClickListener { onDelete(record) }
    }

    private fun formatDate(date: String?): String {
        if (date.isNullOrBlank()) return "—"
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val out = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            out.format(sdf.parse(date)!!)
        } catch (e: Exception) { date }
    }

    override fun getItemCount() = records.size

    fun updateData(newRecords: List<Attendance>) {
        records = newRecords
        notifyDataSetChanged()
    }
}