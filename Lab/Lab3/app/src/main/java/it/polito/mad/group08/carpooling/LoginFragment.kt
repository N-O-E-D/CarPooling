package it.polito.mad.group08.carpooling

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task


class LoginFragment : Fragment() {

    lateinit var signin_button: SignInButton
    lateinit var mGoogleSignInClient: GoogleSignInClient
    var account: GoogleSignInAccount? = null
    private val model: SharedViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onStart() {
        super.onStart()
        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        Log.d("BBBB", "on start")
        account = GoogleSignIn.getLastSignedInAccount(requireActivity())
        /*if (account != null) {
            model.setAccount(account)
            model.setUser(User(name = account.displayName.toString(), email = account.email.toString()))
            (activity as? ShowProfileFragment.InfoManager)?.
            updateTexts(account.displayName.toString(),account.email.toString())
            findNavController().navigate(R.id.action_loginFragment_to_tripListFragment)
        }*/
        //updateUI(account)
    }

    override fun onResume() {
        super.onResume()
        if(account != null) {
            model.setAccount(account!!)
            model.setUser(User(name = account!!.displayName.toString(), email = account!!.email.toString()))
            (activity as? ShowProfileFragment.InfoManager)?.
            updateTexts(account!!.displayName.toString(), account!!.email.toString())
            Log.d("BBBB", "on resume")
            findNavController().navigate(R.id.action_loginFragment_to_othersTripListFragment)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        signin_button = view.findViewById(R.id.sign_in_button)

        signin_button.setOnClickListener{
            signIn()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == 0) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            val task =
                GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun signIn() {
        val signInIntent: Intent = mGoogleSignInClient.getSignInIntent()
        startActivityForResult(signInIntent, 0)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            account =
                completedTask.getResult(ApiException::class.java)
            // Signed in successfully, show authenticated UI.
            //updateUI(account)
            /*model.setAccount(account!!)
            model.setUser(User(name = account.displayName.toString(), email = account.email.toString()))
            (activity as? ShowProfileFragment.InfoManager)?.
            updateTexts(account.displayName.toString(),account.email.toString())
            findNavController().navigate(R.id.action_loginFragment_to_tripListFragment)*/
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("TAG", "signInResult:failed code=" + e.statusCode)
            //updateUI(null)
        }
    }
}