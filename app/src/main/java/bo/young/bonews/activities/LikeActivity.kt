package bo.young.bonews.activities

import android.app.Activity
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import bo.young.bonews.R
import bo.young.bonews.adapters.LikeAdapter
import bo.young.bonews.interfaces.CallbackListener
import bo.young.bonews.utilities.Constants
import com.amplifyframework.api.graphql.model.ModelQuery
import com.amplifyframework.core.Amplify
import com.amplifyframework.datastore.generated.model.Like
import com.amplifyframework.datastore.generated.model.Post
import com.amplifyframework.datastore.generated.model.Profile
import kotlinx.android.synthetic.main.activity_like.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

class LikeActivity : AppCompatActivity(), CallbackListener {
    val context = this
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var currentPost: Post? = null
    private val likeList: ArrayList<Like> = ArrayList()
    private val profileMap: HashMap<String, String> = HashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_like)

        val profileIdCurrentUser = intent.getStringExtra(Constants.PROFILE_ID_CURRENTUSER)
        val postId = intent.getStringExtra(Constants.POST_ID)
        val linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager.reverseLayout = true
        likeAct_rc_posts.layoutManager = linearLayoutManager

        showProgressbar()

        coroutineScope.launch {
            if (profileIdCurrentUser != null && postId != null) {
                queryPost(postId)
            }

        }

        likeAct_image_back_bt.setOnClickListener {
            onBackPressed()
        }

    }

    private suspend fun queryPost(postId: String) = withContext(Dispatchers.IO) {
        Amplify.API.query(
            ModelQuery.get(Post::class.java, postId),
            { response ->
                val post = response.data
                currentPost = post
                Log.i("MyAmplifyApp", post.title)
                if (currentPost != null) {
                    for (likeItem in currentPost!!.likes) {
                        likeList.add(likeItem)
                    }
                    likeList.sortByDescending { it.date }
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
                hideProgressbar()

            },
            { error -> Log.e("MyAmplifyApp", "Query failure", error) }
        )
    }

    private fun showProgressbar() = runOnUiThread {
        likeAct_progressbar.visibility = View.VISIBLE
        likeAct_layout_all.visibility = View.INVISIBLE
    }

    private fun hideProgressbar() = runOnUiThread {
        likeAct_progressbar.visibility = View.GONE
        likeAct_layout_all.visibility = View.VISIBLE
    }

    override fun onBackPressed() {
        finish()
        super.onBackPressed()
    }

    override fun callback() = runOnUiThread {
        likeAct_rc_posts.adapter = LikeAdapter(likeList, context, profileMap)
        likeAct_rc_posts.scrollToPosition(0)
    }

}