package bo.young.bonews.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import bo.young.bonews.R
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
import com.amplifyframework.storage.result.StorageUploadFileResult
import com.bumptech.glide.Glide
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
    private var draftPost: Post? = null
    private var profileLoaded: Boolean = false
    private var hasDraft: Boolean = false
    private var postLoaded: Boolean = false
    private var imageChanged = false
    private var isLoading = false
    private var imageNumber = 0
    private var activityFrom = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)
        showProgressBar()
        val profileId = intent.getStringExtra(Constants.PROFILE_ID)
        val postId = intent.getStringExtra(Constants.POST_ID)
        activityFrom = intent.getIntExtra(Constants.ACTIVITY_FROM, 0)
        val coroutineScope = CoroutineScope(Main)
        coroutineScope.launch {
            if (postId != null) {
                isEdit = true
                uploadAct_text_save_draft.visibility = View.GONE
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
        uploadAct_image_save_bt.setOnClickListener {
            CoroutineScope(Main).launch {
                if (uploadAct_edit_title.text.toString() == "") {
                    Toast.makeText(context, "Title should not be empty", Toast.LENGTH_SHORT).show()
                } else {
                    showProgressBar()
                    val title = uploadAct_edit_title.text.toString()
                    val content = uploadAct_edit_content.text.toString()
                    if (profileList.size != 0) {
                        val profile = profileList[0]
                        if (imageChanged) {
                            imageNumber++
                        }
                        uploadPost(file, title, content, PostStatus.PUBLISHED, imageNumber, profile)
                    }
                }
            }
        }
        uploadAct_text_save_draft.setOnClickListener {
            CoroutineScope(Main).launch {
                if (uploadAct_edit_title.text.toString() == "") {
                    Toast.makeText(context, "Title should not be empty", Toast.LENGTH_SHORT).show()
                } else if (hasDraft && draftPost != null) {
                    showChangeDraftDialog()
                } else {
                    showProgressBar()
                    val title = uploadAct_edit_title.text.toString()
                    val content = uploadAct_edit_content.text.toString()
                    if (profileList.size != 0) {
                        val profile = profileList[0]
                        uploadPost(file, title, content, PostStatus.DRAFT, imageNumber, profile)
                    }
                }
            }
        }
        uploadAct_image_add_photo_bt.setOnClickListener {
            getImage()
        }
    }

    override fun onBackPressed() {
        if (!isLoading) {
            super.onBackPressed()
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
                                hasDraft = true
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
            imageNumber = draftItem.image
            val imagePath = "$cacheDir/$id-$imageNumber.jpg"
            val file = File(imagePath)
            if (draftItem.hasImage) {
                if (file.exists()) {
                    updateUIfromSavedDraft(draftItem)
                } else {
                    Amplify.Storage.downloadFile(
                        "$id-$imageNumber.jpg",
                        file,
                        {
                            updateUIfromSavedDraft(draftItem)
                        },
                        { error: StorageException? ->
                            Log.e(
                                "MyAmplifyApp",
                                "Download Failure",
                                error
                            )
                            updateUIfromSavedDraft(draftItem)
                        }
                    )
                }
            } else {
                updateUIfromSavedDraft(draftItem)
            }

        }
        builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
            draftPost = draftItem
            dialog.dismiss()
        }
        builder.show()
    }

    private fun showChangeDraftDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Another draft item found")
        builder.setMessage("Draft found. Do you want to delete the previous draft item and continue saving?")

        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            val draftItem = draftPost!!
            Amplify.API.mutate(
                ModelMutation.delete(draftItem),
                { result ->
                    Log.i("MyAmplifyApp", "postItem deleted ")
                    CoroutineScope(Main).launch {
                        val id = result.data.id
                        val imageNumber = result.data.image
                        if (result.data.hasImage) {
                            deleteImageFromS3("$id-$imageNumber.jpg")
                        }
                        val title = uploadAct_edit_title.text.toString()
                        val content = uploadAct_edit_content.text.toString()
                        if (profileList.size != 0) {
                            val profile = profileList[0]
                            uploadPost(file, title, content, PostStatus.DRAFT, 0, profile)
                        }
                    }
                },
                { error -> Log.e("MyAmplifyApp", "Create failed", error) })
        }
        builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun deleteImageFromS3(fileName: String) {
        Amplify.Storage.remove(
            fileName,
            { result -> Log.i("MyAmplifyApp", "Successfully removed: " + result.getKey()) },
            { error -> Log.e("MyAmplifyApp", "Remove failure", error) }
        )
    }

    private suspend fun queryPost(postId: String) = withContext(IO) {
        Amplify.API.query(
            ModelQuery.get(Post::class.java, postId),
            { response ->
                val postItem = response.data
                updateUIfromSavedDraft(postItem)
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

    private fun updateUIfromSavedDraft(postItem: Post) {
        post = postItem
        updateEdit(postItem.title, postItem.contents)
        if (postItem.hasImage) {
            hasImage = true
            val postId = postItem.id
            imageNumber = postItem.image
            val filepath = "$cacheDir/$postId-$imageNumber.jpg"
            loadImage(filepath)
        }
        postLoaded = true
    }

    private fun updateEdit(title: String, content: String) {
        runOnUiThread {
            uploadAct_edit_title.setText(title)
            uploadAct_edit_content.setText(content)
        }
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
                file = File(uploadHelper.getRealPath(this, data.data!!))
            }
            Glide.with(this)
                .load(data?.data)
                .into(uploadAct_image_postImage)
            hasImage = true
            imageChanged = true
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private suspend fun uploadPost(
        file: File?,
        title: String,
        content: String,
        status: PostStatus,
        imageNumber: Int,
        profile: Profile
    ) {
        if (post != null) {
            val postItem = post!!.copyOfBuilder()
                .title(title)
                .status(status)
                .date(getTodayDate())
                .profile(profile)
                .contents(content)
                .image(imageNumber)
                .hasImage(hasImage)
                .build()

            Amplify.API.mutate(
                ModelMutation.update(postItem),
                { response ->
                    val id = response.data.id
                    Log.i("MyAmplifyApp", "Todo with id: $id")
                    if (file != null && hasImage && imageChanged) {
                        CoroutineScope(Main).launch {
                            val prevKey = getImageKey(id, imageNumber - 1)
                            File("$cacheDir/$prevKey").deleteOnExit()
                            val key = getImageKey(id, imageNumber)
                            val fileUpdate = File("$cacheDir/$key")
                            file.copyTo(fileUpdate)
                            imageToS3(file, key, status)
                        }
                    } else {
                        requestResultToPost()
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
                .image(imageNumber)
                .hasImage(hasImage)
                .build()

            Amplify.API.mutate(
                ModelMutation.create(postItem),
                { response ->
                    val id = response.data.id
                    Log.i("MyAmplifyApp", "Todo with id: $id")
                    if (file != null && hasImage) {
                        CoroutineScope(IO).launch {
                            val key = getImageKey(id, imageNumber)
                            imageToS3(file, key, status)
                        }
                    } else {
                        requestResultToMain(status)
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

    private suspend fun getImageKey(postId: String, imageNumber: Int): String = withContext(Main) {
        val builder = StringBuilder()
        builder.append("$postId-$imageNumber.jpg")

        return@withContext builder.toString()
    }

    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy. MM. dd. HH:mm:ss")
        return sdf.format(Date())
    }

    private suspend fun imageToS3(file: File?, imageKey: String, status: PostStatus) = withContext(IO) {
        if (file != null) {
            Amplify.Storage.uploadFile(
                imageKey,
                file,
                StorageUploadFileOptions.defaultInstance(),
                { result: StorageUploadFileResult ->
                    Log.i("MyAmplifyApp", "Successfully uploaded: " + result.key)
                    if(activityFrom == Constants.POST_ACTIVITY) {
                        requestResultToPost()
                    } else if (activityFrom == Constants.MAIN_ACTIVITY) {
                        requestResultToMain(status)
                    }
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
        } else {
            if(activityFrom == Constants.POST_ACTIVITY) {
                requestResultToPost()
            } else if (activityFrom == Constants.MAIN_ACTIVITY) {
                requestResultToMain(status)
            }
            finish()
        }
    }

    private fun requestResultToPost() {
        val intentForResult = Intent()
        intentForResult.putExtra(Constants.POST_EDITED, true)
        setResult(Constants.REQUEST_EDIT, intentForResult)
    }

    private fun requestResultToMain(status: PostStatus) {
        if (status == PostStatus.PUBLISHED) {
            val resultIntent = intent.putExtra(Constants.POST_CREATED, true)
            setResult(Constants.UPLOAD, resultIntent)
        }
    }

    private fun showProgressBar() = runOnUiThread {
        runOnUiThread {
            isLoading = true
            uploadAct_layout_all.visibility = View.INVISIBLE
            uploadAct_progressbar.visibility = View.VISIBLE
        }
    }

    private fun hideProgressBar() = runOnUiThread {
        runOnUiThread {
            isLoading = false
            uploadAct_layout_all.visibility = View.VISIBLE
            uploadAct_progressbar.visibility = View.GONE
        }

    }
}