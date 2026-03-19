package edu.cit.cararag.attendme.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.cit.cararag.attendme.R
import edu.cit.cararag.attendme.data.model.Student

class StudentAdapter(
    private var students: List<Student>,
    private val onRemove: (Student) -> Unit
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    inner class StudentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInitials: TextView   = view.findViewById(R.id.tvStudentInitials)
        val tvName: TextView       = view.findViewById(R.id.tvStudentName)
        val tvRoll: TextView       = view.findViewById(R.id.tvRollNumber)
        val btnRemove: Button      = view.findViewById(R.id.btnRemoveStudent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]
        val name = student.fullName ?: "${student.firstName} ${student.lastName}"

        holder.tvName.text  = name
        holder.tvRoll.text  = "Roll: ${student.rollNumber ?: "N/A"}"
        holder.tvInitials.text = name.split(" ")
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2)
            .joinToString("")
            .uppercase()

        holder.btnRemove.setOnClickListener { onRemove(student) }
    }

    override fun getItemCount() = students.size

    fun updateData(newStudents: List<Student>) {
        students = newStudents
        notifyDataSetChanged()
    }
}