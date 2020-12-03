package bo.young.bonews.adapters

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import bo.young.bonews.R
import bo.young.bonews.activities.PostActivity
import bo.young.bonews.utilities.Constants
import com.amplifyframework.core.Amplify
import com.amplifyframework.datastore.generated.model.Post
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.result.StorageDownloadFileResult
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.android.synthetic.main.list_item_main.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainAdapters(private val items: ArrayList<Post>, val context: Context, val profileIdCurrentUser: String) :
        RecyclerView.Adapter<MainAdapters.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Holds the TextView that will add each animal to
        val nameTextView: TextView = view.item_main_text_name
        val dateTextView: TextView = view.item_main_text_date
        val titleTextView: TextView = view.item_main_text_title
        val contentTextView: TextView = view.item_main_text_content
        val commentsTextView: TextView = view.item_main_text_comments
        val imageImageView: ImageView = view.item_main_image_postImage
        val progressbar: ProgressBar = view.item_main_progressbar
        val itemLayout: ConstraintLayout = view.item_main_layout_all
        val profileImageView: ImageView = view.item_main_image_profile_image
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
                LayoutInflater.from(context).inflate(
                        R.layout.list_item_main,
                        parent,
                        false
                )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        CoroutineScope(Main).launch {
            if (items[position].profile != null) {
                turnOnProgressBar(holder)
                val username = items[position].profile.username
                val name = if (items[position].profile.nickname == null) {
                    username
                } else {
                    items[position].profile.nickname
                }

                holder.nameTextView.text = name
                val date = items[position].date
                holder.dateTextView.text = date
                val title = items[position].title
                holder.titleTextView.text = title
                val content = items[position].contents
                holder.contentTextView.text = content
                val comments = items[position].comments.size
                val numberOfComments = "$comments Comments"
                holder.commentsTextView.text = numberOfComments

                val profileImageList = if (items[position].profile.profileImage == null) {
                    null
                } else if (items[position].profile.profileImage.size == 0) {
                    null
                } else {
                    items[position].profile.profileImage
                }
                if (profileImageList != null) {
                    profileImageList.sortByDescending { it.date }
                    val profileImageKey = profileImageList.first().profileImageKey
                    val profileImagePath = context.cacheDir.toString() + "/" + profileImageKey
                    loadProfileImage(profileImagePath, profileImageKey, holder, items[position].profile.hasImage)
                }
                val postId = items[position].id
                val image = "$postId.jpg"
                val filepath = context.cacheDir.toString() + "/$image"
                if (items[position].hasImage) {
                    loadImageFromS3(filepath, image, holder, items[position].hasImage)
                } else {
                    holder.imageImageView.visibility = View.GONE
                    hideProgressBar(holder)
                }
                holder.itemView.setOnClickListener {
                    val intent = Intent(context, PostActivity::class.java).apply {
                        putExtra(Constants.PROFILE_ID_CURRENTUSER, profileIdCurrentUser)
                        putExtra(Constants.PROFILE_ID, items[position].profile.id)
                        putExtra(Constants.POST_ID, postId)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }


    private suspend fun loadImageFromS3(filepath: String, image: String, holder: ViewHolder, hasImage: Boolean) =
            withContext(Main) {
                val file = File(filepath)
                if (hasImage) {
                    if (!file.exists()) {
                        withContext(IO) {
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
                                                        hideProgressBar(holder)
                                                        holder.imageImageView.visibility = View.VISIBLE
                                                        return false
                                                    }

                                                    override fun onResourceReady(
                                                            resource: Drawable?,
                                                            model: Any?,
                                                            target: Target<Drawable>?,
                                                            dataSource: DataSource?,
                                                            isFirstResource: Boolean
                                                    ): Boolean {
                                                        hideProgressBar(holder)
                                                        holder.imageImageView.visibility = View.VISIBLE
                                                        return false
                                                    }
                                                })
                                                .into(holder.imageImageView)
                                    },
                                    { error: StorageException? ->
                                        Log.e(
                                                "MyAmplifyApp",
                                                "Download Failure",
                                                error
                                        )
                                        hideProgressBar(holder)
                                        holder.imageImageView.visibility = View.GONE
                                    }
                            )
                        }

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
                                            hideProgressBar(holder)
                                            holder.imageImageView.visibility = View.GONE
                                            return false
                                        }

                                        override fun onResourceReady(
                                                resource: Drawable?,
                                                model: Any?,
                                                target: Target<Drawable>?,
                                                dataSource: DataSource?,
                                                isFirstResource: Boolean
                                        ): Boolean {
                                            hideProgressBar(holder)
                                            holder.imageImageView.visibility = View.VISIBLE
                                            return false
                                        }
                                    })
                                    .into(holder.imageImageView)
                        }
                    }


                    hideProgressBar(holder)
                    holder.imageImageView.visibility = View.VISIBLE

                }
            }

    private suspend fun loadProfileImage(filePath: String, imageKey: String, holder: ViewHolder, hasImage: Boolean) =
            withContext(Main) {
                val file = File(filePath)
                if (hasImage) {
                    if (!file.exists()) {
                        Amplify.Storage.downloadFile(
                                imageKey,
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
                                            .into(holder.profileImageView)
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
                                    .into(holder.profileImageView)
                        }
                    }
                }
            }

    private fun turnOnProgressBar(holder: ViewHolder) {
        holder.progressbar.visibility = View.VISIBLE
        holder.itemLayout.visibility = View.GONE
    }

    private fun hideProgressBar(holder: ViewHolder) {
        holder.progressbar.visibility = View.GONE
        holder.itemLayout.visibility = View.VISIBLE
    }

    override fun getItemCount(): Int {
        return items.size
    }
}

