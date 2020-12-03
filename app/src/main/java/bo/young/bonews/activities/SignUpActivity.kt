package bo.young.bonews.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import bo.young.bonews.R
import bo.young.bonews.utilities.Constants
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.core.Amplify
import kotlinx.android.synthetic.main.activity_sign_up.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignUpActivity : AppCompatActivity() {
    private val context = this
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        signUpAct_text_signUp_bt.setOnClickListener {
            val username: String = signUpAct_edit_username.text.toString()
            val password: String = signUpAct_edit_password.text.toString()
            val emailAddress: String = signUpAct_edit_email.text.toString()
            val phoneNumber = signUpAct_edit_phone.text.toString()
            if (username.isEmpty() || password.isEmpty() || emailAddress.isEmpty()) {
                when {
                    username.isEmpty() -> {
                        Toast.makeText(this, "Username is required", Toast.LENGTH_SHORT).show()
                    }
                    password.isEmpty() -> {
                        Toast.makeText(this, "password is required", Toast.LENGTH_SHORT).show()
                    }
                    emailAddress.isEmpty() -> {
                        Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                CoroutineScope(IO).launch {
                    signUp(username, password, emailAddress, phoneNumber)
                }
            }
        }
    }

    private suspend fun signUp(userName: String, password: String, email: String, phoneNumber: String) {
        withContext(IO) {
            val attributes: ArrayList<AuthUserAttribute> = ArrayList()
            //"my@email.com"
            attributes.add(AuthUserAttribute(AuthUserAttributeKey.email(), email))
            //"+15551234567"
            attributes.add(AuthUserAttribute(AuthUserAttributeKey.phoneNumber(), phoneNumber))
            Amplify.Auth.signUp(
                    userName,
                    password,
                    AuthSignUpOptions.builder().userAttributes(attributes).build(),
                    { result ->
                        Log.i("MyAmplifyApp", "Result: $result")
                        finish()
                    },
                    { error ->
                        Log.e("MyAmplifyApp", "Sign up failed", error)
                        runOnUiThread {
                            Toast.makeText(context, error.recoverySuggestion, Toast.LENGTH_SHORT).show()
                        }
                    }
            )
        }
    }

}