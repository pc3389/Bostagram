package bo.young.bonews.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import bo.young.bonews.R
import com.amplifyframework.datastore.generated.model.PostPermission
import kotlinx.android.synthetic.main.userid_list_item.view.*

class PermissionAdapter(val items: ArrayList<PostPermission>, val context: Context) :
    RecyclerView.Adapter<PermissionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val usernameTextView: TextView = view.item_permission_text_username
        val isPostableTextView : TextView = view.item_permission_text_isPostable
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.list_item_username,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.usernameTextView.text = items[position].username
        holder.isPostableTextView.text = items[position].permission.toString()
    }

    override fun getItemCount(): Int {
        return items.size
    }

}