package it.polito.mad.group08.carpooling

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.*


const val MY_TRIPS_IS_PARENT = "TRIPS"
const val OTHER_TRIPS_PARENT = "OTHERS_TRIPS"

class TripDetailsFragment : Fragment() {
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

    private val model: SharedViewModel by activityViewModels()


    private fun takeSavedPhoto(name: String?) {
        try {
            if(name != null) {
                view?.context?.applicationContext?.openFileInput(name).use {
                    val imageBitmap = BitmapFactory.decodeStream(it)
                    if (imageBitmap != null)
                        carPhotoPath.setImageBitmap(imageBitmap)
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("ShowToast")
    private fun setTripInformation(trip: Trip){
        takeSavedPhoto(trip.carPhotoPath)
        carDescription.text = trip.carDescription

        driverName.text = trip.driverName
        driverRate.rating = trip.driverRate

        //TRIP RECYCLER VIEW
        intermediateTripsRecyclerView.layoutManager = LinearLayoutManager(context)
        val departureCheckpoint = trip.checkPoints.first()
        val arrivalCheckpoint = trip.checkPoints.last()

        val departureItem = DepartureItem(departureCheckpoint.location, departureCheckpoint.timestamp)
        val arrivalItem = ArrivalItem(arrivalCheckpoint.location, arrivalCheckpoint.timestamp)

        val startEndCheckpoints = listOf(departureItem, arrivalItem)
        val allCheckpoints: MutableList<Item> = mutableListOf()
        if(trip.checkPoints.size > 2){
            intermediateTripsShowHideButton.text = getString(R.string.show_intermediate_stops)
            trip.checkPoints.forEachIndexed { index, checkPoint ->
                when(index){
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
        }
        else{
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

        estimatedDuration.text = getString(R.string.estimated_duration_msg, calcDuration(trip.checkPoints.first(), trip.checkPoints.last()))
        availableSeats.text = getString(R.string.available_seats_msg, trip.availableSeats)
        seatPrice.text = getString(R.string.seat_price_msg, trip.seatPrice.toString())
        description.text = trip.description

        //TODO do it in landscape
        //TODO would be nice change color too

        // FAB (FOR USER != OWNER)
        if(trip.driverEmail != model.getAccount().email){
            if(model.bookingIsAccepted(trip.id)){ //user alredy show favorite and owner accepted
                showInterestFab.setImageResource(R.drawable.check)
                showInterestFab.setOnClickListener {
                    Toast.makeText(context, "You already booked this trip!", Toast.LENGTH_LONG).show()
                }
            }else{
                if(model.userIsInterested(trip)){
                    showInterestFab.setImageResource(R.drawable.ic_baseline_clear_24)
                }else{
                    showInterestFab.setImageResource(R.drawable.ic_baseline_favorite_24)
                }

                showInterestFab.setOnClickListener {
                    val anim: Animation = AnimationUtils.loadAnimation(showInterestFab.context, R.anim.zoom)
                    anim.duration = 150
                    showInterestFab.startAnimation(anim)

                    if(model.userIsInterested(trip)){ // Already interested, but she would to cancel
                        model.updateTripInterestedUser(trip, false, null)
                        showInterestFab.setImageResource(R.drawable.ic_baseline_favorite_24)
                    }
                    else {
                        model.updateTripInterestedUser(trip, true, null)
                        showInterestFab.setImageResource(R.drawable.ic_baseline_clear_24)
                    }
                }
            }
        }

        //INTERESTED USERS RECYCLER VIEW (FOR THE OWNER)
        interestedUsersRecyclerView.layoutManager = LinearLayoutManager(context)
        interestedUsersRecyclerView.visibility = View.GONE

        if(trip.interestedUsers.isNotEmpty()){
            interestedUsersShowHideButton.text = getString(R.string.show_interested_users)
            interestedUsersShowHideButton.visibility = View.VISIBLE
            interestedUsersRecyclerView.adapter = InterestedUserAdapter(trip.interestedUsers, model, trip, findNavController())
        }
        else{
            interestedUsersShowHideButton.visibility = View.GONE
        }

        //UPDATE BUTTON STATUS INTERESTED USERS (FOR OWNER)
        var j = 0
        interestedUsersShowHideButton.setOnClickListener {
            if (j % 2 == 0) {
                interestedUsersShowHideButton.text = getString(R.string.hide_intermediate_stops)
                interestedUsersRecyclerView.visibility = View.VISIBLE
            } else {
                interestedUsersShowHideButton.text = getString(R.string.show_interested_users)
                interestedUsersRecyclerView.visibility = View.GONE
            }
            j++
        }
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
        //println("$days $hours $minutes")
        return concatenate(days.toInt(), hours.toInt(), minutes.toInt())
    }

    private fun concatenate(days: Int, hours: Int, minutes: Int): String {
        var finalString = ""
        if(days != 0) {
            finalString = "$finalString $days g"
        }
        if(hours != 0) {
            val newHours = hours - days*24
            if (newHours != 0)
                finalString = "$finalString $newHours h"
        }
        if(minutes != 0) {
            val newMinutes = minutes - hours*60
            if(newMinutes != 0)
                finalString = "$finalString $newMinutes m"
        }

        return finalString
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val v = inflater.inflate(R.layout.fragment_trip_details, container, false)

        setHasOptionsMenu(true)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // FIND VIEW
        carPhotoPath = view.findViewById(R.id.carPhoto)
        carDescription = view.findViewById(R.id.carName)
        driverName = view.findViewById(R.id.driverName)
        driverRate = view.findViewById(R.id.driverRate)
        intermediateTripsRecyclerView = view.findViewById(R.id.tripRecyclerView)
        estimatedDuration = view.findViewById(R.id.estimatedDuration)
        availableSeats = view.findViewById(R.id.availableSeats)
        seatPrice = view.findViewById(R.id.seatPrice)
        description = view.findViewById(R.id.tripDescription)

        intermediateTripsShowHideButton = view.findViewById<Button>(R.id.showHideIntermediateSteps)

        showInterestFab = view.findViewById(R.id.show_interest_fab)

        interestedUsersRecyclerView = view.findViewById(R.id.interestedUserRecyclerView)
        interestedUsersShowHideButton = view.findViewById(R.id.showHideInterestedUsers)

        // INITIALIZE DATA
        //NOTE: please notice the nested call. You can access parentPosition only when it's returned
        model.getPosition().observe(viewLifecycleOwner, Observer<Int> {parentPosition ->
            when(arguments?.getString("parent")){
                MY_TRIPS_IS_PARENT -> {
                    model.getTrips()
                            .observe(viewLifecycleOwner, Observer<MutableList<Trip>> { tripsDB ->
                        // POPULATE VIEW WITH DATA
                        setTripInformation(tripsDB[parentPosition])
                        showInterestFab.hide()
                    })
                }
                OTHER_TRIPS_PARENT -> {
                    model.getOthersTrips()
                            .observe(viewLifecycleOwner, Observer<MutableList<Trip>> { tripsDB ->
                        // POPULATE VIEW WITH DATA
                        model.getBookings().observe(viewLifecycleOwner, Observer<MutableList<Booking>> {
                            setTripInformation(tripsDB[parentPosition])
                            if(tripsDB[parentPosition].availableSeats > 0)
                                showInterestFab.show()
                            else
                                showInterestFab.hide()
                            interestedUsersRecyclerView.visibility = View.GONE
                            interestedUsersShowHideButton.visibility = View.INVISIBLE
                        })
                    })
                }
                else -> {
                    Toast.makeText(context, "Error in laod the Trip!", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if(arguments?.getString("parent").equals(MY_TRIPS_IS_PARENT))
            inflater.inflate(R.menu.edit_menu, menu)    //Edit menu button only in MY trip details
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
            else -> super.onOptionsItemSelected(item)
        }
    }
}

open class Item(val location: String, val timestamp: String)

class DepartureItem(location: String, timestamp: String) : Item(location, timestamp)

class IntermediateItem(location: String, timestamp: String) : Item(location, timestamp)

class ArrivalItem(location: String, timestamp: String) : Item(location, timestamp)

class ItemAdapter(private val items: List<Item>) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

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
        private val navController: NavController): RecyclerView.Adapter<InterestedUserAdapter.UserViewHolder>(){
    class UserViewHolder(v: View) : RecyclerView.ViewHolder(v){
        private val userImage = v.findViewById<ImageButton>(R.id.userImage)
        private val userName = v.findViewById<TextView>(R.id.userName)
        private val userEmail = v.findViewById<TextView>(R.id.userEmail)
        private val acceptButton = v.findViewById<ImageButton>(R.id.acceptUserButton)
        private val rejectButton = v.findViewById<ImageButton>(R.id.rejectUserButton)

        fun bind(u: User, model: SharedViewModel, targetTrip: Trip, navController: NavController) {
            userImage.setImageResource(R.drawable.photo_default)
            //TODO return email to showProfileUser in show/hide Interested User
            userImage.setOnClickListener {
                model.setOtherUser(u.email)
                navController.navigate(R.id.action_tripDetailsFragment_to_showProfileFragment,bundleOf("parent" to "OTHERUSER"))
            }
            userName.text = u.name
            userEmail.text = u.email
            acceptButton.setOnClickListener {
                model.acceptUser(targetTrip, u)
            }
            rejectButton.setOnClickListener {
                model.updateTripInterestedUser(targetTrip, false, u)
                //TODO should I prevent this user to express her preferences again?
            }
        }

        fun unbind(){
            acceptButton.setOnClickListener { null }
            rejectButton.setOnClickListener { null }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val layout = LayoutInflater.from(parent.context).inflate(R.layout.user_item, parent, false)
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
}