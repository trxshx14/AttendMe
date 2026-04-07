package edu.cit.cararag.attendme.ui.admin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.google.android.material.button.MaterialButton
import de.hdodenhof.circleimageview.CircleImageView
import edu.cit.cararag.attendme.R
import edu.cit.cararag.attendme.data.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class UserAdapter(
    private var users: List<User>,
    private val token: String,
    private val activity: Activity,
    private val coroutineScope: CoroutineScope,
    private val onEdit: (User) -> Unit,
    private val onToggleActive: (User) -> Unit,
    private val onDelete: (User) -> Unit,
    private val onRefresh: () -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private val httpClient = OkHttpClient()
    private var pendingUploadUserId: Long? = null

    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val frameAvatar: FrameLayout     = view.findViewById(R.id.frameAvatar)
        val ivAvatar: CircleImageView    = view.findViewById(R.id.ivAvatar)
        val layoutInitials: LinearLayout = view.findViewById(R.id.layoutInitials)
        val tvInitials: TextView         = view.findViewById(R.id.tvInitials)
        val tvFullName: TextView         = view.findViewById(R.id.tvFullName)
        val tvEmail: TextView            = view.findViewById(R.id.tvEmail)
        val tvRole: TextView             = view.findViewById(R.id.tvRole)
        val tvStatus: TextView           = view.findViewById(R.id.tvStatus)
        val btnEdit: MaterialButton      = view.findViewById(R.id.btnEdit)
        val btnToggle: MaterialButton    = view.findViewById(R.id.btnToggleActive)
        val btnDelete: MaterialButton    = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        val name = user.fullName ?: user.username ?: "Unknown"

        holder.tvFullName.text = name
        holder.tvEmail.text    = user.email ?: ""
        holder.tvRole.text     = if (user.role.uppercase() == "ADMIN") "Admin" else "Teacher"

        // ✅ Use isOnline for status display instead of isActive
        val isOnline = user.isOnline == true
        holder.tvStatus.text = if (isOnline) "Online" else "Offline"
        holder.tvStatus.setTextColor(
            android.graphics.Color.parseColor(if (isOnline) "#16A34A" else "#DC2626")
        )
        holder.tvStatus.setBackgroundResource(
            if (isOnline) R.drawable.bg_status_active else R.drawable.bg_status_inactive
        )

        // ✅ Hide toggle button — status is now automatic based on login session
        holder.btnToggle.visibility = View.GONE

        loadAvatar(holder, user)

        holder.frameAvatar.setOnClickListener {
            pendingUploadUserId = user.userId
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            activity.startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        holder.btnEdit.setOnClickListener   { onEdit(user) }
        holder.btnDelete.setOnClickListener { onDelete(user) }
    }

    private fun loadAvatar(holder: UserViewHolder, user: User) {
        if (!user.profilePicUrl.isNullOrBlank()) {
            holder.ivAvatar.visibility       = View.VISIBLE
            holder.layoutInitials.visibility = View.GONE
            Glide.with(holder.ivAvatar.context)
                .load(user.profilePicUrl)
                .signature(ObjectKey(System.currentTimeMillis().toString()))
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(R.drawable.bg_circle_navy)
                .error(R.drawable.bg_circle_navy)
                .circleCrop()
                .into(holder.ivAvatar)
        } else {
            holder.ivAvatar.visibility       = View.GONE
            holder.layoutInitials.visibility = View.VISIBLE
            val name = user.fullName ?: user.username ?: "?"
            holder.tvInitials.text = name.split(" ")
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2).joinToString("").uppercase()
        }
    }

    fun handleImageResult(uri: Uri) {
        val userId = pendingUploadUserId ?: return
        pendingUploadUserId = null

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val inputStream = activity.contentResolver.openInputStream(uri) ?: return@launch
                val tempFile = File(activity.cacheDir, "upload_${userId}.jpg")
                FileOutputStream(tempFile).use { out -> inputStream.copyTo(out) }

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file", tempFile.name,
                        tempFile.asRequestBody("image/jpeg".toMediaType())
                    )
                    .build()

                val request = Request.Builder()
                    .url("http://10.0.2.2:8888/api/users/$userId/profile-picture")
                    .header("Authorization", "Bearer $token")
                    .post(requestBody)
                    .build()

                val response = httpClient.newCall(request).execute()
                val success  = response.isSuccessful

                Glide.get(activity).clearDiskCache()

                withContext(Dispatchers.Main) {
                    Glide.get(activity).clearMemory()
                    if (success) {
                        Toast.makeText(activity, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                        onRefresh()
                    } else {
                        Toast.makeText(activity, "Failed to upload picture", Toast.LENGTH_SHORT).show()
                    }
                }

                tempFile.delete()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun getItemCount() = users.size

    fun updateData(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }

    companion object {
        const val PICK_IMAGE_REQUEST = 1001
    }
}