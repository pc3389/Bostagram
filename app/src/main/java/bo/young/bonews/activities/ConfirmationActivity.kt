package bo.young.bonews.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import bo.young.bonews.R
import bo.young.bonews.utilities.Constants
import com.amplifyframework.core.Amplify
import kotlinx.android.synthetic.main.activity_confirmation.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConfirmationActivity : AppCompatActivity() {
    private val context = this
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmation)
        val username: String? = intent.getStringExtra(Constants.KEY_USERNAME)
        confirmAct_text_username.text = username

        confirmAct_text_confirm_bt.setOnClickListener {
            if (username != null) {
                val confirmationCode = confirmAct_edit_Confirmation.text.toString()
                CoroutineScope(IO).launch {
                    confirmSignUp(username, confirmationCode)
                }
            }
        }

        confirmAct_text_resend_bt.setOnClickListener {
            if (username != null) {
                CoroutineScope(IO).launch {
                    resendConfirm(username)
                }
            }
        }
    }

    private suspend fun confirmSignUp(userName: String, confirmationCode: String) {
        withContext(IO) {
            Amplify.Auth.confirmSignUp(
                userName,
                confirmationCode,
                { result ->
                    if (result.isSignUpComplete) {
                        startLogInActivity()
                    }
                },
                { error ->
                    runOnUiThread {
                        Toast.makeText(context, error.recoverySuggestion, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    private suspend fun resendConfirm(userName: String) {
        withContext(IO) {
            Amplify.Auth.resendSignUpCode(
                userName,
                { result ->
                    runOnUiThread {
                        Toast.makeText(
                            context,
                            "Confirmation resent to ${result.nextStep.codeDeliveryDetails?.destination}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                { error ->
                    runOnUiThread {
                        Toast.makeText(context, error.recoverySuggestion, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    private fun startLogInActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}