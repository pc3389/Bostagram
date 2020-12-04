package bo.young.bonews.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import bo.young.bonews.R
import bo.young.bonews.adapters.MainAdapters
import bo.young.bonews.utilities.Constants
import bo.young.bonews.utilities.UploadHelper
import com.amplifyframework.api.graphql.model.ModelMutation
import com.amplifyframework.api.graphql.model.ModelQuery
import com.amplifyframework.core.Amplify
import com.amplifyframework.datastore.generated.model.Post
import com.amplifyframework.datastore.generated.model.PostStatus
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
import kotlinx.android.synthetic.main.activity_upload.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class UploadActivity : AppCompatActivity() {
    private val context = this

    private var file: File? = null
    private var hasImage: Boolean = false
    private val profileList: ArrayList<Profile> = ArrayList()
    private var isEdit: Boolean = false
    private var post: Post? = null
    private var profileLoaded: Boolean = false
    private var postLoaded: Boolean = false
    private var imageChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)
        showProgressBar()
        val profileId = intent.getStringExtra(Constants.PROFILE_ID)
        val postId = intent.getStringExtra(Constants.POST_ID)
        val coroutineScope = CoroutineScope(Main)
        coroutineScope.launch {
            if (postId != null) {
                isEdit = true
                queryPost(postId)
            }
            if (profileId != null) {
                queryProfile(profileId)
            } else {
                Toast.makeText(context, "Error occured", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        uploadAct_image_back_bt.setOnClickListener {
            onBackPressed()
        }
        uploadAct_image_save.setOnClickListener {
            showProgressBar()
            CoroutineScope(Main).launch {
                if (uploadAct_edit_title.text.toString() == "") {
                    Toast.makeText(context, "Title should not be empty", Toast.LENGTH_SHORT).show()
                } else {
                    val title = uploadAct_edit_title.text.toString()
                    val imageKey = if (hasImage) {
                        getImageKey(title)
                    } else {
                        null
                    }
                    val content = uploadAct_edit_content.text.toString()
                    if (profileList.size != 0) {
                        val profile = profileList[0]
                        uploadPost(file, title, content, PostStatus.PUBLISHED, imageKey, profile)
                    }
                }
            }
        }
        uploadAct_text_save_draft.setOnClickListener {
            CoroutineScope(Main).launch {
                showProgressBar()
                if (uploadAct_edit_title.text.toString() == "") {
                    Toast.makeText(context, "Title should not be empty", Toast.LENGTH_SHORT).show()
                } else {
                    val title = uploadAct_edit_title.text.toString()
                    val imageKey = if (hasImage) {
                        getImageKey(title)
                    } else {
                        null
                    }
                    val content = uploadAct_edit_content.text.toString()
                    if (profileList.size != 0) {
                        val profile = profileList[0]
                        uploadPost(file, title, content, PostStatus.DRAFT, imageKey, profile)
                    }
                }
            }
        }
        uploadAct_image_add_photo_bt.setOnClickListener {
            getImage()
        }
    }

    private suspend fun queryProfile(profileId: String) = withContext(IO) {
        Amplify.API.query(
                ModelQuery.get(Profile::class.java, profileId),
                { response ->
                    if (response.data.id == profileId) {
                        profileList.add(response.data)
                        Log.i("MyAmplifyApp", response.data.username + "is added")
                    }
                    profileLoaded = true
                    if (!isEdit) {
                        for (postItem in response.data.posts) {
                            if (postItem.status == PostStatus.DRAFT) {
                                runOnUiThread {
                                    showDraftDialog(postItem)
                                }
                            }
                        }
                    }
                    if (isEdit) {
                        if (postLoaded) {
                            hideProgressBar()
                        }
                    } else {
                        hideProgressBar()
                    }
                },
                { error ->
                    Log.e("MyAmplifyApp", "Query failure", error)
                    hideProgressBar()
                }
        )
    }

    private fun showDraftDialog(draftItem: Post) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Draft")
        builder.setMessage("Draft found. Do you want to continue writing the post?")

        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            isEdit = true
            val id = draftItem.id
            val imagePath = "$cacheDir/$id.jpg"
            val file = File(imagePath)
            if (draftItem.hasImage) {
                if (file.exists()) {
                    updateUIfromSavedState(draftItem)
                } else {
                    Amplify.Storage.downloadFile(
                            "$id.jpg",
                            file,
                            { result: StorageDownloadFileResult ->
                                updateUIfromSavedState(draftItem)
                            },
                            { error: StorageException? ->
                                Log.e(
                                        "MyAmplifyApp",
                                        "Download Failure",
                                        error
                                )
                                updateUIfromSavedState(draftItem)
                            }
                    )
                }
            }

        }
        builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }


    private suspend fun queryPost(postId: String) = withContext(IO) {
        Amplify.API.query(
                ModelQuery.get(Post::class.java, postId),
                { response ->
                    val postItem = response.data
                    updateUIfromSavedState(postItem)
                    if (profileLoaded) {
                        hideProgressBar()
                    }
                },
                { error ->
                    Log.e("MyAmplifyApp", "Query failure", error)
                    runOnUiThread {
                    }
                }
        )
    }

    private fun updateUIfromSavedState(postItem: Post) {
        post = postItem
        updateEdit(postItem.title, postItem.contents, postItem.id)
        if (postItem.hasImage) {
            hasImage = true
        }
        postLoaded = true
    }

    private fun updateEdit(title: String, content: String, postId: String) {
        runOnUiThread {
            uploadAct_edit_title.setText(title)
            uploadAct_edit_content.setText(content)
        }
        val filepath = "$cacheDir/$postId.jpg"
        loadImage(filepath)
    }

    private fun loadImage(filepath: String) {
        val file = File(filepath)
        if (file.exists()) {
            runOnUiThread {
                Glide.with(context)
                        .load(file)
                        .into(uploadAct_image_postImage)
            }
        }
    }

    private fun getImage() {
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

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        when (requestCode) {
            Constants.PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED
                ) {
                    //permission from popup granted
                    pickImageFromGallery()
                } else {
                    //permission from popup denied
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
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
                    .into(uploadAct_image_postImage)
            hasImage = true
            imageChanged = true
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private suspend fun uploadPost(file: File?, title: String, content: String, status: PostStatus, imageKey: String?, profile: Profile) {

        if (post != null) {
            val postItem = post!!.copyOfBuilder()
                    .title(title)
                    .status(status)
                    .date(getTodayDate())
                    .profile(profile)
                    .contents(content)
                    .image(imageKey)
                    .hasImage(hasImage)
                    .build()

            Amplify.API.mutate(
                    ModelMutation.update(postItem),
                    { response ->
                        val id = response.data.id
                        Log.i("MyAmplifyApp", "Todo with id: $id")
                        if (file != null && hasImage && imageChanged) {
                            CoroutineScope(IO).launch {
                                val key = getImageKey(id)
                                imageToS3(file, key)
                            }
                        } else {
                            finish()
                        }
                    },
                    { error ->
                        Log.e("MyAmplifyApp", "Create failed", error)
                        hideProgressBar()
                    }
            )
        } else {
            val postItem = Post.builder()
                    .title(title)
                    .status(status)
                    .date(getTodayDate())
                    .profile(profile)
                    .contents(content)
                    .image(imageKey)
                    .hasImage(hasImage)
                    .build()

            Amplify.API.mutate(
                    ModelMutation.create(postItem),
                    { response ->
                        val id = response.data.id
                        Log.i("MyAmplifyApp", "Todo with id: $id")
                        if (file != null && hasImage) {
                            CoroutineScope(IO).launch {
                                val key = getImageKey(id)
                                imageToS3(file, key)
                            }
                        } else {
                            finish()
                        }
                    },
                    { error ->
                        Log.e("MyAmplifyApp", "Create failed", error)
                        hideProgressBar()
                    }
            )
        }
    }

    private suspend fun getImageKey(postId: String): String = withContext(Main) {
        val builder = StringBuilder()
        builder.append("$postId.jpg")

        return@withContext builder.toString()
    }

    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy. MM. dd. HH:mm:ss")
        return sdf.format(Date())
    }

    private suspend fun imageToS3(file: File?, imageKey: String) = withContext(IO) {
        if (file != null) {
            Amplify.Storage.uploadFile(
                    imageKey,
                    file,
                    StorageUploadFileOptions.defaultInstance(),
                    { result: StorageUploadFileResult ->
                        Log.i("MyAmplifyApp", "Successfully uploaded: " + result.key)
                        finish()
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

    private fun showProgressBar() = runOnUiThread {
        runOnUiThread {
            uploadAct_layout_all.visibility = View.INVISIBLE
            uploadAct_progressbar.visibility = View.VISIBLE
        }

    }

    private fun hideProgressBar() = runOnUiThread {
        runOnUiThread {
            uploadAct_layout_all.visibility = View.VISIBLE
            uploadAct_progressbar.visibility = View.GONE
        }

    }
}