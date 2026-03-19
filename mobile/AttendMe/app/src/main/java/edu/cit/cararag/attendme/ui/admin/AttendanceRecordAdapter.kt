package edu.cit.cararag.attendme.ui.admin

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.cit.cararag.attendme.R
import edu.cit.cararag.attendme.data.model.Attendance

class AttendanceRecordAdapter(
    private var records: List<Attendance>
) : RecyclerView.Adapter<AttendanceRecordAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInitials: TextView = view.findViewById(R.id.tvRecordInitials)
        val tvName: TextView     = view.findViewById(R.id.tvRecordName)
        val tvRoll: TextView     = view.findViewById(R.id.tvRecordRoll)
        val tvDate: TextView     = view.findViewById(R.id.tvRecordDate)
        val tvStatus: TextView   = view.findViewById(R.id.tvRecordStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        val name   = record.studentName ?: "Unknown"

        holder.tvName.text = name
        holder.tvRoll.text = "Roll: ${record.attendanceId}"
        holder.tvDate.text = record.date ?: ""
        holder.tvInitials.text = name.split(" ")
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2).joinToString("").uppercase()

        val status = record.status?.lowercase() ?: "unknown"
        holder.tvStatus.text = status.replaceFirstChar { it.uppercase() }

        val (textColor, bgRes) = when (status) {
            "present" -> Pair("#16A34A", R.drawable.bg_status_active)
            "absent"  -> Pair("#DC2626", R.drawable.bg_status_inactive)
            "late"    -> Pair("#D97706", R.drawable.bg_stat_late_badge)
            "excused" -> Pair("#7C3AED", R.drawable.bg_stat_excused_badge)
            else      -> Pair("#64748B", R.drawable.bg_detail_chip)
        }
        holder.tvStatus.setTextColor(Color.parseColor(textColor))
        holder.tvStatus.setBackgroundResource(bgRes)
    }

    override fun getItemCount() = records.size

    fun updateData(newRecords: List<Attendance>) {
        records = newRecords
        notifyDataSetChanged()
    }
}