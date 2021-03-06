package it.polito.mad.group08.carpooling

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

const val RC_SIGN_IN = 0

class LoginFragment : Fragment() {

    lateinit var signin_button: SignInButton
    lateinit var mGoogleSignInClient: GoogleSignInClient
    private val model: SharedViewModel by activityViewModels()
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.token_id))
                .requestEmail()
                .build()

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        // Initialize Firebase Auth
        auth = Firebase.auth
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
        if (auth.currentUser != null) {
            Log.d("PROVA", "Not Null currentUser")
            updateUI()
        } else
            Log.d("PROVA", "Null currentUser")
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

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Toast.makeText(requireContext(), "Error in login. Try again.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun signIn() {
        val signInIntent: Intent = mGoogleSignInClient.getSignInIntent()
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }


    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        updateUI()
                    } else {
                        Toast.makeText(requireContext(), "Error in login. Try again.", Toast.LENGTH_LONG).show()
                    }
                }
    }

    private fun updateUI() {
        if(auth.currentUser != null) {
            model.setUser(
                    User(
                            name = auth.currentUser!!.displayName!!,
                            email = auth.currentUser!!.email!!
                    )
            )
            (activity as? ShowProfileFragment.InfoManager)?.updateTexts(
                    auth.currentUser!!.displayName!!,
                    auth.currentUser!!.email!!
            )
            findNavController().navigate(R.id.action_loginFragment_to_othersTripListFragment)
            val storage = Firebase.storage
            val storageRef = storage.reference
            val testRef = storageRef.child(auth.currentUser!!.email!!)
            testRef.metadata.addOnSuccessListener { metadata ->
                val size = metadata.sizeBytes
                val ONE_MEGABYTE: Long = 1024 * 1024
                testRef.getBytes(ONE_MEGABYTE).addOnSuccessListener {
                    val imageBitmap = BitmapFactory.decodeByteArray(it, 0, size.toInt())
                    if (imageBitmap != null) {
                        model.setUserBitmap(imageBitmap)
                        (activity as? ShowProfileFragment.InfoManager)?.updatePhoto(imageBitmap)
                    }
                }.addOnFailureListener {
                    // Handle any errors
                }
            }.addOnFailureListener {
                // Uh-oh, an error occurred!
            }
        }
    }
}