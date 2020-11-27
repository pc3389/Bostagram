package bo.young.bonews.activities

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import bo.young.bonews.R
import bo.young.bonews.adapters.PostAdapter
import bo.young.bonews.utilities.Constants
import com.amplifyframework.api.graphql.model.ModelMutation
import com.amplifyframework.api.graphql.model.ModelQuery
import com.amplifyframework.core.Amplify
import com.amplifyframework.datastore.generated.model.Post
import com.amplifyframework.datastore.generated.model.Profile
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

class PostActivity : AppCompatActivity() {
    private val context = this

    companion object {
        private val posts: ArrayList<Post> = ArrayList()
        private val thisPost: ArrayList<Post> = ArrayList()
        private var postNumber = 0
        private var postLoaded = false
        val coroutineScope = CoroutineScope(Main)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)

        postNumber = 0
        val profileId = intent.getStringExtra(Constants.PROFILE_ID)
        val postId = intent.getStringExtra(Constants.POST_ID)

        coroutineScope.launch {
            val currentUserName = withContext(IO) {Amplify.Auth.currentUser.username}
            if (postId != null && profileId != null) {
                queryPostById(postId, thisPost, profileId, currentUserName)
            }
        }
        postAct_image_back_bt.setOnClickListener {
            onBackPressed()
        }
    }

    private fun loadUI(profileId: String, postId: String, currentUserName: String) = runOnUiThread {
        val post = thisPost[0]
        val date = post.date
        val image = post.image
        val name = post.profile.nickname
        val username = post.profile.username
        val title = post.title
        val content = post.contents
        val profileImage = post.profile.profileImage
        val comments = post.comments.size

        val imagePath = "$cacheDir/$image"
        val profileImagePath = "$cacheDir/$profileImage"
        postAct_text_title.text = title
        postAct_text_date.text = date
        postAct_text_name.text = name
        postAct_text_content.text = content
        val commentSize = "$comments comments"
        postAct_text_comments.text = commentSize

        val recyclerTitle = "Other posts from $name"
        postAct_text_recycler_title.text = recyclerTitle

        CoroutineScope(Main).launch {
            loadProfileImage(profileImagePath, postAct_image_profile_image, context)
        }
        loadImage(imagePath)

        if (username != null) {
            setupRecycler(profileId, postId)
            setupMenu(username, profileId, currentUserName)
        }



        postAct_text_comments.setOnClickListener {
            if (thisPost[0].comments.size == 0) {
                val intent = Intent(context, CommentActivity::class.java).apply {
                    putExtra(Constants.PROFILE_ID, profileId)
                    putExtra(Constants.POST_ID, postId)
                }
                startActivity(intent)
            }
        }
        postAct_layout_profile.setOnClickListener {
            val intent = Intent(context, ProfileActivity::class.java).apply {
                putExtra(Constants.PROFILE_ID, profileId)
            }
            startActivity(intent)
        }
    }

    private fun loadImage(imagePath: String) {
        val file = File(imagePath)
        if (file.exists()) {
            Glide.with(context)
                .load(file)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        postAct_image_post_image.visibility = View.VISIBLE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        postAct_image_post_image.visibility = View.VISIBLE
                        return false
                    }
                })
                .into(postAct_image_post_image)
        }
    }

    private fun setupMenu(postUsername: String, profileId: String, currentUserName: String) {
        if (postUsername == currentUserName) {
            postAct_image_menu_bt.setOnClickListener {
                val popupMenu = PopupMenu(this@PostActivity, postAct_image_menu_bt)
                popupMenu.menuInflater.inflate(R.menu.menu_post, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener {
                    if (it.itemId == R.id.action_edit) {
                        val intent = Intent(context, UploadActivity::class.java).apply {
                            putExtra(Constants.PROFILE_ID, profileId)
                        }
                        startActivity(intent)
                    }
                    if (it.itemId == R.id.action_delete) {
                        CoroutineScope(Main).launch {
                            if (postLoaded) {
                                postLoaded = if (thisPost.size == 0) {
                                    Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
                                    false
                                } else {
                                    showDeleteDialog(thisPost[0])
                                    false
                                }
                            }
                        }
                    }
                    true
                }
                popupMenu.show()
            }
        } else {
            postAct_image_menu_bt.visibility = View.GONE
        }
    }

    private fun setupRecycler(profileId: String, postId: String) {
        val linearLayoutManager = LinearLayoutManager(context)
        postAct_rc_posts.layoutManager = linearLayoutManager
        CoroutineScope(Main).launch {
            queryPost(profileId, postId)
        }
    }

    private fun showDeleteDialog(deletePost: Post) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Post")
        builder.setMessage("Do you want to delete this post?")

        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            CoroutineScope(Main).launch {
                deletePostFromAWS(deletePost)
                finish()
            }
        }

        builder.setNegativeButton(android.R.string.cancel) { _, _ ->
            onBackPressed()
        }

        builder.show()
    }

    private suspend fun loadProfileImage(filePath: String, imageView: ImageView, context: Context) =
        withContext(Main) {
            val file = File(filePath)
            if (file.exists()) {
                val glideWork = CoroutineScope(Main).launch {
                    Glide.with(context)
                        .load(file)
                        .into(imageView)
                }
            }
        }

    private suspend fun queryPost(profileId: String, postId: String) = withContext(IO) {
        Amplify.API.query(
            ModelQuery.list(Profile::class.java, Profile.ID.contains(profileId)),
            { response ->
                posts.clear()
                for (profile in response.data) {
                    if (profile.id == profileId) {
                        for (post in profile.posts) {
                            if (post.id != postId) {
                                posts.add(post)
                            }

                        }
                    }
                }
                Log.i("MyAmplifyApp", "Posts added in recyclerview for PostActivity")
                CoroutineScope(Main).launch {
                    withContext(Default) {
                        posts.sortByDescending { it.date }
                    }
                    withContext(Main) {
                        runOnUiThread {
                            val fivePosts = getFivePosts(posts)
                            postAct_rc_posts.adapter = PostAdapter(fivePosts, context, profileId)
                            pageHelper(profileId, postId, posts)
                        }
                    }
                }
            },
            { error ->
                Log.e("MyAmplifyApp", "Query failure", error)
            }
        )
    }

    private suspend fun queryPostById(postId: String, postItem: ArrayList<Post>, profileId: String, currentUserName: String) =
        withContext(IO) {
            Amplify.API.query(
                ModelQuery.list(Post::class.java, Post.ID.contains(postId)),
                { response ->
                    postItem.clear()
                    for (post in response.data) {
                        if(postId == post.id) {
                            postItem.add(post)
                            postLoaded = true
                        }
                    }
                    loadUI(profileId, postId, currentUserName)
                },
                { error ->
                    Log.e("MyAmplifyApp", "Query failure", error)
                }
            )
        }

    private suspend fun deletePostFromAWS(postItem: Post) = withContext(IO) {
        Amplify.API.mutate(
            ModelMutation.delete(postItem),
            { Log.i("MyAmplifyApp", "postItem deleted ") },
            { error -> Log.e("MyAmplifyApp", "Create failed", error) }
        )
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

    private fun pageHelper(username: String?, id: String?, posts: ArrayList<Post>) {
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
                        postAct_frame_previous_page.isClickable = false
                        return false
                    }
                })
                .into(postAct_image_previous_page)
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
                        postAct_frame_previous_page.setOnClickListener {
                            if (username != null && id != null) {
                                postNumber -= 10
                                val fivePosts = getFivePosts(posts)
                                postAct_rc_posts.adapter = PostAdapter(fivePosts, context, username)
                                pageHelper(username, id, posts)
                            }
                        }
                        return false
                    }
                })
                .into(postAct_image_previous_page)
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
                        postAct_frame_next_page.isClickable = false
                        return false
                    }
                })
                .into(postAct_image_next_page)
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
                        postAct_frame_next_page.setOnClickListener {
                            if (username != null && id != null) {
                                val fivePosts = getFivePosts(posts)
                                postAct_rc_posts.adapter = PostAdapter(fivePosts, context, username)
                                pageHelper(username, id, posts)
                            }
                        }
                        return false
                    }
                })
                .into(postAct_image_next_page)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
