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

    companion object {
        private var file: File? = null
        private var hasImage: Boolean = false
        private val profileList: ArrayList<Profile> = ArrayList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)
        turnOnProgressBar()
        val profileId = intent.getStringExtra(Constants.PROFILE_ID)
        val coroutineScope = CoroutineScope(Main)
        coroutineScope.launch {
            if (profileId != null) {
                queryProfile(profileId)
            } else {
                Toast.makeText(context, "Error occured", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        uploadAct_text_back_bt.setOnClickListener {
            onBackPressed()
        }
        uploadAct_text_save.setOnClickListener {
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
        uploadAct_text_add_photo_bt.setOnClickListener {
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
                    turnOffProgressBar()
                },
                { error ->
                    Log.e("MyAmplifyApp", "Query failure", error)
                    runOnUiThread {
                        turnOffProgressBar()
                    }
                }
        )
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
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private suspend fun uploadPost(
            file: File?,
            title: String,
            content: String,
            status: PostStatus,
            imageKey: String?,
            profile: Profile,
    ) =
            withContext(IO) {
                if (file != null && imageKey != null && hasImage) {
                    imageToS3(file, imageKey)
                }

                val post = Post.builder()
                        .title(title)
                        .status(status)
                        .date(getTodayDate())
                        .profile(profile)
                        .contents(content)
                        .image(imageKey)
                        .hasImage(hasImage)
                        .build()


                Amplify.API.mutate(
                        ModelMutation.create(post),
                        { response ->
                            Log.i("MyAmplifyApp", "Todo with id: " + response.data.id)
                            finish()
                        },
                        { error -> Log.e("MyAmplifyApp", "Create failed", error) }
                )
            }

    private suspend fun getUsername(): String = withContext(IO) {
        return@withContext Amplify.Auth.currentUser.username
    }

    private suspend fun getImageKey(title: String): String = withContext(Main) {
        val builder = StringBuilder()
        builder.append(getUsername())
        builder.append("_$title.jpg")

        return@withContext builder.toString()
    }

    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy. MM. dd. HH:mm")
        return sdf.format(Date())
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

    private fun turnOnProgressBar() {
        runOnUiThread {
            uploadAct_layout_all.visibility = View.INVISIBLE
            uploadAct_progressbar.visibility = View.VISIBLE
        }

    }

    private fun turnOffProgressBar() {
        runOnUiThread {
            uploadAct_layout_all.visibility = View.VISIBLE
            uploadAct_progressbar.visibility = View.GONE
        }

    }
}