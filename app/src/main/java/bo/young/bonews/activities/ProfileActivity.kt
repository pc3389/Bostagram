package bo.young.bonews.activities

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import bo.young.bonews.R
import bo.young.bonews.adapters.PostAdapter
import bo.young.bonews.utilities.Constants
import bo.young.bonews.utilities.UploadHelper
import com.amplifyframework.api.graphql.model.ModelMutation
import com.amplifyframework.api.graphql.model.ModelQuery
import com.amplifyframework.core.Amplify
import com.amplifyframework.datastore.generated.model.Post
import com.amplifyframework.datastore.generated.model.Profile
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

class ProfileActivity : AppCompatActivity() {
    private val context: Context = this

    companion object {
        private var file: File? = null
        private val profiles = ArrayList<Profile>()
        private val posts: ArrayList<Post> = ArrayList()
        private var postNumber = 0
        private var hasProfile = false
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val profileId = intent.getStringExtra(Constants.PROFILE_ID)
        CoroutineScope(Main).launch {
            showProgressBar()
            val a = CoroutineScope(IO).launch {
                if (profileId != null) {
                    queryProfile(profileId)
                } else {
                    withContext(Main) {
                        hideProgressBar()
                        Toast.makeText(context, "Please create your profile", Toast.LENGTH_SHORT)
                                .show()
                        showEditProfile()
                    }
                }
            }

            val username = getUsername()

            profAct_text_username.text = username
        }

        profAct_image_camera.setOnClickListener {
            getImageFromGallery()
        }

        setupMenu()

        profAct_image_cancel_bt.setOnClickListener {
            hideEditProfile()
        }

        profAct_image_save_bt.setOnClickListener {
            CoroutineScope(Main).launch {
                val username = getUsername()
                val name = if (profAct_edit_name.text.toString() == "") {
                    username
                } else {
                    profAct_edit_name.text.toString()
                }
                val imageKey = if (file != null) {
                    getImageKey(username)
                } else null
                val email = profAct_text_email.text.toString()
                saveProfile(file, username, name, email, imageKey)
                showProgressBar()
                hideEditProfile()
            }
        }

        profAct_image_back_bt.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setupMenu() {
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
    }

    private suspend fun getEmail() = withContext(IO) {
        Amplify.Auth.fetchUserAttributes(
                {
                    Log.i("AuthDemo", "User attributes = $it")
                    runOnUiThread { profAct_text_email.text = it[0].value }

                },
                { Log.e("AuthDemo", "Failed to fetch user attributes. $it") }
        )
    }

    private suspend fun queryProfile(profileId: String) {
        withContext(IO) {
            Amplify.API.query(
                    ModelQuery.list(Profile::class.java, Profile.ID.contains(profileId)),
                    { response ->
                        for (profile in response.data) {
                            profiles.add(profile)
                            Log.i("MyAmplifyApp", profile.username)
                        }
                        if (profiles.isNotEmpty()) {
                            CoroutineScope(Main).launch {
                                updateUI(profiles[0])
                                hasProfile = true

                                if (profiles.size == 0) {
                                    Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
                                } else {
                                    setupRecycler(profiles[0].id)
                                }
                            }
                        }

                        hideProgressBar()
                    },
                    { error -> Log.e("MyAmplifyApp", "Query failure", error) }
            )
        }

    }

    private suspend fun updateUI(profile: Profile) = withContext(Main) {
        profAct_text_name.text = profile.nickname
        if (profile.profileImage != null && !isDestroyed) {
            val profileImage = profile.profileImage
            val file = File("$cacheDir/$profileImage")
            loadProfileImage(file, profileImage)
        }
    }

    private suspend fun saveProfile(
            file: File?,
            username: String,
            name: String,
            email: String,
            imageKey: String?
    ) =
            withContext(IO) {
                if (imageKey != null) {
                    imageToS3(file, imageKey)
                }

                val profile = Profile.builder()
                        .username(username)
                        .nickname(name)
                        .emailAddress(email)
                        .profileImage(imageKey)
                        .build()

                Amplify.API.mutate(
                        ModelMutation.create(profile),
                        { response ->
                            CoroutineScope(Main).launch {
                                queryProfile(response.data.id)
                            }
                            Log.i(
                                    "MyAmplifyApp",
                                    "Profile with name: " + response.data.nickname
                            )
                        },
                        { error -> Log.e("MyAmplifyApp", "Create failed", error) }
                )
            }

    private suspend fun getUsername(): String = withContext(IO) {
        return@withContext Amplify.Auth.currentUser.username
    }

    private suspend fun getImageKey(username: String): String = withContext(Main) {
        val builder = StringBuilder()
        builder.append(username)
        builder.append("_profile.jpg")

        return@withContext builder.toString()
    }

    private suspend fun imageToS3(file: File?, imageKey: String) = withContext(IO) {
        if (file != null) {
            Amplify.Storage.uploadFile(
                    imageKey,
                    file,
                    StorageUploadFileOptions.defaultInstance(),
                    { result: StorageUploadFileResult ->
                        Log.i(
                                "MyAmplifyApp",
                                "Successfully uploaded: " + result.key
                        )
                    },
                    { error: StorageException? ->
                        Log.e(
                                "MyAmplifyApp",
                                "Upload failed",
                                error
                        )
                    }
            )
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
                                        hideProgressBar()
                                        return false
                                    }

                                    override fun onResourceReady(
                                            resource: Drawable?,
                                            model: Any?,
                                            target: Target<Drawable>?,
                                            dataSource: DataSource?,
                                            isFirstResource: Boolean
                                    ): Boolean {
                                        hideProgressBar()
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
                        hideProgressBar()
                    }
            )
        } else {
            val glideWork = CoroutineScope(Main).launch {
                Glide.with(context)
                        .load(file)
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: Target<Drawable>?,
                                    isFirstResource: Boolean
                            ): Boolean {
                                hideProgressBar()
                                return false
                            }

                            override fun onResourceReady(
                                    resource: Drawable?,
                                    model: Any?,
                                    target: Target<Drawable>?,
                                    dataSource: DataSource?,
                                    isFirstResource: Boolean
                            ): Boolean {
                                hideProgressBar()
                                return false
                            }
                        })
                        .into(profAct_image_profile_image)
            }
            hideProgressBar()
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
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun setupRecycler(profileId: String) {
        val linearLayoutManager = LinearLayoutManager(context)
        profAct_rc_post.layoutManager = linearLayoutManager
        CoroutineScope(Main).launch {
            queryPost(profileId)
        }
    }

    private suspend fun queryPost(profileId: String) = withContext(Default) {
        withContext(Default) {
            for (postItem in profiles[0].posts) {
                posts.add(postItem)
            }
            posts.sortByDescending { it.date }
        }
        withContext(Main) {
            val fivePosts = getFivePosts(posts)
            profAct_rc_post.adapter = PostAdapter(fivePosts, context, profileId)
            pageHelper(profileId, posts)
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

    private fun pageHelper(username: String?, posts: ArrayList<Post>) {
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
                            profAct_frame_previous_page.isClickable = false
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
                            profAct_frame_previous_page.setOnClickListener {
                                if (username != null) {
                                    postNumber -= 10
                                    val fivePosts = getFivePosts(posts)
                                    profAct_rc_post.adapter = PostAdapter(fivePosts, context, username)
                                    pageHelper(username, posts)
                                }
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
                            profAct_frame_next_page.isClickable = false
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
                            profAct_frame_next_page.setOnClickListener {
                                if (username != null) {
                                    val fivePosts = getFivePosts(posts)
                                    profAct_rc_post.adapter = PostAdapter(fivePosts, context, username)
                                    pageHelper(username, posts)
                                }
                            }
                            return false
                        }
                    })
                    .into(profAct_image_next_page)
        }
    }

    override fun onBackPressed() {
        if (hasProfile) {
            if (profAct_layout_save_and_cancel.visibility == View.VISIBLE) {
                hideEditProfile()
                return
            } else {
                finish()
            }
            super.onBackPressed()
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
            profAct_text_name.visibility = View.VISIBLE
            profAct_layout_username_and_email.visibility = View.VISIBLE
            profAct_layout_postrc.visibility = View.VISIBLE
            profAct_edit_name.visibility = View.GONE
            profAct_layout_save_and_cancel.visibility = View.GONE
            profAct_image_camera.visibility = View.GONE
        }
    }

    private fun showProgressBar() {
        runOnUiThread {
            profAct_progressbar.visibility = View.VISIBLE
            profAct_all_layout.visibility = View.GONE
        }
    }

    private fun hideProgressBar() {
        runOnUiThread {
            profAct_progressbar.visibility = View.GONE
            profAct_all_layout.visibility = View.VISIBLE
        }
    }
}