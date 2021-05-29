package it.polito.mad.group08.carpooling

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
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
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ShowProfileFragment : Fragment() {

    private lateinit var photoIV: ImageView
    private lateinit var fullNameTV : TextView
    private lateinit var nicknameTV : TextView
    private lateinit var emailTV : TextView
    private lateinit var locationTV : TextView
    private lateinit var phonenumberTV : TextView
    private lateinit var ratingBarAsDriver : RatingBar
    private lateinit var ratingBarAsPassenger : RatingBar
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


    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        photoIV = view.findViewById(R.id.photoImage)
        fullNameTV = view.findViewById(R.id.fullNameTV)
        nicknameTV = view.findViewById(R.id.nicknameTV)
        emailTV = view.findViewById(R.id.emailTV)
        locationTV = view.findViewById(R.id.locationTV)
        phonenumberTV = view.findViewById(R.id.phonenumberTV)
        ratingBarAsDriver = view.findViewById(R.id.ratingAsDriver)
        ratingBarAsPassenger = view.findViewById(R.id.ratingAsPassenger)

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
                MainScope().launch {
                    withContext(Dispatchers.IO) {
                        ratingBarAsDriver.rating = model.calculateRating(userDB.email, true)
                        ratingBarAsPassenger.rating = model.calculateRating(userDB.email, false)
                    }
                }
            })
        }
        else{
            model.getUser().observe(viewLifecycleOwner, Observer<User> { userDB->
                fullNameTV.text = if (userDB.name == "") "Full Name" else userDB.name
                nicknameTV.text = if (userDB.nickname == "") "Nickname" else userDB.nickname
                emailTV.text = if (userDB.email == "") "Email" else userDB.email
                locationTV.text = if (userDB.location == "") "Location" else userDB.location
                phonenumberTV.text = if (userDB.phone_number == "") "#" else userDB.phone_number
                retrieveUserImage("self")
                MainScope().launch {
                    withContext(Dispatchers.IO) {
                        ratingBarAsDriver.rating = model.calculateRating(model.auth.currentUser!!.email!!, true)
                        ratingBarAsPassenger.rating = model.calculateRating(model.auth.currentUser!!.email!!, false)
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