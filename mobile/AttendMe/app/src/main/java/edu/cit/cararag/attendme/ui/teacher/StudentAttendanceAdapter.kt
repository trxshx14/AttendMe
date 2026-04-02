package edu.cit.cararag.attendme.ui.teacher

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import edu.cit.cararag.attendme.R
import edu.cit.cararag.attendme.data.model.Student

class StudentAttendanceAdapter(
    private var students: List<Student>,
    private val attendance: MutableMap<Long, String>,
    private val onStatusChange: (studentId: Long, status: String) -> Unit
) : RecyclerView.Adapter<StudentAttendanceAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInitials: TextView      = view.findViewById(R.id.tvInitials)
        val tvName: TextView          = view.findViewById(R.id.tvStudentName)
        val tvRoll: TextView          = view.findViewById(R.id.tvRollNumber)
        val tvStatus: TextView        = view.findViewById(R.id.tvCurrentStatus)
        val btnPresent: MaterialButton = view.findViewById(R.id.btnPresent)
        val btnAbsent: MaterialButton  = view.findViewById(R.id.btnAbsent)
        val btnLate: MaterialButton    = view.findViewById(R.id.btnLate)
        val btnExcused: MaterialButton = view.findViewById(R.id.btnExcused)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_attendance, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val student = students[position]
        val name    = "${student.firstName ?: ""} ${student.lastName ?: ""}".trim()
        val status  = attendance[student.studentId] ?: "present"

        holder.tvName.text = name
        holder.tvRoll.text = student.rollNumber ?: ""
        holder.tvInitials.text = name.split(" ")
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2).joinToString("").uppercase()

        // Update status badge
        applyStatus(holder, status)

        // Button clicks
        holder.btnPresent.setOnClickListener { changeStatus(holder, student.studentId, "present") }
        holder.btnAbsent.setOnClickListener  { changeStatus(holder, student.studentId, "absent") }
        holder.btnLate.setOnClickListener    { changeStatus(holder, student.studentId, "late") }
        holder.btnExcused.setOnClickListener { changeStatus(holder, student.studentId, "excused") }

        // Highlight active button
        highlightActiveButton(holder, status)
    }

    private fun changeStatus(holder: ViewHolder, studentId: Long, status: String) {
        attendance[studentId] = status
        applyStatus(holder, status)
        highlightActiveButton(holder, status)
        onStatusChange(studentId, status)
    }

    private fun applyStatus(holder: ViewHolder, status: String) {
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
    }

    private fun highlightActiveButton(holder: ViewHolder, status: String) {
        val alpha = 0.45f
        holder.btnPresent.alpha = if (status == "present") 1f else alpha
        holder.btnAbsent.alpha  = if (status == "absent")  1f else alpha
        holder.btnLate.alpha    = if (status == "late")    1f else alpha
        holder.btnExcused.alpha = if (status == "excused") 1f else alpha

        // Elevate active button
        holder.btnPresent.elevation = if (status == "present") 4f else 0f
        holder.btnAbsent.elevation  = if (status == "absent")  4f else 0f
        holder.btnLate.elevation    = if (status == "late")    4f else 0f
        holder.btnExcused.elevation = if (status == "excused") 4f else 0f
    }

    override fun getItemCount() = students.size

    fun updateStudents(newStudents: List<Student>) {
        students = newStudents
        notifyDataSetChanged()
    }

    fun getAttendanceMap(): Map<Long, String> = attendance.toMap()
}