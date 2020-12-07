package bo.young.bonews.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import bo.young.bonews.R
import bo.young.bonews.activities.ProfileActivity
import bo.young.bonews.utilities.Constants
import com.amplifyframework.datastore.generated.model.Like
import kotlinx.android.synthetic.main.list_item_like.view.*


class LikeAdapter(
    private val items: ArrayList<Like>,
    val context: Context,
    private val profileMap: HashMap<String, String>,
    private val profileIdCurrentUser: String
) : RecyclerView.Adapter<LikeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.item_like_text_name
        val profileImage: ImageView = view.item_like_image_profile_image
        val profileLayout: ConstraintLayout = view.item_like_layout_profile
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.list_item_like,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val profileId = items[position].profileId
        val name = profileMap[profileId]
        holder.nameTextView.text = name
        holder.profileLayout.setOnClickListener {
            val intent = Intent(context, ProfileActivity::class.java).apply {
                putExtra(Constants.PROFILE_ID, profileId)
                putExtra(Constants.PROFILE_ID_CURRENTUSER, profileIdCurrentUser)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

}