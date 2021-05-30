package it.polito.mad.group08.carpooling

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private lateinit var ratingBarAsDriver : RatingBar
    private lateinit var ratingBarAsPassenger : RatingBar

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

    @SuppressLint("ClickableViewAccessibility")
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
        ratingBarAsDriver = view.findViewById(R.id.ratingAsDriver)
        ratingBarAsPassenger = view.findViewById(R.id.ratingAsPassenger)

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
                            MainScope().launch {
                                withContext(Dispatchers.IO) {
                                    ratingBarAsDriver.rating = model.calculateRating(resource.data.email, true)
                                    ratingBarAsPassenger.rating = model.calculateRating(resource.data.email, false)
                                }
                            }
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
                            MainScope().launch {
                                withContext(Dispatchers.IO) {
                                    ratingBarAsDriver.rating = model.calculateRating(model.auth.currentUser!!.email!!, true)
                                    ratingBarAsPassenger.rating = model.calculateRating(model.auth.currentUser!!.email!!, false)
                                }
                            }
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

        ratingBarAsDriver.setOnTouchListener(View.OnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                Log.d("ABABABA", "Click su driver rating bar")
                initDialog(v, true)
            }
            return@OnTouchListener true
        })

        ratingBarAsPassenger.setOnTouchListener(View.OnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                Log.d("ABABABA", "Click su driver rating bar")
                initDialog(v, false)
            }
            return@OnTouchListener true
        })
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

        ratingBarAsPassenger.visibility = if (showHide) View.VISIBLE else View.GONE
        ratingBarAsDriver.visibility = if (showHide) View.VISIBLE else View.GONE
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

    private fun initDialog(v: View, isDriverRating: Boolean) {
        val inflater = v.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_reviews, null)
        val dialogFilter = MaterialAlertDialogBuilder(v.context)
            .setView(dialogView)
            .show()
        val recyclerView = dialogFilter.findViewById<RecyclerView>(R.id.recycleReviews)
        recyclerView!!.layoutManager = LinearLayoutManager(context)
        MainScope().launch {
            val reviews = withContext(Dispatchers.IO) {
                model.getReviews(emailTV.text.toString(), isDriverRating)
            }
            recyclerView.adapter = ReviewAdapter(reviews)
        }
    }
}

class ReviewAdapter(private val items: MutableList<Review>) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    class ReviewViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val ratingBar = v.findViewById<RatingBar>(R.id.rating)
        private val textReview = v.findViewById<TextView>(R.id.reviewText)
        private val nameReview = v.findViewById<TextView>(R.id.nameReview)


        fun bind(review: Review) {
            ratingBar.rating = review.rating.toFloat()
            if(review.text == "")
                textReview.visibility = View.GONE
            else
                textReview.text = review.text
            nameReview.text = review.from
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val layout = layoutInflater.inflate(R.layout.review_item, parent, false)

        return ReviewViewHolder(layout)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}