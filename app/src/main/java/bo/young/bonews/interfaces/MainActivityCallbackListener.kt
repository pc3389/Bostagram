package bo.young.bonews.interfaces

interface MainActivityCallbackListener {
    fun callback(profileIdCurrentUser: String, profileIdForPost: String, postId: String)
}