package edu.cit.cararag.attendme.ui.teacher

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import edu.cit.cararag.attendme.R
import edu.cit.cararag.attendme.data.model.StudentAttendance

class StudentAttendanceAdapter(
    private val students: List<StudentAttendance>,
    private val onStatusChanged: () -> Unit
) : RecyclerView.Adapter<StudentAttendanceAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_attendance, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val student = students[position]
        holder.tvName.text = student.fullName
        holder.tvRoll.text = student.rollNumber ?: "No Roll #"
        holder.tvAvatar.text = student.firstName.take(1) + student.lastName.take(1)

        // Reset UI States
        resetButtons(holder)

        // Highlight Active Status (Matching your Web Colors)
        when (student.status.lowercase()) {
            "present" -> highlightButton(holder.btnPresent, "#D1FAE5", "#10B981")
            "absent" -> highlightButton(holder.btnAbsent, "#FEE2E2", "#EF4444")
            "late" -> highlightButton(holder.btnLate, "#FEF3C7", "#F59E0B")
        }

        // Click Listeners
        holder.btnPresent.setOnClickListener { updateStatus(student, "present") }
        holder.btnAbsent.setOnClickListener { updateStatus(student, "absent") }
        holder.btnLate.setOnClickListener { updateStatus(student, "late") }
    }

    private fun updateStatus(student: StudentAttendance, status: String) {
        student.status = status
        notifyDataSetChanged()
        onStatusChanged()
    }

    private fun highlightButton(view: TextView, bgColor: String, textColor: String) {
        view.setBackgroundColor(Color.parseColor(bgColor))
        view.setTextColor(Color.parseColor(textColor))
    }

    private fun resetButtons(holder: ViewHolder) {
        val gray = "#64748B"
        val transparent = "#00000000"
        holder.btnPresent.setTextColor(Color.parseColor(gray))
        holder.btnPresent.setBackgroundColor(Color.parseColor(transparent))
        holder.btnAbsent.setTextColor(Color.parseColor(gray))
        holder.btnAbsent.setBackgroundColor(Color.parseColor(transparent))
        holder.btnLate.setTextColor(Color.parseColor(gray))
        holder.btnLate.setBackgroundColor(Color.parseColor(transparent))
    }

    override fun getItemCount() = students.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvStudentName)
        val tvRoll: TextView = view.findViewById(R.id.tvRollNumber)
        val tvAvatar: TextView = view.findViewById(R.id.tvAvatar)
        val btnPresent: TextView = view.findViewById(R.id.btnPresent)
        val btnAbsent: TextView = view.findViewById(R.id.btnAbsent)
        val btnLate: TextView = view.findViewById(R.id.btnLate)
    }
}