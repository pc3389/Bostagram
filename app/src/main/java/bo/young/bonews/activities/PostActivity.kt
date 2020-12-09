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
import bo.young.bonews.interfaces.CallbackListener
import bo.young.bonews.utilities.Constants
import com.amplifyframework.api.graphql.model.ModelMutation
import com.amplifyframework.api.graphql.model.ModelQuery
import com.amplifyframework.core.Amplify
import com.amplifyframework.datastore.generated.model.Like
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

class PostActivity : AppCompatActivity(), CallbackListener {
    private val context = this
    private val posts: ArrayList<Post> = ArrayList()
    private val currentPostList: ArrayList<Post> = ArrayList()
    private var postNumber = 0
    private var postLoaded = false
    private var hasLike = false
    private val coroutineScope = CoroutineScope(Main)
    private var likeStatus = false
    private var currentLike: Like? = null
    private var postEdited = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)
        setupUI()
    }

    override fun onResume() {
        refreshCommentNumber()
        super.onResume()
    }

    private fun setupUI() {
        postNumber = 0
        showProgressbar()
        val profileIdCurrentUser = intent.getStringExtra(Constants.PROFILE_ID_CURRENTUSER)
        val postId = intent.getStringExtra(Constants.POST_ID)

        coroutineScope.launch {
            val currentUserName = withContext(IO) { Amplify.Auth.currentUser.username }
            if (postId != null && profileIdCurrentUser != null) {
                queryPostById(postId, currentPostList, currentUserName, profileIdCurrentUser)
            }
        }
        postAct_image_back_bt.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        if(postEdited) {
            val intentForResult = Intent()
            intentForResult.putExtra(Constants.POST_EDITED, true)
            setResult(Constants.POST, intentForResult)
        }
        super.onBackPressed()
    }

    private fun loadUI(
        profileId: String,
        postId: String,
        currentUserName: String,
        profileIdCurrentUser: String
    ) = runOnUiThread {
        val post = currentPostList[0]
        val date = post.date
        val imageNumber = post.image
        val image = "$postId-$imageNumber.jpg"
        val name = post.profile.nickname
        val username = post.profile.username
        val title = post.title
        val content = post.contents
        val profileImage = post.profile.profileImage
        val comments = post.comments.size
        var likes = 0

        for(likeItem in post.likes) {
            if(likeItem.like) {
                likes++
            }
            if(likeItem.profileId == profileIdCurrentUser) {
                hasLike = true
                likeStatus = likeItem.like
                currentLike = likeItem
                hasLike = true
                if(likeStatus) {
                    postAct_image_like_bt.setImageResource(R.drawable.ic_thumb_up_selected_24)
                }
            }
        }

        val profileImageKey = if (profileImage.isNotEmpty()) {
            profileImage.sortByDescending { it.date }
            profileImage.first().profileImageKey
        } else {
            "NoImage"
        }

        val imagePath = "$cacheDir/$image"
        val profileImagePath = "$cacheDir/$profileImageKey"
        postAct_text_title.text = title
        postAct_text_date.text = date
        postAct_text_name.text = name
        postAct_text_content.text = content
        postAct_text_comments_number.text = comments.toString()
        postAct_text_likes_number.text = likes.toString()

        val recyclerTitle = "Other posts from $name"
        postAct_text_recycler_title.text = recyclerTitle

        CoroutineScope(Main).launch {
            loadProfileImage(profileImagePath, postAct_image_profile_image, context)
        }
        loadImage(imagePath)

        if (username != null) {
            setupRecycler(profileId, postId, profileIdCurrentUser)
            setupMenu(username, profileId, currentUserName, postId)
        }

        postAct_layout_comment.setOnClickListener {
            val intent = Intent(context, CommentActivity::class.java).apply {
                putExtra(Constants.PROFILE_ID_CURRENTUSER, profileIdCurrentUser)
                putExtra(Constants.PROFILE_ID, profileId)
                putExtra(Constants.POST_ID, postId)
            }
            startActivity(intent)
        }

        postAct_image_like_bt.setOnClickListener {
            if(likeStatus) {
                likeStatus = false
                postAct_image_like_bt.setImageResource(R.drawable.ic_thumb_up_not_selected24)
                likes--
                postEdited = true
                postAct_text_likes_number.text = likes.toString()
            } else {
                likeStatus = true
                postAct_image_like_bt.setImageResource(R.drawable.ic_thumb_up_selected_24)
                likes++
                postEdited = true
                postAct_text_likes_number.text = likes.toString()
            }
            updateLikeStatus(profileIdCurrentUser)
        }
        postAct_text_likes_number.setOnClickListener {
            val intent = Intent(context, LikeActivity::class.java).apply{
                putExtra(Constants.POST_ID, postId)
                putExtra(Constants.PROFILE_ID_CURRENTUSER, profileIdCurrentUser)
            }
            startActivity(intent)
        }

        postAct_layout_profile.setOnClickListener {
            val intent = Intent(context, ProfileActivity::class.java).apply {
                putExtra(Constants.PROFILE_ID, profileId)
                putExtra(Constants.PROFILE_ID_CURRENTUSER, profileIdCurrentUser)
            }
            startActivity(intent)
        }
    }

    private fun updateLikeStatus(profileIdCurrentUser: String) {
        if (currentLike != null) {
            val likeItem = currentLike!!.copyOfBuilder()
                .like(likeStatus)
                .profileId(profileIdCurrentUser)
                .post(currentPostList[0])
                .build()

            Amplify.API.mutate(
                ModelMutation.update(likeItem),
                { response ->
                    Log.i("MyAmplifyApp", "Like status changed: " + response.data.profileId)
                },
                { error ->
                    Log.e("MyAmplifyApp", "Create failed", error)
                }
            )
        } else {
            val likeItem = Like.builder()
                .profileId(profileIdCurrentUser)
                .like(likeStatus)
                .post(currentPostList[0])
                .build()

            currentLike = likeItem

            Amplify.API.mutate(
                ModelMutation.create(likeItem),
                { response ->
                    Log.i("MyAmplifyApp", "Like status changed: " + response.data.profileId)
                },
                { error ->
                    Log.e("MyAmplifyApp", "Create failed", error)
                }
            )
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

    private fun setupMenu(
        postUsername: String,
        profileId: String,
        currentUserName: String,
        postId: String
    ) {
        if (postUsername == currentUserName) {
            postAct_image_menu_bt.setOnClickListener {
                val popupMenu = PopupMenu(this@PostActivity, postAct_image_menu_bt)
                popupMenu.menuInflater.inflate(R.menu.menu_post, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener {
                    if (it.itemId == R.id.action_edit) {
                        val intent = Intent(context, UploadActivity::class.java).apply {
                            putExtra(Constants.PROFILE_ID, profileId)
                            putExtra(Constants.POST_ID, postId)
                            putExtra(Constants.ACTIVITY_FROM, Constants.POST_ACTIVITY)
                        }
                        startActivityForResult(intent, Constants.REQUEST_EDIT)
                    }
                    if (it.itemId == R.id.action_delete) {
                        if (postLoaded) {
                            postLoaded = if (currentPostList.size == 0) {
                                Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
                                true
                            } else {
                                showDeleteDialog(currentPostList[0])
                                true
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.REQUEST_EDIT) {
            if (data?.getBooleanExtra(Constants.POST_EDITED, false) == true) {
                setupUI()
                postEdited = true
            }
        }
    }

    private fun setupRecycler(profileId: String, postId: String, profileIdCurrentUser: String) {
        val linearLayoutManager = LinearLayoutManager(context)
        postAct_rc_posts.layoutManager = linearLayoutManager
        CoroutineScope(Main).launch {
            queryPost(profileId, postId, profileIdCurrentUser)
        }
    }

    private fun showDeleteDialog(deletePost: Post) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Post")
        builder.setMessage("Do you want to delete this post?")
        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            CoroutineScope(Main).launch {
                deletePostFromAWS(deletePost)
            }
        }
        builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private suspend fun loadProfileImage(filePath: String, imageView: ImageView, context: Context) =
        withContext(Main) {
            val file = File(filePath)
            if (file.exists()) {
                CoroutineScope(Main).launch {
                    Glide.with(context)
                        .load(file)
                        .into(imageView)
                }
            }
        }

    private suspend fun queryPost(profileId: String, postId: String, profileIdCurrentUser: String) =
        withContext(IO) {
            Amplify.API.query(
                ModelQuery.get(Profile::class.java, profileId),
                { response ->
                    posts.clear()
                    if (response.data != null) {
                        for (post in response.data.posts) {
                            if (post.id != postId) {
                                posts.add(post)
                            }

                        }
                    }
                    Log.i("MyAmplifyApp", "Posts added in recyclerview for PostActivity")
                    CoroutineScope(Main).launch {
                        withContext(Default) {
                            posts.sortByDescending { it.date }
                        }
                        withContext(Main) {
                            val fivePosts = getFivePosts(posts)
                            runOnUiThread {
                                postAct_rc_posts.adapter =
                                    PostAdapter(fivePosts, context, profileIdCurrentUser)
                                postAct_rc_posts.viewTreeObserver.addOnGlobalLayoutListener {
                                    callback()
                                }
                                pageHelper(postId, posts, profileIdCurrentUser)
                            }
                        }
                    }
                },
                { error ->
                    Log.e("MyAmplifyApp", "Query failure", error)
                }
            )
        }

    private suspend fun queryPostById(
        postId: String,
        postItem: ArrayList<Post>,
        currentUserName: String,
        profileIdCurrentUser: String
    ) =
        withContext(IO) {
            Amplify.API.query(
                ModelQuery.get(Post::class.java, postId),
                { response ->
                    postItem.clear()
                    val post = response.data
                    postItem.add(post)
                    postLoaded = true
                    val profileId = post.profile.id
                    loadUI(profileId, postId, currentUserName, profileIdCurrentUser)
                },
                { error ->
                    Log.e("MyAmplifyApp", "Query failure", error)
                }
            )
        }

    private suspend fun deletePostFromAWS(postItem: Post) = withContext(IO) {
        if (postItem.comments.size != 0) {
            for (commentItem in postItem.comments) {
                Amplify.API.mutate(
                    ModelMutation.delete(commentItem), {}, {}
                )
            }
        }
        Amplify.API.mutate(
            ModelMutation.delete(postItem),
            { result ->
                Log.i("MyAmplifyApp", "postItem deleted ")
                val id = result.data.id
                val imageNumber = result.data.image
                if (result.data.hasImage) {
                    deleteImageFromS3("$id-$imageNumber.jpg")
                }
                finish()
            },
            { error -> Log.e("MyAmplifyApp", "Create failed", error) }
        )
    }

    private fun deleteImageFromS3(fileName: String) {
        Amplify.Storage.remove(
            fileName,
            { result -> Log.i("MyAmplifyApp", "Successfully removed: " + result.getKey()) },
            { error -> Log.e("MyAmplifyApp", "Remove failure", error) }
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

    private fun refreshCommentNumber() {
        if (currentPostList.size != 0) {
            Amplify.API.query(
                ModelQuery.get(Post::class.java, currentPostList[0].id),
                { response ->
                    runOnUiThread {
                        postAct_text_comments_number.text = response.data.comments.size.toString()
                        postAct_text_likes_number.text = response.data.likes.size.toString()
                    }
                },
                { error ->
                    Log.e("MyAmplifyApp", "Query failure", error)
                }
            )
        }
    }

    private fun pageHelper(id: String?, posts: ArrayList<Post>, profileIdCurrentUser: String) {
        if (postNumber - 5 <= 0) {
            postAct_image_previous_page.visibility = View.INVISIBLE
            postAct_image_previous_page.isClickable = false
        } else {
            postAct_image_previous_page.visibility = View.VISIBLE
            postAct_image_previous_page.isClickable = true
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
                        postAct_image_previous_page.setOnClickListener {
                            if (id != null) {
                                postNumber -= 10
                                val fivePosts = getFivePosts(posts)
                                postAct_rc_posts.adapter =
                                    PostAdapter(fivePosts, context, profileIdCurrentUser)
                                pageHelper(id, posts, profileIdCurrentUser)
                            }
                        }
                        return false
                    }
                })
                .into(postAct_image_previous_page)
        }
        if (postNumber >= posts.size) {
            postAct_image_next_page.isClickable = false
            postAct_image_next_page.visibility = View.INVISIBLE
        } else {
            postAct_image_next_page.isClickable = true
            postAct_image_next_page.visibility = View.VISIBLE
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
                        postAct_image_next_page.setOnClickListener {
                            if (id != null) {
                                val fivePosts = getFivePosts(posts)
                                postAct_rc_posts.adapter =
                                    PostAdapter(fivePosts, context, profileIdCurrentUser)
                                pageHelper(id, posts, profileIdCurrentUser)
                            }
                        }
                        return false
                    }
                })
                .into(postAct_image_next_page)
        }
    }

    private fun showProgressbar() = runOnUiThread {
        postAct_progressbar.visibility = View.VISIBLE
        postAct_layout_all.visibility = View.INVISIBLE
    }

    private fun hideProgressbar() = runOnUiThread {
        postAct_progressbar.visibility = View.GONE
        postAct_layout_all.visibility = View.VISIBLE
    }

    override fun callback() {
        coroutineScope.launch {
            postAct_nestedScrollView.scrollTo(0, 0)
            delay(700L)
            hideProgressbar()
            postAct_rc_posts.viewTreeObserver.removeOnGlobalLayoutListener { }
        }
    }
}
