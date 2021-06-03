package it.polito.mad.group08.carpooling

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val TRIP_DETAILS_IS_PARENT = "TRIP_DETAILS_IS_PARENT"


class ShowProfileFragment : Fragment() {
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
    private lateinit var ratingBarAsDriver: RatingBar
    private lateinit var ratingBarAsPassenger: RatingBar
    private lateinit var shimmer: ShimmerFrameLayout

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
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_show_profile, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        shimmer = view.findViewById(R.id.shimmerProfile)

        if (arguments?.getString("parent") == TRIP_DETAILS_IS_PARENT) { // Other user
            model.getOtherUser()
                .observe(viewLifecycleOwner, { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                        }
                        is Resource.Success -> {
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

                            model.getOtherUserPhoto()
                                .observe(
                                    viewLifecycleOwner, { imgResource ->
                                        when (imgResource) {
                                            is Resource.Loading -> {
                                                shimmer.startShimmer()
                                            }
                                            is Resource.Success -> {
                                                shimmer.hideShimmer()
                                                if (imgResource.data != null)
                                                    photoIV.setImageBitmap(imgResource.data)
                                            }
                                            is Resource.Failure -> {
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
                                    ratingBarAsDriver.rating =
                                        model.calculateRating(resource.data.email, true)
                                    ratingBarAsPassenger.rating =
                                        model.calculateRating(resource.data.email, false)
                                }
                            }
                        }
                        is Resource.Failure -> {
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
                .observe(viewLifecycleOwner, { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                        }
                        is Resource.Success -> {

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
                                    viewLifecycleOwner, { resPhotoDB ->
                                        when (resPhotoDB) {
                                            is Resource.Loading -> {
                                                shimmer.startShimmer()
                                            }
                                            is Resource.Success -> {
                                                shimmer.hideShimmer()
                                                if (resPhotoDB.data != null)
                                                    photoIV.setImageBitmap(resPhotoDB.data)
                                            }
                                            is Resource.Failure -> {
                                                Toast.makeText(
                                                    context,
                                                    "Error in loading the photo",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    })
                            MainScope().launch {
                                withContext(Dispatchers.IO) {
                                    ratingBarAsDriver.rating = model.calculateRating(
                                        model.auth.currentUser!!.email!!,
                                        true
                                    )
                                    ratingBarAsPassenger.rating = model.calculateRating(
                                        model.auth.currentUser!!.email!!,
                                        false
                                    )
                                }
                            }
                        }
                        is Resource.Failure -> {
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

                initDialog(v, true)
            }
            return@OnTouchListener true
        })

        ratingBarAsPassenger.setOnTouchListener(View.OnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {

                initDialog(v, false)
            }
            return@OnTouchListener true
        })
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
        val reviewTV = dialogFilter.findViewById<TextView>(R.id.reviewsTV)
        recyclerView!!.layoutManager = LinearLayoutManager(context)
        MainScope().launch {
            val reviews = withContext(Dispatchers.IO) {
                model.getReviews(emailTV.text.toString(), isDriverRating)
            }

            if (reviews.isEmpty()) {
                if (reviewTV != null) {
                    reviewTV.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                }
            } else {
                if (reviewTV != null) {
                    reviewTV.visibility = View.GONE
                }
                recyclerView.visibility = View.VISIBLE
                recyclerView.adapter = ReviewAdapter(reviews)
            }
        }
    }
}

class ReviewAdapter(private val items: MutableList<Review>) :
    RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    class ReviewViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val ratingBar = v.findViewById<RatingBar>(R.id.rating)
        private val textReview = v.findViewById<TextView>(R.id.reviewText)
        private val nameReview = v.findViewById<TextView>(R.id.nameReview)


        fun bind(review: Review) {
            ratingBar.rating = review.rating.toFloat()
            if (review.text == "")
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