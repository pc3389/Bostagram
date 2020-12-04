package bo.young.bonews.activities

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import bo.young.bonews.R
import bo.young.bonews.adapters.CommentAdapter
import bo.young.bonews.adapters.PostAdapter
import bo.young.bonews.interfaces.CallbackListener
import bo.young.bonews.utilities.Constants
import com.amplifyframework.api.graphql.model.ModelMutation
import com.amplifyframework.api.graphql.model.ModelQuery
import com.amplifyframework.core.Amplify
import com.amplifyframework.datastore.generated.model.Comment
import com.amplifyframework.datastore.generated.model.Post
import com.amplifyframework.datastore.generated.model.PostStatus
import com.amplifyframework.datastore.generated.model.Profile
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.activity_post.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class CommentActivity : AppCompatActivity(), CallbackListener {
    val context = this
    private val coroutineScope = CoroutineScope(Main)
    private var currentProfile: Profile? = null
    private var currentPost: Post? = null
    private val commentList: ArrayList<Comment> = ArrayList()
    private var hasProfile = false
    private var hasPosts = false
    private val profileMap: HashMap<String, String> = HashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)

        val profileIdCurrentUser = intent.getStringExtra(Constants.PROFILE_ID_CURRENTUSER)
        val postId = intent.getStringExtra(Constants.POST_ID)
        val linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager.reverseLayout = true
        commentAct_rc_posts.layoutManager = linearLayoutManager

        showProgressbar()

        coroutineScope.launch {
            if (profileIdCurrentUser != null && postId != null) {
                queryProfile(profileIdCurrentUser, postId)
            }

        }

        commentAct_image_send.setOnClickListener {
            coroutineScope.launch {
                if (profileIdCurrentUser != null) {
                    val content = commentAct_edit_comment.text.toString()
                    val date = getTodayDate()
                    if (content == "") {
                        Toast.makeText(context, "Comment cannot be empty", Toast.LENGTH_SHORT).show()
                    } else {
                        uploadComment(profileIdCurrentUser, date, content, currentPost!!)
                        commentAct_rc_posts.scrollToPosition(0)
                        hideKeyboard()
                    }
                }
            }
        }

        commentAct_image_back_bt.setOnClickListener {
            onBackPressed()
        }

    }

    private suspend fun queryProfile(profileId: String, postId: String) = withContext(IO) {
        Amplify.API.query(
                ModelQuery.get(Profile::class.java, profileId),
                { response ->
                    if (response.data != null) {
                        val profile = response.data
                        currentProfile = profile
                        if (response.data.username == "guest") {
                            hideComment()
                        }
                        hasProfile = true
                        if (hasPosts) {
                            hideProgressbar()
                        }
                        CoroutineScope(Main).launch { queryPost(postId) }

                        Log.i("MyAmplifyApp", profile.username)
                    }
                },
                { error -> Log.e("MyAmplifyApp", "Query failure", error) }
        )
    }

    private suspend fun queryPost(postId: String) = withContext(IO) {
        Amplify.API.query(
                ModelQuery.get(Post::class.java, postId),
                { response ->
                    val post = response.data
                    currentPost = post
                    Log.i("MyAmplifyApp", post.title)
                    if (currentPost != null) {
                        for (commentItem in currentPost!!.comments) {
                            commentList.add(commentItem)
                            profileMap[commentItem.profileId] = commentItem.name
                        }
                        commentList.sortByDescending { it.date }
                        var count = 0
                        val maxCount = profileMap.keys.size
                        for (profileId in profileMap.keys) {
                            Amplify.API.query(
                                    ModelQuery.get(Profile::class.java, profileId),
                                    { profileResponse ->
                                        val profileItem = profileResponse.data
                                        if (profileMap[profileItem.id] != profileItem.nickname) {
                                            profileMap[profileItem.id] = profileItem.nickname
                                        }
                                        count++
                                        if (count == maxCount) {
                                            callback()
                                        }
                                    },
                                    { error ->
                                        Log.e("MyAmplifyApp", "Query failure", error)
                                        count++
                                        if (count == maxCount) {
                                            callback()
                                        }
                                    })
                        }
                    }
                    hasPosts = true
                    if (hasProfile) {
                        hideProgressbar()
                    }

                },
                { error -> Log.e("MyAmplifyApp", "Query failure", error) }
        )
    }

    private suspend fun uploadComment(profileId: String, date: String, content: String, post: Post) =
            withContext(IO) {

                val comment = Comment.builder()
                        .profileId(profileId)
                        .date(date)
                        .name(currentProfile?.nickname)
                        .content(content)
                        .post(post)
                        .build()


                Amplify.API.mutate(
                        ModelMutation.create(comment),
                        { response ->
                            Log.i("MyAmplifyApp", "Todo with id: " + response.data.id)
                            commentList.add(0, response.data)
                            profileMap[response.data.profileId] = currentProfile!!.nickname
                            callback()
                        },
                        { error -> Log.e("MyAmplifyApp", "Create failed", error) }
                )
            }

    private fun hideComment() = runOnUiThread {
        commentAct_layout_comment.visibility = View.GONE
    }

    private fun showProgressbar() = runOnUiThread {
        commentAct_progressbar.visibility = View.VISIBLE
        commentAct_layout_all.visibility = View.INVISIBLE
    }

    private fun hideProgressbar() = runOnUiThread {
        commentAct_progressbar.visibility = View.GONE
        commentAct_layout_all.visibility = View.VISIBLE
    }

    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy. MM. dd. HH:mm:ss")
        return sdf.format(Date())
    }

    override fun onBackPressed() {
        finish()
        super.onBackPressed()
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

    override fun callback() = runOnUiThread {
        commentAct_rc_posts.adapter = CommentAdapter(commentList, context, currentProfile!!.id, profileMap)
        commentAct_rc_posts.scrollToPosition(0)
    }
}