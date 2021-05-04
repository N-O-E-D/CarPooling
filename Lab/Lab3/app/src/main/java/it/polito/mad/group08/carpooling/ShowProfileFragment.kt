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
import org.json.JSONObject
import java.io.FileNotFoundException

class ShowProfileFragment : Fragment() {

    private lateinit var photoIV: ImageView
    private lateinit var fullNameTV : TextView
    private lateinit var nicknameTV : TextView
    private lateinit var emailTV : TextView
    private lateinit var locationTV : TextView
    private lateinit var phonenumberTV : TextView
    //private lateinit var sharedPref : SharedPreferences
    //private lateinit var user: User
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

        /*sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)!!

        val jsonObjectDefault = JSONObject()
        jsonObjectDefault.put("fullName", getString(R.string.fullName))
        jsonObjectDefault.put("nickname", getString(R.string.nickname))
        jsonObjectDefault.put("email", getString(R.string.email))
        jsonObjectDefault.put("location", getString(R.string.location))
        jsonObjectDefault.put("phonenumber", "#")

        if(arguments != null){
            with (sharedPref.edit()) {
                val jsonObject = JSONObject()
                jsonObject.put("fullName", if(arguments?.getString("fullname")!="") arguments?.getString("fullname") else "Full name")
                jsonObject.put("nickname", if(arguments?.getString("nickname")!="") arguments?.getString("nickname") else "Nickname")
                jsonObject.put("email", if(arguments?.getString("email")!="") arguments?.getString("email") else "Email")
                jsonObject.put("location", if(arguments?.getString("location")!="") arguments?.getString("location") else "Location")
                jsonObject.put("phonenumber", if(arguments?.getString("phonenumber")!="") arguments?.getString("phonenumber") else "#")

                putString("profile", jsonObject.toString())
                apply()
            }
        }

        val jsonObject = sharedPref.getString("profile", jsonObjectDefault.toString())!!

        val deserializedJSON = JSONObject(jsonObject)*/

        model.getUser().observe(viewLifecycleOwner, Observer<User> { userDB->
            fullNameTV.text = if (userDB.name == "") "Full Name" else userDB.name
            nicknameTV.text = if (userDB.nickname == "") "Nickname" else userDB.nickname
            emailTV.text = if (userDB.email == "") "Email" else userDB.email
            locationTV.text = if (userDB.location == "") "Location" else userDB.location
            phonenumberTV.text = if (userDB.phone_number == "") "#" else userDB.phone_number
        })
/*
        fullNameTV.text = if (user.name == "") "Full Name" else user.name
        nicknameTV.text = if (user.nickname == "") "Nickname" else user.nickname
        emailTV.text = if (user.email == "") "Email" else user.email
        locationTV.text = if (user.location == "") "Location" else user.location
        phonenumberTV.text = if (user.phone_number == "") "#" else user.phone_number
*/
        retrieveUserImage()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu,inflater)
        inflater.inflate(R.menu.edit_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.editButton -> {
                /*val bundle = bundleOf(
                    "fullname" to fullNameTV.text.toString(),
                    "nickname" to nicknameTV.text.toString(),
                    "email" to emailTV.text.toString(),
                    "location" to locationTV.text.toString(),
                    "phonenumber" to phonenumberTV.text.toString()
                )*/
                findNavController().navigate(R.id.action_showProfileFragment_to_editProfileFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun retrieveUserImage(){
        try{
            requireActivity().applicationContext.openFileInput("userProfileImage").use{
                val bitmap: Bitmap? = BitmapFactory.decodeStream(it)
                if(bitmap != null){
                    photoIV.setImageBitmap(bitmap)
                    (activity as? InfoManager)?.updatePhoto(bitmap)
                }
            }
        }
        catch(e: FileNotFoundException){
            e.printStackTrace()
        }
    }
}