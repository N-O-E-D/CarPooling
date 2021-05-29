package it.polito.mad.group08.carpooling

import android.graphics.Bitmap
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController

const val TRIP_DETAILS_IS_PARENT = "TRIP_DETAILS_IS_PARENT"


class ShowProfileFragment : Fragment() {
    private lateinit var showProfileProgressBar: ProgressBar
    private lateinit var showProfilePhotoProgressBar: ProgressBar

    private lateinit var photoIV: ImageView
    private lateinit var fullNameIcon: ImageView
    private lateinit var fullNameTV: TextView
    private lateinit var nicknameIcon: ImageView
    private lateinit var nicknameTV: TextView
    private lateinit var emailIcon: ImageView
    private lateinit var emailTV: TextView
    private lateinit var locationIcon: ImageView
    private lateinit var locationTV: TextView
    private lateinit var phoneNumberIcon: ImageView
    private lateinit var phoneNumberTV: TextView
    private lateinit var ratingBar: RatingBar

    private val model: SharedViewModel by activityViewModels()

    interface InfoManager {
        fun updateTexts(main: String, secondary: String)
        fun updatePhoto(bitmap: Bitmap)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_show_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showProfileProgressBar = view.findViewById(R.id.showProfileProgressBar)
        showProfilePhotoProgressBar = view.findViewById(R.id.showProfilePhotoProgressBar)
        photoIV = view.findViewById(R.id.photoImage)
        fullNameIcon = view.findViewById(R.id.fullnameIcon)
        fullNameTV = view.findViewById(R.id.fullNameTV)
        nicknameIcon = view.findViewById(R.id.nicknameIcon)
        nicknameTV = view.findViewById(R.id.nicknameTV)
        emailIcon = view.findViewById(R.id.emailIcon)
        emailTV = view.findViewById(R.id.emailTV)
        locationIcon = view.findViewById(R.id.locationIcon)
        locationTV = view.findViewById(R.id.locationTV)
        phoneNumberIcon = view.findViewById(R.id.phonenumberIcon)
        phoneNumberTV = view.findViewById(R.id.phonenumberTV)
        ratingBar = view.findViewById(R.id.ratingBar)

        if (arguments?.getString("parent") == TRIP_DETAILS_IS_PARENT) { // Other user
            model.getOtherUser()
                .observe(viewLifecycleOwner, Observer<Resource<User>> { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            showAllComponents(false)
                            showProfileProgressBar.visibility = View.VISIBLE
                            showProfilePhotoProgressBar.visibility = View.VISIBLE
                        }
                        is Resource.Success -> {
                            showProfileProgressBar.visibility = View.GONE
                            showProfilePhotoProgressBar.visibility = View.VISIBLE
                            showAllComponents(true)
                            fullNameTV.text =
                                if (resource.data.name == "") "Full Name" else resource.data.name
                            nicknameTV.text =
                                if (resource.data.nickname == "") "Nickname" else resource.data.nickname
                            emailTV.text =
                                if (resource.data.email == "") "Email" else resource.data.email

                            locationIcon.visibility = View.GONE
                            locationTV.visibility = View.GONE
                            phoneNumberIcon.visibility = View.GONE
                            phoneNumberTV.visibility = View.GONE

                            //TODO Benedetto: There is a graphical 'bug' if I see User1 then User2
                            // The same below. Maybe some time for upload the photo?
                            model.getOtherUserPhoto()
                                .observe(
                                    viewLifecycleOwner,
                                    Observer<Resource<Bitmap>> { imgResource ->
                                        when (imgResource) {
                                            is Resource.Loading -> {
                                                showProfilePhotoProgressBar.visibility =
                                                    View.VISIBLE
                                            }
                                            is Resource.Success -> {
                                                showProfilePhotoProgressBar.visibility = View.GONE
                                                photoIV.setImageBitmap(imgResource.data)
                                            }
                                            is Resource.Failure -> {
                                                showProfilePhotoProgressBar.visibility = View.GONE
                                                Toast.makeText(
                                                    context,
                                                    "Error loading the photo. Try later",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    })
                        }
                        is Resource.Failure -> {
                            showAllComponents(false)
                            showProfileProgressBar.visibility = View.GONE
                            showProfilePhotoProgressBar.visibility = View.GONE
                            Toast.makeText(
                                context,
                                getString(R.string.error_occur),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                })
        } else { //Show profile or return from edit
            model.getUser()
                .observe(viewLifecycleOwner, Observer<Resource<User>> { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            showAllComponents(false)
                            showProfileProgressBar.visibility = View.VISIBLE
                        }
                        is Resource.Success -> {
                            showProfileProgressBar.visibility = View.GONE
                            showAllComponents(true)

                            fullNameTV.text =
                                if (resource.data.name == "") "Full Name" else resource.data.name
                            nicknameTV.text =
                                if (resource.data.nickname == "") "Nickname" else resource.data.nickname
                            emailTV.text =
                                if (resource.data.email == "") "Email" else resource.data.email
                            locationTV.text =
                                if (resource.data.location == "") "Location" else resource.data.location
                            phoneNumberTV.text =
                                if (resource.data.phone_number == "") "#" else resource.data.phone_number

                            model.getUserPhoto()
                                .observe(
                                    viewLifecycleOwner,
                                    Observer<Resource<Bitmap>> { resPhotoDB ->
                                        when (resPhotoDB) {
                                            is Resource.Loading -> {
                                                showProfilePhotoProgressBar.visibility =
                                                    View.VISIBLE
                                            }
                                            is Resource.Success -> {
                                                showProfilePhotoProgressBar.visibility = View.GONE
                                                photoIV.setImageBitmap(resPhotoDB.data)
                                            }
                                            is Resource.Failure -> {
                                                showProfilePhotoProgressBar.visibility = View.GONE
                                                Toast.makeText(
                                                    context,
                                                    "Error in loading the new photo",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    })
                        }
                        is Resource.Failure -> {
                            showAllComponents(false)
                            showProfileProgressBar.visibility = View.GONE
                            Toast.makeText(
                                context,
                                getString(R.string.error_occur),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                })
        }
    }

    private fun showAllComponents(showHide: Boolean) {
        fullNameIcon.visibility = if (showHide) View.VISIBLE else View.GONE
        nicknameIcon.visibility = if (showHide) View.VISIBLE else View.GONE
        emailIcon.visibility = if (showHide) View.VISIBLE else View.GONE
        locationIcon.visibility = if (showHide) View.VISIBLE else View.GONE
        phoneNumberIcon.visibility = if (showHide) View.VISIBLE else View.GONE

        fullNameTV.visibility = if (showHide) View.VISIBLE else View.GONE
        nicknameTV.visibility = if (showHide) View.VISIBLE else View.GONE
        emailTV.visibility = if (showHide) View.VISIBLE else View.GONE
        locationTV.visibility = if (showHide) View.VISIBLE else View.GONE
        phoneNumberTV.visibility = if (showHide) View.VISIBLE else View.GONE

        ratingBar.visibility = if (showHide) View.VISIBLE else View.GONE
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (arguments?.getString("parent") != TRIP_DETAILS_IS_PARENT) // != other user
            inflater.inflate(R.menu.edit_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.editButton -> {
                findNavController().navigate(R.id.action_showProfileFragment_to_editProfileFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}