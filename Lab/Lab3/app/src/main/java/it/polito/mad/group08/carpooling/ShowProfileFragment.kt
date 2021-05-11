package it.polito.mad.group08.carpooling

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import org.json.JSONObject
import java.io.FileNotFoundException

class ShowProfileFragment : Fragment() {

    private lateinit var photoIV: ImageView
    private lateinit var fullNameTV : TextView
    private lateinit var nicknameTV : TextView
    private lateinit var emailTV : TextView
    private lateinit var locationTV : TextView
    private lateinit var phonenumberTV : TextView
    private val model: SharedViewModel by activityViewModels()

    interface InfoManager{
        fun updateTexts(main: String, secondary: String)
        fun updatePhoto(bitmap: Bitmap)
    }


    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_show_profile, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        photoIV = view.findViewById(R.id.photoImage)
        fullNameTV = view.findViewById(R.id.fullNameTV)
        nicknameTV = view.findViewById(R.id.nicknameTV)
        emailTV = view.findViewById(R.id.emailTV)
        locationTV = view.findViewById(R.id.locationTV)
        phonenumberTV = view.findViewById(R.id.phonenumberTV)

        if(arguments?.getString("parent")=="OTHERUSER"){
            model.getOtherUser().observe(viewLifecycleOwner, Observer<User> { userDB->
                fullNameTV.text = if (userDB.name == "") "Full Name" else userDB.name
                nicknameTV.text = if (userDB.nickname == "") "Nickname" else userDB.nickname
                emailTV.text = if (userDB.email == "") "Email" else userDB.email

                locationTV.visibility = View.GONE
                phonenumberTV.visibility = View.GONE
                view.findViewById<ImageView>(R.id.locationIcon).visibility = View.GONE
                view.findViewById<ImageView>(R.id.phonenumberIcon).visibility = View.GONE
                retrieveUserImage("other")
            })
        }
        else{
            model.getUser().observe(viewLifecycleOwner, Observer<User> { userDB->
                fullNameTV.text = if (userDB.name == "") "Full Name" else userDB.name
                nicknameTV.text = if (userDB.nickname == "") "Nickname" else userDB.nickname
                emailTV.text = if (userDB.email == "") "Email" else userDB.email
                //TODO: set location & phone number visibility SHOW
                locationTV.text = if (userDB.location == "") "Location" else userDB.location
                phonenumberTV.text = if (userDB.phone_number == "") "#" else userDB.phone_number
                retrieveUserImage("self")
            })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu,inflater)
        if(arguments?.getString("parent")!="OTHERUSER")
            inflater.inflate(R.menu.edit_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.editButton -> {
                findNavController().navigate(R.id.action_showProfileFragment_to_editProfileFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun retrieveUserImage(mode: String){
        if(mode == "self") {
            if (model.getUser().value?.bitmap != null) {
                photoIV.setImageBitmap(model.getUser().value?.bitmap)
            }
        } else if(mode == "other") {
            val storage = Firebase.storage
            val storageRef = storage.reference
            val testRef = storageRef.child(model.getOtherUser().value?.email!!)
            testRef.metadata.addOnSuccessListener { metadata ->
                val size = metadata.sizeBytes
                val ONE_MEGABYTE: Long = 1024 * 1024
                testRef.getBytes(ONE_MEGABYTE).addOnSuccessListener {
                    val imageBitmap = BitmapFactory.decodeByteArray(it, 0, size.toInt())
                    if (imageBitmap != null) {
                        photoIV.setImageBitmap(imageBitmap)
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