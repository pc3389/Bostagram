package bo.young.bonews.activities

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import bo.young.bonews.R
import bo.young.bonews.adapters.PostAdapter
import bo.young.bonews.utilities.Constants
import bo.young.bonews.utilities.UploadHelper
import com.amplifyframework.api.graphql.model.ModelMutation
import com.amplifyframework.api.graphql.model.ModelQuery
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.core.Amplify
import com.amplifyframework.datastore.generated.model.Comment
import com.amplifyframework.datastore.generated.model.Post
import com.amplifyframework.datastore.generated.model.Profile
import com.amplifyframework.datastore.generated.model.ProfileImage
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.options.StorageUploadFileOptions
import com.amplifyframework.storage.result.StorageDownloadFileResult
import com.amplifyframework.storage.result.StorageUploadFileResult
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.android.synthetic.main.activity_post.*
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import java.io.File
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ProfileActivity : AppCompatActivity() {
    private val context = this

    private var file: File? = null
    private val profiles = ArrayList<Profile>()
    private val posts: ArrayList<Post> = ArrayList()
    private var postNumber = 0
    private var hasProfile = false
    private var hasImage = false
    private var imageChanged = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val profileId = intent.getStringExtra(Constants.PROFILE_ID)
        val currentUserProfileId = intent.getStringExtra(Constants.PROFILE_ID_CURRENTUSER)
        CoroutineScope(Main).launch {
            showProgressBar()
            if (profileId != null && currentUserProfileId != null) {
                queryProfile(profileId, currentUserProfileId)
            } else {
                Toast.makeText(context, "Please create your profile", Toast.LENGTH_SHORT)
                        .show()
                showEditProfile()
                val username = getUsername()
                profAct_text_username.text = username
                getEmail()
            }
        }

        profAct_image_camera.setOnClickListener {
            getImageFromGallery()
        }

        profAct_image_cancel_bt.setOnClickListener {
            showCancelDialog()
        }

        profAct_image_back_bt.setOnClickListener {
            onBackPressed()
        }

        profAct_image_save_bt.setOnClickListener {
            CoroutineScope(Main).launch {
                postNumber = 0
                hideKeyboard()
                hideEditProfile()
                val username = getUsername()
                val name = if (profAct_edit_name.text.toString() == "") {
                    username
                } else {
                    profAct_edit_name.text.toString()
                }
                val number = if (profiles[0].profileImage.isEmpty()) {
                    1
                } else {
                    profiles[0].profileImage.first().number + 1
                }
                val imageKey = if (file != null) {
                    getImageKey(username, number)
                } else null
                val email = profAct_text_email.text.toString()
                if (hasProfile) {
                    updateProfile(file, username, name, email, imageKey, hasImage, number)
                } else {
                    createProfile(file, username, name, email, imageKey, hasImage, number)
                }
            }
        }
    }

    private fun setupMenu(username: String) {
        if (username == profiles[0].username) {
            profAct_image_menu_bt.setOnClickListener {
                val popupMenu = PopupMenu(this@ProfileActivity, profAct_image_menu_bt)
                popupMenu.menuInflater.inflate(R.menu.menu_profile, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener {
                    if (it.itemId == R.id.action_edit) {
                        val name = profAct_text_name.text.toString()
                        profAct_edit_name.setText(name)
                        showEditProfile()
                    }
                    true
                }
                popupMenu.show()
            }
        } else {
            profAct_image_menu_bt.visibility = View.GONE
        }
    }

    private suspend fun getEmail() = withContext(IO) {
        Amplify.Auth.fetchUserAttributes(
                {
                    Log.i("AuthDemo", "User attributes = $it")
                    for (attribute in it) {
                        if (attribute.key == AuthUserAttributeKey.email()) {
                            runOnUiThread { profAct_text_email.text = attribute.value }
                        }
                    }


                },
                { Log.e("AuthDemo", "Failed to fetch user attributes. $it") }
        )
    }

    private suspend fun queryProfile(profileId: String, currentUserProfileId: String) = withContext(IO) {
        Amplify.API.query(
                ModelQuery.get(Profile::class.java, profileId),
                { response ->
                    profiles.clear()
                    val profile = response.data
                    profiles.add(profile)
                    Log.i("MyAmplifyApp", profile.username)

                    if (profiles.isNotEmpty()) {
                        CoroutineScope(Main).launch {
                            hasProfile = true
                            hasImage = profiles[0].hasImage
                            runOnUiThread {
                                profAct_text_username.text = profile.username
                                profAct_text_email.text = profile.emailAddress
                            }
                            updateUI(profiles[0])
                            setupRecycler()
                            queryPost(currentUserProfileId)
                        }
                    }
                    CoroutineScope(Main).launch {
                        setupMenu(getUsername())
                    }
                },
                { error -> Log.e("MyAmplifyApp", "Query failure", error) }
        )
    }

    private suspend fun updateUI(profile: Profile) = withContext(Main) {
        profAct_text_name.text = profile.nickname
        if (profile.profileImage != null && !isDestroyed) {
            if (profile.profileImage.size != 0) {
                val profileImageList = profiles[0].profileImage
                profileImageList.sortByDescending { it.date }
                val imageKey = profileImageList.first().profileImageKey
                val file = File("$cacheDir/$imageKey")
                loadProfileImage(file, imageKey)
            }
        }
        hideProgressBar()
    }


    private suspend fun createProfile(file: File?, username: String, name: String, email: String, imageKey: String?, hasImage: Boolean, imageNumber: Int) =
            withContext(IO) {
                if (imageKey != null && hasImage && imageChanged) {
                    val date = getTodayDate()
                    createProfileImage(date, imageNumber, imageKey)
                    imageToS3(file, username, name, email, imageKey, hasImage, false)
                }

                val profile = Profile.builder()
                        .username(username)
                        .nickname(name)
                        .emailAddress(email)
                        .hasImage(hasImage)
                        .build()

                Amplify.API.mutate(
                        ModelMutation.create(profile),
                        { response ->
                            CoroutineScope(Main).launch {
                                profAct_text_name.text = name
                                hideProgressBar()
                            }
                            Log.i(
                                    "MyAmplifyApp",
                                    "Profile with name: " + response.data.nickname
                            )
                        },
                        { error ->
                            Log.e("MyAmplifyApp", "Create failed", error)
                            hideProgressBar()
                        }
                )
            }

    private suspend fun createProfileImage(date: String, profileNumber: Int, imageKey: String) = withContext(IO) {
        val profileImage = ProfileImage.builder()
                .date(date)
                .number(profileNumber)
                .profileImageKey(imageKey)
                .profile(profiles[0])
                .build()

        Amplify.API.mutate(
                ModelMutation.create(profileImage),
                { response ->
                    Log.i("MyAmplifyApp", "ProfileImage added: " + response.data.profileImageKey)
                },
                { error -> Log.e("MyAmplifyApp", "Create failed", error) }
        )
    }

    private suspend fun updateProfile(file: File?, username: String, name: String, email: String, imageKey: String?, hasImage: Boolean, imageNumber: Int) = withContext(IO) {
        if (imageKey != null && hasImage && imageChanged) {
            val date = getTodayDate()
            createProfileImage(date, imageNumber, imageKey)
            imageToS3(file, username, name, email, imageKey, hasImage, true)
        } else {
            upload(username, name, email, hasImage)
        }
    }

    private suspend fun upload(username: String, name: String, email: String, hasImage: Boolean) = withContext(IO) {
        val profile = profiles[0].copyOfBuilder()
                .username(username)
                .nickname(name)
                .emailAddress(email)
                .hasImage(hasImage)
                .build()

        Amplify.API.mutate(
                ModelMutation.update(profile),
                { response ->
                    CoroutineScope(Main).launch {
                        profAct_text_name.text = name
                        hideProgressBar()
                    }
                    Log.i("MyAmplifyApp", "Profile with name updated: " + response.data.nickname)
                },
                { error ->
                    Log.e("MyAmplifyApp", "Create failed", error)
                    hideProgressBar()
                }
        )
    }


    private suspend fun getUsername(): String = withContext(IO) {
        return@withContext Amplify.Auth.currentUser.username
    }

    private suspend fun getImageKey(username: String, number: Int): String = withContext(Main) {
        val builder = StringBuilder()
        builder.append(username)
        builder.append("_profile_$number.jpg")

        return@withContext builder.toString()
    }

    private suspend fun imageToS3(file: File?, username: String, name: String, email: String, imageKey: String, hasImage: Boolean, isUpload: Boolean) {
        if (file != null) {
            withContext(Main) {
                val photoInCache = File("$cacheDir/$imageKey")
                if (photoInCache.exists()) {
                    photoInCache.delete()
                }
            }
            withContext(IO) {
                Amplify.Storage.uploadFile(
                        imageKey,
                        file,
                        StorageUploadFileOptions.defaultInstance(),
                        { result2: StorageUploadFileResult ->
                            Log.i("MyAmplifyApp", "Successfully uploaded: " + result2.key)
                            if (isUpload) {
                                CoroutineScope(Main).launch { upload(username, name, email, hasImage) }
                            }
                        },
                        { error: StorageException? ->
                            Log.e("MyAmplifyApp", "Upload failed", error)
                        }
                )
            }
        }
    }


    private suspend fun loadProfileImage(file: File, image: String) = withContext(IO) {
        if (!file.exists()) {
            Amplify.Storage.downloadFile(
                    image,
                    file,
                    { result: StorageDownloadFileResult ->
                        Glide.with(context)
                                .load(result.file)
                                .listener(object : RequestListener<Drawable> {
                                    override fun onLoadFailed(
                                            e: GlideException?,
                                            model: Any?,
                                            target: Target<Drawable>?,
                                            isFirstResource: Boolean
                                    ): Boolean {
                                        return false
                                    }

                                    override fun onResourceReady(
                                            resource: Drawable?,
                                            model: Any?,
                                            target: Target<Drawable>?,
                                            dataSource: DataSource?,
                                            isFirstResource: Boolean
                                    ): Boolean {
                                        return false
                                    }
                                })
                                .into(profAct_image_profile_image)
                    },
                    { error: StorageException? ->
                        Log.e(
                                "MyAmplifyApp",
                                "Download Failure",
                                error
                        )
                    }
            )
        } else {
            CoroutineScope(Main).launch {
                Glide.with(context)
                        .load(file)
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: Target<Drawable>?,
                                    isFirstResource: Boolean
                            ): Boolean {
                                return false
                            }

                            override fun onResourceReady(
                                    resource: Drawable?,
                                    model: Any?,
                                    target: Target<Drawable>?,
                                    dataSource: DataSource?,
                                    isFirstResource: Boolean
                            ): Boolean {
                                return false
                            }
                        })
                        .into(profAct_image_profile_image)
            }
        }
    }

    private fun getImageFromGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_DENIED
            ) {
                //permission denied
                val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE);
                //show popup to request runtime permission
                requestPermissions(permissions, Constants.PERMISSION_CODE);
            } else {
                //permission already granted
                pickImageFromGallery();
            }
        } else {
            //system OS is < Marshmallow
            pickImageFromGallery();
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, Constants.IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == Constants.IMAGE_PICK_CODE) {

            val uploadHelper = UploadHelper()
            if (data?.data != null) {
                file = File(uploadHelper.getRealPath(this, data?.data!!))
            }
            Glide.with(this)
                    .load(data?.data)
                    .into(profAct_image_profile_image)
            hasImage = true
            imageChanged = true
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun setupRecycler() {
        val linearLayoutManager = LinearLayoutManager(context)
        profAct_rc_post.layoutManager = linearLayoutManager
    }

    private suspend fun queryPost(currentUserProfileId: String) = withContext(Default) {
        withContext(Default) {
            posts.clear()
            for (postItem in profiles[0].posts) {
                posts.add(postItem)
            }
            posts.sortByDescending { it.date }
        }
        withContext(Main) {
            val fivePosts = getFivePosts(posts)
            profAct_rc_post.adapter = PostAdapter(fivePosts, context, currentUserProfileId)
            pageHelper(posts, currentUserProfileId)
        }
    }


    private fun getFivePosts(posts: ArrayList<Post>): ArrayList<Post> {
        val end = postNumber + 5
        val fivePosts: ArrayList<Post> = ArrayList()
        while (postNumber < end && posts.size > postNumber) {
            fivePosts.add(posts[postNumber])
            postNumber += 1
        }
        if (postNumber % 5 != 0) {
            postNumber += 5 - postNumber % 5
        }
        return fivePosts
    }

    private fun pageHelper(posts: ArrayList<Post>, profileIdCurrentUser: String) {
        if (postNumber - 5 <= 0) {
            Glide.with(context)
                    .load(R.drawable.previous_page_unavailable_24)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>?,
                                isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                        ): Boolean {
                            profAct_image_previous_page.isClickable = false
                            return false
                        }
                    })
                    .into(profAct_image_previous_page)
        } else {
            Glide.with(context)
                    .load(R.drawable.previous_page_available)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>?,
                                isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                        ): Boolean {
                            profAct_image_previous_page.setOnClickListener {
                                postNumber -= 10
                                val fivePosts = getFivePosts(posts)
                                profAct_rc_post.adapter = PostAdapter(fivePosts, context, profileIdCurrentUser)
                                pageHelper(posts, profileIdCurrentUser)
                            }
                            return false
                        }
                    })
                    .into(profAct_image_previous_page)
        }
        if (postNumber >= posts.size) {
            Glide.with(context)
                    .load(R.drawable.next_page_unavailable_24)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>?,
                                isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                        ): Boolean {
                            profAct_image_next_page.isClickable = false
                            return false
                        }
                    })
                    .into(profAct_image_next_page)
        } else {
            Glide.with(context)
                    .load(R.drawable.next_page_available_24)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>?,
                                isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                        ): Boolean {
                            profAct_image_next_page.setOnClickListener {
                                val fivePosts = getFivePosts(posts)
                                profAct_rc_post.adapter = PostAdapter(fivePosts, context, profileIdCurrentUser)
                                pageHelper(posts, profileIdCurrentUser)
                            }
                            return false
                        }
                    })
                    .into(profAct_image_next_page)
        }
    }

    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy. MM. dd. HH:mm:ss")
        return sdf.format(Date())
    }

    override fun onBackPressed() {
        if (hasProfile) {
            if (profAct_layout_save_and_cancel.visibility == View.VISIBLE) {
                hideEditProfile()
                return
            } else {
                super.onBackPressed()
                finish()
            }
        }
    }


    private fun showEditProfile() {
        runOnUiThread {
            profAct_text_name.visibility = View.GONE
            profAct_layout_username_and_email.visibility = View.GONE
            profAct_layout_postrc.visibility = View.GONE
            profAct_edit_name.visibility = View.VISIBLE
            profAct_layout_save_and_cancel.visibility = View.VISIBLE
            profAct_image_camera.visibility = View.VISIBLE
        }
    }

    private fun hideEditProfile() {
        runOnUiThread {
            profAct_layout_all.visibility = View.INVISIBLE
            profAct_layout_postrc.visibility = View.VISIBLE
            profAct_text_name.visibility = View.VISIBLE
            profAct_layout_username_and_email.visibility = View.VISIBLE
            profAct_edit_name.visibility = View.GONE
            profAct_layout_save_and_cancel.visibility = View.GONE
            profAct_image_camera.visibility = View.GONE
            profAct_layout_all.visibility = View.VISIBLE
        }
    }

    private fun showProgressBar() {
        runOnUiThread {
            profAct_progressbar.visibility = View.VISIBLE
            profAct_layout_all.visibility = View.GONE
        }
    }

    private fun hideProgressBar() {
        runOnUiThread {
            profAct_progressbar.visibility = View.GONE
            profAct_layout_all.visibility = View.VISIBLE
        }
    }

    private fun hideKeyboard() {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    Log.d("focus", "touchevent")
                    v.clearFocus()
                    hideKeyboard()
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun showCancelDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Cancel Editing")
        builder.setMessage("Do you want to cancel editing?")

        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            hideKeyboard()
            onBackPressed()
        }
        builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }
}