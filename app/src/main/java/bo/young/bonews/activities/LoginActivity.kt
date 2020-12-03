package bo.young.bonews.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import bo.young.bonews.R
import bo.young.bonews.utilities.Constants
import com.amplifyframework.core.Amplify
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class LoginActivity : AppCompatActivity() {

    val context = this
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        Amplify.Auth.fetchAuthSession(
            { result ->
                if (result.isSignedIn ) {
                    when {
                        Amplify.Auth.currentUser == null -> {
                            Amplify.Auth.signOut(
                                    {
                                        Log.i("MyAmplifyApp", "Signed out successfully")
                                    },
                                    { error ->
                                        runOnUiThread {
                                            Toast.makeText(
                                                    context,
                                                    error.recoverySuggestion,
                                                    Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    })
                        }
                        Amplify.Auth.currentUser.username == "guest" -> {

                            Amplify.Auth.signOut(
                                    {
                                        Log.i("MyAmplifyApp", "Signed out successfully")
                                    },
                                    { error ->
                                        runOnUiThread {
                                            Toast.makeText(
                                                    context,
                                                    error.recoverySuggestion,
                                                    Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    })
                        }
                        else -> runOnUiThread{
                            loginAct_progressbar.visibility = View.VISIBLE
                            loginAct_layout_all.visibility = View.GONE
                            val username: String = Amplify.Auth.currentUser.username
                            startMainActivity(username)
                            finish()
                        }
                    }
                }
                Log.i("MyAmplifyApp", result.toString())
                setupUI()
            },
            { error -> Log.e("MyAmplifyApp", error.toString()) }
        )


    }

    private fun setupUI() = runOnUiThread {
        loginAct_text_guest_bt.setOnClickListener {
            CoroutineScope(IO).launch { logIn("guest", "guest123") }
        }

        loginAct_text_signUp_bt.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        loginAct_text_logIn_bt.setOnClickListener {
            val username: String = loginAct_edit_username.text.toString()
            val password: String = loginAct_edit_password.text.toString()
            CoroutineScope(Main).launch {
                logIn(username, password)
            }
        }
        loginAct_edit_password.setOnEditorActionListener { _, actionId, _ ->
            var handled = false
            val username: String = loginAct_edit_username.text.toString()
            val password: String = loginAct_edit_password.text.toString()
            if (actionId === EditorInfo.IME_ACTION_DONE) {
                CoroutineScope(Main).launch {
                    logIn(username, password)
                }
                handled = true
            }
            return@setOnEditorActionListener handled
        }
    }

    private suspend fun logIn(username: String, password: String) = withContext(Main) {
        hideKeyboard()
        loginAct_progressbar.visibility = View.VISIBLE
        loginAct_layout_all.visibility = View.GONE
        withContext(IO) {
            Amplify.Auth.signIn(
                username,
                password,
                {
                    startMainActivity(username)
                    Log.i("MyAmplifyApp", "login")
                    finish()
                },
                { error ->
                    Log.e("MyAmplifyApp", error.toString())
                    val recoverySuggestion = error.recoverySuggestion
                    if (recoverySuggestion == "Please confirm user first and then retry operation") {
                        startConfirmationActivity(username)
                    }
                    runOnUiThread {
                        if(recoverySuggestion != "See attached exception for more details.") {
                            Toast.makeText(context, error.recoverySuggestion, Toast.LENGTH_SHORT).show()
                        } else if (error.cause.toString().contains("Incorrect username or password")) {
                            Toast.makeText(context, "Incorrect username or password", Toast.LENGTH_SHORT).show()
                        }

                        loginAct_progressbar.visibility = View.GONE
                        loginAct_layout_all.visibility = View.VISIBLE
                    }
                }
            )
        }

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
        return super.dispatchTouchEvent(event)}

    private fun hideKeyboard() {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }


    private fun startMainActivity(username: String) {
        val intent = Intent(this, MainActivity::class.java).apply{
            putExtra(Constants.KEY_USERNAME, username)
        }
        startActivity(intent)
    }

    private fun startConfirmationActivity(username: String) {
        val intent = Intent(this, ConfirmationActivity::class.java).apply {
            putExtra(Constants.KEY_USERNAME, username)
        }
        startActivity(intent)
    }
}