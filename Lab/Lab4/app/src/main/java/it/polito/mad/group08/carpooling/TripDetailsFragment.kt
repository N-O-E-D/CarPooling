package it.polito.mad.group08.carpooling

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.OverlayItem
import java.text.SimpleDateFormat
import java.util.*


const val MY_TRIP_LIST_IS_PARENT = "TRIPS"
const val OTHERS_TRIP_LIST_IS_PARENT = "OTHERS_TRIP"
const val BOOKED_TRIPS_IS_PARENT = "BOOKED_TRIPS"
const val INTERESTED_TRIPS_IS_PARENT = "INTERESTED_TRIPS"

const val REQUEST_PERMISSIONS_REQUEST_CODE = 1

class TripDetailsFragment : Fragment() {

    private lateinit var scrollView: ScrollView
    private lateinit var carPhotoPath: ImageView
    private lateinit var carDescription: TextView
    private lateinit var driverName: TextView
    private lateinit var driverRate: RatingBar
    private lateinit var intermediateTripsRecyclerView: RecyclerView
    private lateinit var intermediateTripsShowHideButton: Button

    private lateinit var interestedUsersRecyclerView: RecyclerView
    private lateinit var interestedUsersShowHideButton: Button

    private lateinit var estimatedDuration: TextView
    private lateinit var availableSeats: TextView
    private lateinit var seatPrice: TextView

    private lateinit var description: TextView

    private lateinit var showInterestFab: FloatingActionButton
    private lateinit var reviewFab: FloatingActionButton
    private lateinit var reviewTV: TextView
    private lateinit var bgFab: View
    private lateinit var accountButton: ImageButton

    private var currentTrip: Trip? = null

    private val model: SharedViewModel by activityViewModels()

    private lateinit var map: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
    }

    private fun takeSavedPhoto(bitmap: Bitmap?) {
        if (bitmap != null) {
            carPhotoPath.setImageBitmap(bitmap)
        }
    }

    private fun openFabMenu() {
        bgFab.visibility = View.VISIBLE
        reviewFab.visibility = View.VISIBLE
        reviewTV.visibility = View.VISIBLE
        showInterestFab.animate().rotationBy(360f)
        reviewFab.animate().translationY(-resources.getDimension(R.dimen.transitionY))
        reviewTV.animate().translationY(-resources.getDimension(R.dimen.transitionY))
    }

    private fun closeFabMenu() {
        bgFab.visibility = View.GONE
        showInterestFab.animate().rotation(0f)
        reviewTV.animate().translationY(0f)
        reviewFab.animate().translationY(0f)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animator: Animator) {}
                override fun onAnimationEnd(animator: Animator) {
                    if (View.GONE == bgFab.visibility) {
                        reviewFab.visibility = View.GONE
                        reviewTV.visibility = View.GONE
                    }
                }

                override fun onAnimationCancel(animator: Animator) {}
                override fun onAnimationRepeat(animator: Animator) {}
            })
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val permissionsToRequest = ArrayList<String>()
        var i = 0
        while (i < grantResults.size) {
            permissionsToRequest.add(permissions[i])
            i++
        }
        if (permissionsToRequest.size > 0) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                permissionsToRequest.toTypedArray(),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("ShowToast")
    private fun setTripInformation(trip: Trip) {
        takeSavedPhoto(model.bitmaps[trip.id])
        carDescription.text = trip.carDescription

        driverName.text = trip.driverName
        driverRate.rating = trip.driverRate

        //TRIP RECYCLER VIEW
        intermediateTripsRecyclerView.layoutManager = LinearLayoutManager(context)
        val departureCheckpoint = trip.checkPoints.first()
        val arrivalCheckpoint = trip.checkPoints.last()

        val departureItem =
            DepartureItem(departureCheckpoint.location, departureCheckpoint.timestamp)
        val arrivalItem = ArrivalItem(arrivalCheckpoint.location, arrivalCheckpoint.timestamp)

        val startEndCheckpoints = listOf(departureItem, arrivalItem)
        val allCheckpoints: MutableList<Item> = mutableListOf()
        if (trip.checkPoints.size > 2) {
            intermediateTripsShowHideButton.text = getString(R.string.show_intermediate_stops)
            trip.checkPoints.forEachIndexed { index, checkPoint ->
                when (index) {
                    0 -> {
                        allCheckpoints.add(
                            DepartureItem(checkPoint.location, checkPoint.timestamp)
                        )
                    }
                    trip.checkPoints.lastIndex -> {
                        allCheckpoints.add(
                            ArrivalItem(checkPoint.location, checkPoint.timestamp)
                        )
                    }
                    else -> {
                        allCheckpoints.add(
                            IntermediateItem(checkPoint.location, checkPoint.timestamp)
                        )
                    }
                }
            }
        } else {
            intermediateTripsShowHideButton.visibility = View.GONE
        }

        intermediateTripsRecyclerView.adapter = ItemAdapter(startEndCheckpoints)

        //UPDATE BUTTON STATUS TRIP RECYCLER VIEW
        var i = 0
        intermediateTripsShowHideButton.setOnClickListener {
            if (i % 2 == 0) {
                intermediateTripsShowHideButton.text = getString(R.string.hide_intermediate_stops)
                intermediateTripsRecyclerView.adapter = ItemAdapter(allCheckpoints)
            } else {
                intermediateTripsShowHideButton.text = getString(R.string.show_intermediate_stops)
                intermediateTripsRecyclerView.adapter = ItemAdapter(startEndCheckpoints)
            }
            i++
        }

        estimatedDuration.text = getString(
            R.string.estimated_duration_msg,
            calcDuration(trip.checkPoints.first(), trip.checkPoints.last())
        )
        availableSeats.text = getString(R.string.available_seats_msg, trip.availableSeats)
        seatPrice.text = getString(R.string.seat_price_msg, trip.seatPrice.toString())
        if (trip.description == "")
            description.visibility = View.GONE
        else
            description.text = trip.description


        // FAB (FOR USER != OWNER)
        if (trip.driverEmail != model.auth.currentUser!!.email) {
            //val scrollView = requireView().findViewById<ScrollView>(R.id.scrollView)
            scrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                if (scrollY > oldScrollY && showInterestFab.visibility == View.VISIBLE) {
                    showInterestFab.hide()
                } else if (scrollY < oldScrollY && showInterestFab.visibility != View.VISIBLE) {
                    showInterestFab.show()
                }
            }

            if (model.userIsAccepted(trip)) { //user already show favorite and owner accepted
                showInterestFab.setImageResource(R.drawable.check)
                showInterestFab.setOnClickListener {
                    MainScope().launch {
                        if (!withContext(Dispatchers.IO) {
                                model.reviewAlreadySent(
                                    model.auth.currentUser!!.email!!,
                                    currentTrip!!.driverEmail, currentTrip!!.id
                                )
                            }
                        ) {
                            if (reviewFab.visibility == View.GONE) {
                                openFabMenu()
                            } else {
                                closeFabMenu()
                            }
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Review Already Sent",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } else {
                if (model.userIsInterested(trip)) {
                    showInterestFab.setImageResource(R.drawable.ic_baseline_clear_24)
                } else {
                    showInterestFab.setImageResource(R.drawable.ic_baseline_favorite_24)
                }

                showInterestFab.setOnClickListener {
                    val anim: Animation =
                        AnimationUtils.loadAnimation(showInterestFab.context, R.anim.shake)
                    anim.duration = 150
                    showInterestFab.startAnimation(anim)

                    if (model.userIsInterested(trip)) { // Already interested, but she would to cancel
                        model.updateTripInterestedUser(trip, false, null)
                        showInterestFab.setImageResource(R.drawable.ic_baseline_favorite_24)
                    } else {
                        model.updateTripInterestedUser(trip, true, null)
                        showInterestFab.setImageResource(R.drawable.ic_baseline_clear_24)
                    }
                }
            }
        }

        //INTERESTED USERS RECYCLER VIEW (FOR THE OWNER)
        interestedUsersRecyclerView.layoutManager = LinearLayoutManager(context)
        interestedUsersRecyclerView.visibility = View.GONE

        if (trip.interestedUsers.isNotEmpty()) {
            interestedUsersShowHideButton.text = getString(R.string.show_interested_users)
            interestedUsersShowHideButton.visibility = View.VISIBLE
            interestedUsersRecyclerView.adapter = InterestedUserAdapter(
                trip.interestedUsers,
                model,
                trip,
                findNavController()
            )
        } else {
            interestedUsersShowHideButton.visibility = View.GONE
        }

        //UPDATE BUTTON STATUS INTERESTED USERS (FOR OWNER)
        var j = 0
        interestedUsersShowHideButton.setOnClickListener {
            if (j % 2 == 0) {
                interestedUsersShowHideButton.text = getString(R.string.hide_interested_users)
                interestedUsersRecyclerView.visibility = View.VISIBLE
            } else {
                interestedUsersShowHideButton.text = getString(R.string.show_interested_users)
                interestedUsersRecyclerView.visibility = View.GONE
            }
            j++
        }

        val geoPoints = trip.geoPoints

        val items = ArrayList<OverlayItem>()
        GeoMap.drawPath(
            map,
            geoPoints.map { elem -> GeoPoint(elem.latitude, elem.longitude) }.toMutableList(),
            context,
            items
        )
    }

    private fun calcDuration(dep: CheckPoint, arr: CheckPoint): String {
        val depTs = dep.timestamp
        val arrTs = arr.timestamp
        val format = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US)
        val dateDep = format.parse(depTs)!!
        val dateArr = format.parse(arrTs)!!

        val diff: Long = dateArr.time - dateDep.time
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        return concatenate(days.toInt(), hours.toInt(), minutes.toInt())
    }

    private fun concatenate(days: Int, hours: Int, minutes: Int): String {
        var finalString = ""
        if (days != 0) {
            finalString = "$finalString $days g"
        }
        if (hours != 0) {
            val newHours = hours - days * 24
            if (newHours != 0)
                finalString = "$finalString $newHours h"
        }
        if (minutes != 0) {
            val newMinutes = minutes - hours * 60
            if (newMinutes != 0)
                finalString = "$finalString $newMinutes m"
        }

        return finalString
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val v = inflater.inflate(R.layout.fragment_trip_details, container, false)

        setHasOptionsMenu(true)
        return v
    }

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // FIND VIEW

        scrollView = view.findViewById(R.id.scrollView)
        carPhotoPath = view.findViewById(R.id.carPhoto)
        carDescription = view.findViewById(R.id.carName)
        driverName = view.findViewById(R.id.driverName)
        driverRate = view.findViewById(R.id.driverRate)
        intermediateTripsRecyclerView = view.findViewById(R.id.tripRecyclerView)
        estimatedDuration = view.findViewById(R.id.estimatedDuration)
        availableSeats = view.findViewById(R.id.availableSeats)
        seatPrice = view.findViewById(R.id.seatPrice)
        description = view.findViewById(R.id.tripDescription)

        intermediateTripsShowHideButton = view.findViewById(R.id.showHideIntermediateSteps)

        showInterestFab = view.findViewById(R.id.show_interest_fab)
        reviewFab = view.findViewById(R.id.write_review_fab)
        bgFab = view.findViewById(R.id.fabBGLayout)
        reviewTV = view.findViewById(R.id.review_tv)
        accountButton = view.findViewById(R.id.account_button)
        bgFab.setOnClickListener {
            closeFabMenu()
        }

        interestedUsersRecyclerView = view.findViewById(R.id.interestedUserRecyclerView)
        interestedUsersShowHideButton = view.findViewById(R.id.showHideInterestedUsers)

        map = view.findViewById(R.id.mapDetails)
        GeoMap.customizeMap(map, requireView(), context)

        map.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (currentTrip != null) {

                        model.setGeoPoints(currentTrip?.geoPoints!!)
                        findNavController().navigate(R.id.action_tripDetailsFragment_to_mapFragment)
                    }
                }
            }
            map.onTouchEvent(event)
        }

        // INITIALIZE DATA
        when (arguments?.getString("parent")) {
            MY_TRIP_LIST_IS_PARENT -> {
                //NOTE: please notice the nested call. You can access parentPosition only when it's returned
                model.getPosition().observe(viewLifecycleOwner, { parentPosition ->
                    model.getMyTrips()
                        .observe(viewLifecycleOwner, { resource ->
                            // POPULATE VIEW WITH DATA
                            when (resource) {
                                is Resource.Loading -> {
                                }
                                is Resource.Success -> {
                                    currentTrip = resource.data[parentPosition]
                                    setTripInformation(currentTrip!!)
                                    accountButton.visibility = View.GONE
                                    MainScope().launch {
                                        driverRate.rating = withContext(Dispatchers.IO) {
                                            model.calculateRating(
                                                model.auth.currentUser!!.email!!,
                                                true
                                            )
                                        }
                                    }
                                    showInterestFab.hide()
                                }
                                is Resource.Failure -> {

                                    Toast.makeText(
                                        context,
                                        getString(R.string.error_occur),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        })
                })
            }
            OTHERS_TRIP_LIST_IS_PARENT -> {
                //NOTE: please notice the nested call. You can access parentPosition only when it's returned
                model.getPosition().observe(viewLifecycleOwner, { parentPosition ->
                    model.getOthersTrips()
                        .observe(viewLifecycleOwner, { resource ->
                            // POPULATE VIEW WITH DATA
                            when (resource) {
                                is Resource.Loading -> {
                                    //
                                }
                                is Resource.Success -> {
                                    if ((currentTrip != null) && (resource.data[parentPosition].id != currentTrip!!.id))
                                        activity?.onBackPressed()

                                    currentTrip = resource.data[parentPosition]
                                    setTripInformation(resource.data[parentPosition])
                                    MainScope().launch {
                                        driverRate.rating = withContext(Dispatchers.IO) {
                                            model.calculateRating(currentTrip!!.driverEmail, true)
                                        }
                                    }

                                    if (resource.data[parentPosition].availableSeats > 0 ||
                                        model.userIsAccepted(resource.data[parentPosition])
                                    )
                                        showInterestFab.show()
                                    else
                                        showInterestFab.hide()
                                    interestedUsersRecyclerView.visibility = View.GONE
                                    interestedUsersShowHideButton.visibility = View.GONE
                                }
                                is Resource.Failure -> {

                                    Toast.makeText(
                                        context,
                                        getString(R.string.error_occur),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        })
                })
            }
            BOOKED_TRIPS_IS_PARENT -> {
                model.getPosition().observe(viewLifecycleOwner, { parentPosition ->
                    model.getMyBookedTrips()
                        .observe(viewLifecycleOwner, { resource ->
                            // update UI
                            when (resource) {
                                is Resource.Loading -> {
                                    //
                                }
                                is Resource.Success -> {
                                    if (parentPosition >= resource.data.size || ((currentTrip != null) && (resource.data[parentPosition].id != currentTrip!!.id))) {
                                        activity?.onBackPressed()
                                    } else {

                                        currentTrip = resource.data[parentPosition]
                                        setTripInformation(resource.data[parentPosition])
                                        MainScope().launch {
                                            driverRate.rating = withContext(Dispatchers.IO) {
                                                model.calculateRating(
                                                    currentTrip!!.driverEmail,
                                                    true
                                                )
                                            }
                                        }
                                        showInterestFab.show() //FAB with check since she already booked it
                                        interestedUsersRecyclerView.visibility =
                                            View.GONE // Owner Only
                                        interestedUsersShowHideButton.visibility = View.GONE
                                    }
                                }
                                is Resource.Failure -> {

                                    Toast.makeText(
                                        context,
                                        getString(R.string.error_occur),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        })
                })
            }
            INTERESTED_TRIPS_IS_PARENT -> {
                model.getPosition().observe(viewLifecycleOwner, { parentPosition ->
                    model.getMyInterestedTrips()
                        .observe(viewLifecycleOwner, { resource ->
                            // update UI
                            when (resource) {
                                is Resource.Loading -> {
                                    //
                                }
                                is Resource.Success -> {


                                    if (parentPosition >= resource.data.size || ((currentTrip != null) && (resource.data[parentPosition].id != currentTrip!!.id))) {
                                        activity?.onBackPressed()
                                    } else {

                                        currentTrip = resource.data[parentPosition]
                                        setTripInformation(resource.data[parentPosition])
                                        MainScope().launch {
                                            driverRate.rating = withContext(Dispatchers.IO) {
                                                model.calculateRating(
                                                    currentTrip!!.driverEmail,
                                                    true
                                                )
                                            }
                                        }
                                        showInterestFab.show() //FAB with check since she already booked it
                                        interestedUsersRecyclerView.visibility =
                                            View.GONE // Owner Only
                                        interestedUsersShowHideButton.visibility = View.GONE
                                    }
                                }
                                is Resource.Failure -> {

                                    Toast.makeText(
                                        context,
                                        getString(R.string.error_occur),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        })
                })
            }
            else -> {
                Toast.makeText(context, getString(R.string.error_occur), Toast.LENGTH_SHORT).show()
            }
        }

        reviewFab.setOnClickListener {
            closeFabMenu()
            val inflater =
                it.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val dialogView = inflater.inflate(R.layout.dialog_review, null)
            val dialogFilter = MaterialAlertDialogBuilder(it.context)
                .setView(dialogView)
                .setNegativeButton("Cancel") { _, _ ->
                }
                .setPositiveButton("Send") { _, _ ->
                    val ratingBar = dialogView.findViewById<RatingBar>(R.id.reviewRatingBar)
                    val reviewText = dialogView.findViewById<EditText>(R.id.textReviewET)
                    val from = model.auth.currentUser!!.email
                    val to = currentTrip!!.driverEmail
                    if (ratingBar.rating == 0.0f)
                        Snackbar.make(
                            it.context,
                            it,
                            "Please insert a rating",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    else {
                        model.addReview(
                            Review(
                                from!!,
                                to,
                                ratingBar.rating.toString(),
                                reviewText.text.toString(),
                                true,
                                currentTrip!!.id
                            )
                        )
                    }
                }
                .show()
        }

        accountButton.setOnClickListener {
            model.setOtherUser(currentTrip!!.driverEmail)
            findNavController().navigate(
                R.id.action_tripDetailsFragment_to_showProfileFragment,
                bundleOf("parent" to TRIP_DETAILS_IS_PARENT)
            )
        }
    }

    private fun showAlertDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_trip_title))
            .setMessage(getString(R.string.delete_trip_confirm))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                Toast.makeText(
                    requireContext(),
                    getString(R.string.trip_removed_successfully),
                    Toast.LENGTH_SHORT
                ).show()
                activity?.onBackPressed()


                //delete this trip
                model.getPosition().observe(viewLifecycleOwner, { parentPosition ->
                    model.getMyTrips()
                        .observe(viewLifecycleOwner, { resource ->
                            when (resource) {
                                is Resource.Loading -> {
                                    //
                                }
                                is Resource.Success -> {

                                    resource.data[parentPosition].interestedUsers.forEach { user ->
                                        if (user.isAccepted) {
                                            model.removeFromBookings("${resource.data[parentPosition].id}_${user.email}")
                                        }
                                    }

                                    model.deleteTrip(resource.data[parentPosition].id)

                                    if (resource.data[parentPosition].carPhotoPath != null) {
                                        model.deletePhotoTrip(resource.data[parentPosition].carPhotoPath!!)
                                    }
                                }
                                is Resource.Failure -> {

                                    Toast.makeText(
                                        context,
                                        "Error in trip loading! Try later.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        })
                })

            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->

            }
            .show()
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (arguments?.getString("parent").equals(MY_TRIP_LIST_IS_PARENT)) {
            inflater.inflate(R.menu.edit_menu, menu)    //Edit menu button only in MY trip details
            val item: MenuItem? = menu.findItem(R.id.deleteButton)
            item?.isVisible = true
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.editButton -> {
                findNavController()
                    .navigate(
                        R.id.action_tripDetailsFragment_to_tripEditFragment
                    )
                true
            }
            R.id.deleteButton -> {
                showAlertDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

open class Item(val location: String, val timestamp: String)

class DepartureItem(location: String, timestamp: String) : Item(location, timestamp)

class IntermediateItem(location: String, timestamp: String) : Item(location, timestamp)

class ArrivalItem(location: String, timestamp: String) : Item(location, timestamp)

class ItemAdapter(private val items: List<Item>) :
    RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    class ItemViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val location = v.findViewById<TextView>(R.id.itemDetailsLocation)
        private val timestamp = v.findViewById<TextView>(R.id.itemDetailsTimestamp)

        fun bind(i: Item) {
            location.text = i.location
            timestamp.text = i.timestamp
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val layout = when (viewType) {
            R.layout.departure_item -> {
                layoutInflater.inflate(R.layout.departure_item, parent, false)
            }

            R.layout.arrival_item -> {
                layoutInflater.inflate(R.layout.arrival_item, parent, false)
            }
            else -> { //R.layout.intermediate_item -> {
                layoutInflater.inflate(R.layout.intermediate_item, parent, false)
            }
        }
        return ItemViewHolder(layout)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is DepartureItem -> {
                return R.layout.departure_item
            }
            is IntermediateItem -> {
                return R.layout.intermediate_item
            }
            is ArrivalItem -> {
                return R.layout.arrival_item
            }
            else -> super.getItemViewType(position)
        }
    }
}

class InterestedUserAdapter(
    private val users: List<User>,
    private val model: SharedViewModel,
    private val targetTrip: Trip,
    private val navController: NavController
) : RecyclerView.Adapter<InterestedUserAdapter.UserViewHolder>() {
    class UserViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val userImage = v.findViewById<ImageView>(R.id.userImage)
        private val userName = v.findViewById<TextView>(R.id.userName)
        private val userEmail = v.findViewById<TextView>(R.id.userEmail)
        private val acceptButton = v.findViewById<ImageButton>(R.id.acceptUserButton)
        private val rejectButton = v.findViewById<ImageButton>(R.id.rejectUserButton)
        private val reviewButton = v.findViewById<Button>(R.id.reviewButton)
        private val imageProfileShimmer =
            v.findViewById<ShimmerFrameLayout>(R.id.shimmerImageProfile)

        fun bind(
            u: User,
            model: SharedViewModel,
            targetTrip: Trip,
            navController: NavController
        ) {
            userImage.setImageResource(R.drawable.photo_default)
            imageProfileShimmer.startShimmer()
            val storage = Firebase.storage
            val storageRef = storage.reference
            val testRef = storageRef.child(u.email)
            testRef.metadata.addOnSuccessListener { metadata ->
                val size = metadata.sizeBytes
                testRef
                    .getBytes(ONE_MEGABYTE)
                    .addOnSuccessListener {
                        imageProfileShimmer.hideShimmer()
                        val imageBitmap = BitmapFactory.decodeByteArray(it, 0, size.toInt())
                        if (imageBitmap != null) {
                            userImage.setImageBitmap(imageBitmap)
                        }
                    }.addOnFailureListener {
                        Toast.makeText(
                            itemView.context,
                            "Error in downloading photo",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }.addOnFailureListener {
                imageProfileShimmer.hideShimmer()
                //Toast.makeText(itemView.context, "AAA", Toast.LENGTH_LONG).show()
            }

            userImage.setOnClickListener {
                model.setOtherUser(u.email)
                navController.navigate(
                    R.id.action_tripDetailsFragment_to_showProfileFragment,
                    bundleOf("parent" to TRIP_DETAILS_IS_PARENT)
                )
            }
            userName.text = u.name
            userEmail.text = u.email
            acceptButton.setOnClickListener {
                model.acceptUser(targetTrip, u)
            }
            rejectButton.setOnClickListener {
                model.updateTripInterestedUser(targetTrip, false, u)
            }

            reviewButton.setOnClickListener {
                val inflater =
                    it.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val dialogView = inflater.inflate(R.layout.dialog_review, null)
                val dialogFilter = MaterialAlertDialogBuilder(it.context)
                    .setView(dialogView)
                    .setNegativeButton("Cancel") { _, _ ->
                    }
                    .setPositiveButton("Send") { _, _ ->
                        val ratingBar = dialogView.findViewById<RatingBar>(R.id.reviewRatingBar)
                        val reviewText = dialogView.findViewById<EditText>(R.id.textReviewET)
                        val from = model.auth.currentUser!!.email
                        val to = u.email
                        if (ratingBar.rating == 0.0f)
                            Snackbar.make(
                                it.context,
                                it,
                                "Please insert a rating",
                                Snackbar.LENGTH_SHORT
                            ).show()
                        else {
                            model.addReview(
                                Review(
                                    from!!,
                                    to,
                                    ratingBar.rating.toString(),
                                    reviewText.text.toString(),
                                    false,
                                    targetTrip.id
                                )
                            )
                            reviewButton.visibility = View.INVISIBLE
                        }
                    }
                    .show()
            }

            MainScope().launch {
                if (u.isAccepted) {
                    val alreadySent = withContext(Dispatchers.IO) {
                        model.reviewAlreadySent(
                            model.auth.currentUser!!.email!!,
                            u.email,
                            targetTrip.id
                        )
                    }

                    if (!alreadySent)
                        reviewButton.visibility = View.VISIBLE
                }
            }
        }

        fun unbind() {
            acceptButton.setOnClickListener { null }
            rejectButton.setOnClickListener { null }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val layout = when (viewType) {
            R.layout.user_accepted_item -> {
                layoutInflater.inflate(R.layout.user_accepted_item, parent, false)
            }
            else -> { //R.layout.user_item -> {
                layoutInflater.inflate(R.layout.user_item, parent, false)
            }
        }
        return UserViewHolder(layout)
    }

    override fun getItemCount() = users.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position], model, targetTrip, navController)
    }

    override fun onViewRecycled(holder: UserViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    override fun getItemViewType(position: Int): Int {
        if (users[position].isAccepted) {
            return R.layout.user_accepted_item
        }

        return R.layout.user_item
    }
}