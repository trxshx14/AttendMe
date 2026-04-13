package edu.cit.cararag.attendme.ui.admin

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import de.hdodenhof.circleimageview.CircleImageView
import edu.cit.cararag.attendme.R
import edu.cit.cararag.attendme.data.model.User
import edu.cit.cararag.attendme.data.remote.RetrofitClient
import edu.cit.cararag.attendme.data.repository.UserRepository
import edu.cit.cararag.attendme.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ManageUsersActivity : AppCompatActivity() {

    private val userRepo   = UserRepository()
    private val gson       = Gson()
    private val httpClient = OkHttpClient()
    private val jsonType   = "application/json".toMediaType()

    private lateinit var adapter: UserAdapter
    private lateinit var rvUsers: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var tvError: TextView
    private lateinit var etSearch: EditText
    private lateinit var tvStatTotal: TextView
    private lateinit var tvStatAdmins: TextView
    private lateinit var tvStatTeachers: TextView
    private lateinit var tvStatActive: TextView

    private var allUsers = listOf<User>()
    private lateinit var token: String

    // ── Dialog photo state ────────────────────────────────────────────────────
    private var dialogEditUser: User?         = null
    private var dialogSavedUserId: Long?      = null
    private var dialogSelectedPhotoUri: Uri?  = null
    private var dialogIvAvatar: CircleImageView? = null
    private var dialogLayoutInitials: LinearLayout? = null
    private var dialogTvInitials: TextView?   = null

    // Auto-refresh every 10 seconds
    private val refreshHandler  = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            loadUsers()
            refreshHandler.postDelayed(this, 10_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_users)

        token = SessionManager(this).getAccessToken() ?: ""

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        rvUsers        = findViewById(R.id.rvUsers)
        progressBar    = findViewById(R.id.progressBar)
        layoutEmpty    = findViewById(R.id.layoutEmpty)
        tvError        = findViewById(R.id.tvError)
        etSearch       = findViewById(R.id.etSearch)
        tvStatTotal    = findViewById(R.id.tvStatTotal)
        tvStatAdmins   = findViewById(R.id.tvStatAdmins)
        tvStatTeachers = findViewById(R.id.tvStatTeachers)
        tvStatActive   = findViewById(R.id.tvStatActive)

        adapter = UserAdapter(
            users          = emptyList(),
            token          = token,
            activity       = this,
            coroutineScope = lifecycleScope,
            onEdit         = { showUserDialog(it) },
            onToggleActive = { },
            onDelete       = { confirmDelete(it) },
            onRefresh      = { loadUsers() }
        )
        rvUsers.layoutManager = LinearLayoutManager(this)
        rvUsers.adapter       = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filterUsers(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        findViewById<FloatingActionButton>(R.id.fabAddUser).setOnClickListener {
            showUserDialog(null)
        }

        loadUsers()
        refreshHandler.postDelayed(refreshRunnable, 10_000)
    }

    override fun onResume() {
        super.onResume()
        loadUsers()
    }

    override fun onDestroy() {
        super.onDestroy()
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when {
            // ── Photo from list item avatar tap ──
            requestCode == UserAdapter.PICK_IMAGE_REQUEST && resultCode == RESULT_OK -> {
                data?.data?.let { uri -> adapter.handleImageResult(uri) }
            }
            // ── Photo from dialog Choose Photo button ──
            requestCode == UserAdapter.PICK_IMAGE_DIALOG_REQUEST && resultCode == RESULT_OK -> {
                data?.data?.let { uri ->
                    dialogSelectedPhotoUri = uri
                    // Show preview in dialog
                    dialogIvAvatar?.let { iv ->
                        dialogLayoutInitials?.visibility = View.GONE
                        iv.visibility = View.VISIBLE
                        Glide.with(this)
                            .load(uri)
                            .circleCrop()
                            .into(iv)
                    }
                }
            }
        }
    }

    private fun loadUsers() {
        lifecycleScope.launch {
            val result = userRepo.getAllUsers()
            allUsers = result.getOrElse { emptyList() }
            withContext(Dispatchers.Main) {
                tvStatTotal.text    = allUsers.size.toString()
                tvStatAdmins.text   = allUsers.count { it.role.uppercase() == "ADMIN" }.toString()
                tvStatTeachers.text = allUsers.count { it.role.uppercase() == "TEACHER" }.toString()
                tvStatActive.text   = allUsers.count { it.isOnline == true }.toString()
                filterUsers(etSearch.text.toString())
                showLoading(false)
            }
        }
    }

    private fun filterUsers(query: String) {
        val filtered = if (query.isBlank()) allUsers
        else allUsers.filter {
            it.fullName?.contains(query, ignoreCase = true) == true ||
                    it.email?.contains(query, ignoreCase = true) == true
        }
        adapter.updateData(filtered)
        layoutEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        rvUsers.visibility     = if (filtered.isEmpty()) View.GONE    else View.VISIBLE
    }

    private fun showUserDialog(editUser: User?) {
        // Reset dialog photo state
        dialogEditUser        = editUser
        dialogSelectedPhotoUri = null
        dialogSavedUserId     = editUser?.userId

        val view           = LayoutInflater.from(this).inflate(R.layout.dialog_user_form, null)
        val tvTitle        = view.findViewById<TextView>(R.id.tvUserDialogTitle)
        val tvErr          = view.findViewById<TextView>(R.id.tvUserDialogError)
        val etFullName     = view.findViewById<EditText>(R.id.etFullName)
        val etEmail        = view.findViewById<EditText>(R.id.etUserEmail)
        val spinner        = view.findViewById<Spinner>(R.id.spinnerRole)
        val etPassword     = view.findViewById<EditText>(R.id.etUserPassword)
        val tvPassLabel    = view.findViewById<TextView>(R.id.tvPasswordLabel)
        val btnCancel      = view.findViewById<MaterialButton>(R.id.btnCancelUser)
        val btnSave        = view.findViewById<MaterialButton>(R.id.btnSaveUser)
        val btnChoosePhoto = view.findViewById<MaterialButton>(R.id.btnChoosePhoto)
        val ivAvatar       = view.findViewById<CircleImageView>(R.id.ivDialogAvatar)
        val layoutInitials = view.findViewById<LinearLayout>(R.id.layoutDialogInitials)
        val tvInitials     = view.findViewById<TextView>(R.id.tvDialogInitials)

        // Keep references for onActivityResult
        dialogIvAvatar       = ivAvatar
        dialogLayoutInitials = layoutInitials
        dialogTvInitials     = tvInitials

        val roles = listOf("TEACHER", "ADMIN")
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)

        if (editUser != null) {
            tvTitle.text     = "Edit User"
            etFullName.setText(editUser.fullName)
            etEmail.setText(editUser.email)
            val idx = roles.indexOf(editUser.role.uppercase())
            if (idx >= 0) spinner.setSelection(idx)
            tvPassLabel.text = "NEW PASSWORD"
            etPassword.hint  = "Enter new password to reset — teacher will be notified by email"

            // Load existing profile picture
            if (!editUser.profilePicUrl.isNullOrBlank()) {
                layoutInitials.visibility = View.GONE
                ivAvatar.visibility       = View.VISIBLE
                Glide.with(this)
                    .load(editUser.profilePicUrl)
                    .signature(ObjectKey(System.currentTimeMillis().toString()))
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .circleCrop()
                    .into(ivAvatar)
            } else {
                ivAvatar.visibility       = View.GONE
                layoutInitials.visibility = View.VISIBLE
                val name = editUser.fullName ?: "?"
                tvInitials.text = name.split(" ")
                    .mapNotNull { it.firstOrNull()?.toString() }
                    .take(2).joinToString("").uppercase()
            }
        } else {
            // New user — show placeholder initials
            ivAvatar.visibility       = View.GONE
            layoutInitials.visibility = View.VISIBLE
            tvInitials.text           = "?"
        }

        // Choose Photo button opens gallery
        btnChoosePhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            startActivityForResult(intent, UserAdapter.PICK_IMAGE_DIALOG_REQUEST)
        }

        val dialog = AlertDialog.Builder(this).setView(view).create()
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val email    = etEmail.text.toString().trim()
            val role     = roles[spinner.selectedItemPosition]
            val password = etPassword.text.toString().trim()

            if (fullName.isBlank() || email.isBlank()) {
                tvErr.text = "Full name and email are required"
                tvErr.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (editUser == null && password.isBlank()) {
                tvErr.text = "Password is required for new users"
                tvErr.visibility = View.VISIBLE
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            btnSave.text      = "Saving..."

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    var savedUserId: Long? = editUser?.userId

                    val success = if (editUser == null) {
                        // ── CREATE ──
                        val username = email.split("@")[0].replace(Regex("[^a-zA-Z0-9]"), "")
                        val data = mapOf(
                            "username" to username,
                            "email"    to email,
                            "fullName" to fullName,
                            "password" to password,
                            "role"     to role
                        )
                        val req = Request.Builder()
                            .url("${RetrofitClient.BASE_URL_RAW}/api/auth/register")
                            .post(gson.toJson(data).toRequestBody(jsonType))
                            .build()
                        val response = httpClient.newCall(req).execute()
                        // Parse userId from response for photo upload
                        if (response.isSuccessful) {
                            try {
                                val body    = response.body?.string() ?: ""
                                val jsonObj = com.google.gson.JsonParser.parseString(body).asJsonObject
                                savedUserId = jsonObj.getAsJsonObject("data")
                                    ?.get("userId")?.asLong
                            } catch (_: Exception) {}
                            true
                        } else false

                    } else {
                        // ── UPDATE ──
                        val data = mutableMapOf<String, Any?>(
                            "fullName" to fullName,
                            "email"    to email,
                            "role"     to role
                        )
                        if (password.isNotBlank()) data["password"] = password

                        val req = Request.Builder()
                            .url("${RetrofitClient.BASE_URL_RAW}/api/users/${editUser.userId}")
                            .header("Authorization", "Bearer $token")
                            .put(gson.toJson(data).toRequestBody(jsonType))
                            .build()
                        httpClient.newCall(req).execute().isSuccessful
                    }

                    // ── Upload photo if selected ──
                    if (success && dialogSelectedPhotoUri != null && savedUserId != null) {
                        adapter.handleDialogImageResult(savedUserId!!, dialogSelectedPhotoUri!!)
                    }

                    withContext(Dispatchers.Main) {
                        if (success) {
                            dialog.dismiss()
                            loadUsers()
                            Toast.makeText(
                                this@ManageUsersActivity,
                                when {
                                    editUser == null          -> "User created! Welcome email sent."
                                    password.isNotBlank()     -> "User updated! Teacher notified by email."
                                    else                      -> "User updated!"
                                },
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            tvErr.text = "Failed to save user"
                            tvErr.visibility = View.VISIBLE
                            btnSave.isEnabled = true
                            btnSave.text = "Save"
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        tvErr.text = e.message ?: "An error occurred"
                        tvErr.visibility = View.VISIBLE
                        btnSave.isEnabled = true
                        btnSave.text = "Save"
                    }
                }
            }
        }
        dialog.show()
    }

    private fun confirmDelete(user: User) {
        AlertDialog.Builder(this)
            .setTitle("Delete User")
            .setMessage("Delete ${user.fullName}? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val req = Request.Builder()
                            .url("${RetrofitClient.BASE_URL_RAW}/api/users/${user.userId}")
                            .header("Authorization", "Bearer $token")
                            .delete()
                            .build()
                        val success = httpClient.newCall(req).execute().isSuccessful
                        withContext(Dispatchers.Main) {
                            if (success) loadUsers()
                            Toast.makeText(
                                this@ManageUsersActivity,
                                if (success) "User deleted" else "Failed to delete",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ManageUsersActivity,
                                "Error: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        rvUsers.visibility     = if (show) View.GONE    else View.VISIBLE
    }
}