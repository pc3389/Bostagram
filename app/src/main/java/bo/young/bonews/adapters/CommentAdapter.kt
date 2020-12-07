package bo.young.bonews.adapters

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import bo.young.bonews.R
import bo.young.bonews.activities.ProfileActivity
import bo.young.bonews.utilities.Constants
import com.amplifyframework.api.graphql.model.ModelMutation
import com.amplifyframework.core.Amplify
import com.amplifyframework.datastore.generated.model.Comment
import kotlinx.android.synthetic.main.list_item_comment.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CommentAdapter(private val items: ArrayList<Comment>, val context: Context, private val profileIdCurrentUser: String, private val profileMap: HashMap<String, String>) :
        RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.item_comment_text_name
        val contentTextView: TextView = view.item_comment_text_content
        val dateTextView: TextView = view.item_comment_text_date
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
                LayoutInflater.from(context).inflate(
                        R.layout.list_item_comment,
                        parent,
                        false
                )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            if(items[position] == null) {
                holder.nameTextView.text = "null"
            } else {
                val profileId = items[position].profileId
                val date = items[position].date
                holder.dateTextView.text = date
                val name = profileMap[profileId]
                holder.nameTextView.text = name
                val content = items[position].content
                holder.contentTextView.text = content
                holder.nameTextView.setOnClickListener {
                    val intent = Intent(context, ProfileActivity::class.java).apply {
                        putExtra(Constants.PROFILE_ID, profileId)
                        putExtra(Constants.PROFILE_ID_CURRENTUSER, profileIdCurrentUser)
                    }
                    context.startActivity(intent)
                }

                if (profileId == profileIdCurrentUser || Amplify.Auth.currentUser.username == "pc3389") {
                    holder.itemView.setOnLongClickListener {
                        showDeleteDialog(items[position])
                        true
                    }
                }
            }
            holder.itemView.setOnLongClickListener {
                showDeleteDialog(items[position])
                true
            }
        }
    }

    private fun showDeleteDialog(deleteComment: Comment) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Delete Comment")
        builder.setMessage("Do you want to delete this comment?")

        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            CoroutineScope(Dispatchers.Main).launch {
                deleteCommentFromAWS(deleteComment)
            }
        }
        builder.setNegativeButton(android.R.string.cancel) { dialoginterface, _ ->
            dialoginterface.cancel()
        }
        builder.show()
    }

    private fun deleteCommentFromAWS(deleteComment: Comment) {
        Amplify.API.mutate(
                ModelMutation.delete(deleteComment),
                { Log.i("MyAmplifyApp", "postItem deleted ") },
                { error -> Log.e("MyAmplifyApp", "Create failed", error) }
        )
    }

    override fun getItemCount(): Int {
        return items.size
    }

}