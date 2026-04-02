package edu.cit.cararag.attendme.ui.teacher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.cit.cararag.attendme.R
import edu.cit.cararag.attendme.data.model.SchoolClass

data class TeacherClassItem(
    val cls: SchoolClass,
    val presentCount: Int = 0,
    val attendanceRate: Int = 0
)

class TeacherClassAdapter(
    private var items: List<TeacherClassItem>,
    private val onTake: (SchoolClass) -> Unit
) : RecyclerView.Adapter<TeacherClassAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView    = view.findViewById(R.id.tvTCClassName)
        val tvMeta: TextView    = view.findViewById(R.id.tvTCMeta)
        val tvPresent: TextView = view.findViewById(R.id.tvTCPresent)
        val tvRate: TextView    = view.findViewById(R.id.tvTCRate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_teacher_class, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val cls  = item.cls

        val grade   = cls.className ?: ""
        val section = cls.section?.let { " — $it" } ?: ""
        holder.tvName.text = "$grade$section"

        val meta = buildString {
            cls.subject?.let { append(it) }
            cls.schedule?.let { if (it.isNotBlank()) append(" · $it") }
            append(" · ${cls.studentCount ?: 0} students")
        }
        holder.tvMeta.text    = meta
        holder.tvPresent.text = "● ${item.presentCount} present"
        holder.tvRate.text    = "${item.attendanceRate}%"

        // Color rate badge by performance
        val rateColor = when {
            item.attendanceRate >= 80 -> "#10B981"
            item.attendanceRate >= 60 -> "#F59E0B"
            else                      -> "#EF4444"
        }
        holder.tvRate.setTextColor(android.graphics.Color.parseColor(rateColor))

        holder.itemView.setOnClickListener { onTake(cls) }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<TeacherClassItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}