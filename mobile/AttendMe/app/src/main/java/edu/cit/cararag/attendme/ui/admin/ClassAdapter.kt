package edu.cit.cararag.attendme.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.cit.cararag.attendme.R
import edu.cit.cararag.attendme.data.model.SchoolClass

class ClassAdapter(
    private var classes: List<SchoolClass>,
    private val teacherNames: Map<Long, String>,
    private val onEdit: (SchoolClass) -> Unit,
    private val onDelete: (SchoolClass) -> Unit,
    private val onViewStudents: (SchoolClass) -> Unit
) : RecyclerView.Adapter<ClassAdapter.ClassViewHolder>() {

    inner class ClassViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvClassName: TextView    = view.findViewById(R.id.tvClassName)
        val tvSubject: TextView      = view.findViewById(R.id.tvSubject)
        val tvSection: TextView      = view.findViewById(R.id.tvSection)
        val tvTeacher: TextView      = view.findViewById(R.id.tvTeacher)
        val tvSchedule: TextView     = view.findViewById(R.id.tvSchedule)
        val tvStudentCount: TextView = view.findViewById(R.id.tvStudentCount)
        val btnEdit: ImageButton   = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        val btnViewStudents: Button  = view.findViewById(R.id.btnViewStudents)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_class, parent, false)
        return ClassViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        val cls = classes[position]
        holder.tvClassName.text    = cls.className
        holder.tvSubject.text      = cls.subject ?: "—"
        holder.tvSection.text      = if (!cls.section.isNullOrBlank()) cls.section else "—"
        holder.tvTeacher.text      = cls.teacherId?.let { teacherNames[it] } ?: "Unassigned"
        holder.tvStudentCount.text = "${cls.studentCount ?: 0}"
        holder.tvSchedule.text     = cls.schedule ?: "Not set"

        holder.btnEdit.setOnClickListener         { onEdit(cls) }
        holder.btnDelete.setOnClickListener       { onDelete(cls) }
        holder.btnViewStudents.setOnClickListener { onViewStudents(cls) }
    }

    override fun getItemCount() = classes.size

    fun updateData(newClasses: List<SchoolClass>) {
        classes = newClasses
        notifyDataSetChanged()
    }
}