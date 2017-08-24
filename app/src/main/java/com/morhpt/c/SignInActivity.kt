package com.morhpt.c

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import kotlinx.android.synthetic.main.activity_sign_in.*
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import java.util.*
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse


class SignInActivity : GodLikeActivity() {

    private val RC_SIGN_IN = 2341

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        sign_in_button.setOnClickListener {
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setTosUrl("https://superapp.example.com/terms-of-service.html")
                            .setPrivacyPolicyUrl("https://superapp.example.com/privacy-policy.html")
                            .setAvailableProviders(
                                    Arrays.asList(
//                                            AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
//                                            AuthUI.IdpConfig.Builder(AuthUI.PHONE_VERIFICATION_PROVIDER).build(),
                                            AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
                            .build(),
                    RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        // RC_SIGN_IN is the request code you passed into startActivityForResult(...) when starting the sign in flow.
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            // Successfully signed in
            if (resultCode == RESULT_OK) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return
            } else {
                // Sign in failed
                if (response == null) {
                    // User pressed back button
                    showSnackbar(R.string.sign_in_cancelled)
                    return
                }

                if (response.errorCode == ErrorCodes.NO_NETWORK) {
                    showSnackbar(R.string.no_internet_connection)
                    return
                }

                if (response.errorCode == ErrorCodes.UNKNOWN_ERROR) {
                    showSnackbar(R.string.unknown_error)
                    return
                }
            }

            showSnackbar(R.string.unknown_sign_in_response)
        }
    }

    private fun  showSnackbar(error: Int) {
        Snackbar.make(sign_in_main_view, error, Snackbar.LENGTH_LONG).show()
    }


    override fun onStart() {
        super.onStart()
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}
