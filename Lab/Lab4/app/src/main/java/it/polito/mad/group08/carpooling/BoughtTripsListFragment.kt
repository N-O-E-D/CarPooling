package it.polito.mad.group08.carpooling

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BoughtTripsListFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TripAdapter
    private lateinit var emptyTextView: TextView
    private val model: SharedViewModel by activityViewModels()

    //    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        activity?.onBackPressedDispatcher?.addCallback(this, object: OnBackPressedCallback(true){
//            override fun handleOnBackPressed() {
//                activity!!.finish()
//            }
//        })
//    }
    private fun navigationClickListener(mode: Int, trip: Trip?, position: Int?) {
        val navController = findNavController()
        if (mode == CARD_CLICKED && trip != null) {
            model.setPosition(position!!)
            navController.navigate(
                R.id.action_othersTripListFragment_to_tripDetailsFragment,
                bundleOf("parent" to "OTHERS_TRIPS")
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bought_trips_list, container, false)
        emptyTextView = view.findViewById(R.id.emptyTextView)
        recyclerView = view.findViewById(R.id.boughtTripListRecyclerView)

        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                recyclerView.layoutManager = LinearLayoutManager(context)
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                recyclerView.layoutManager = GridLayoutManager(context, 3)
            }
        }

        setHasOptionsMenu(true)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // DECOUPLE DATA FROM UI
        model.getMyBookedTrips()
            .observe(viewLifecycleOwner, Observer<Resource<List<Trip>>> { resource ->
                // update UI
                when (resource) {
                    is Resource.Loading -> {
                        Log.d("AAAA", "IS LOADING")
                        //showLoading()
                    }
                    is Resource.Success -> {
                        //displayCart(resource.data) // Do something with your data.
                        //hideLoading()
                        Log.d("AAAA", "IS SUCCESS")

                        if (resource.data.isEmpty()) {
                            recyclerView.visibility = View.GONE
                            emptyTextView.visibility = View.VISIBLE
                        } else {
                            recyclerView.visibility = View.VISIBLE
                            emptyTextView.visibility = View.GONE
                        }

                        //TODO
                        adapter = TripAdapter(resource.data, model) { mode: Int, tripItem: Trip, position: Int? -> navigationClickListener(mode, tripItem, position) }
                        recyclerView.adapter = adapter
                    }
                    is Resource.Failure -> {
                        //showError(resource.throwable) // Do something with your error.
                        //hideLoading()
                        Log.d("AAAA", "IS ERROR")
                    }
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (activity?.isChangingConfigurations == false) {
            model.setFilter(Filter())
        }
    }
}

//class TripAdapter(private val tripsAdapter: MutableList<Trip>,
//                  private val model: SharedViewModel,
//                  private val clickListener: (Int, Trip, Int?) -> Unit
//) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {
//
//    class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        private val departureLocation: TextView = itemView.findViewById(R.id.departureLocation)
//        private val arrivalLocation: TextView = itemView.findViewById(R.id.arrivalLocation)
//        private val departureTimestamp: TextView = itemView.findViewById(R.id.departureTimestamp)
//        private val arrivalTimestamp: TextView = itemView.findViewById(R.id.arrivalTimestamp)
//        private val showInterestButton: Button = itemView.findViewById(R.id.editButton)
//        private val card: CardView = itemView.findViewById(R.id.card)
//
//
//        fun bind(trip: Trip, model: SharedViewModel, clickListener: (Int, Trip, Int?) -> Unit) {
//            departureLocation.text = trip.checkPoints[0].location
//            arrivalLocation.text = trip.checkPoints[trip.checkPoints.size - 1].location
//            departureTimestamp.text = trip.checkPoints[0].timestamp
//            arrivalTimestamp.text = trip.checkPoints[trip.checkPoints.size - 1].timestamp
//            showInterestButton.text = itemView.context.getString(R.string.trip_show_interest)
//
//            if (model.bitmaps[trip.id] == null) {
//                if(trip.carPhotoPath != null && trip.carPhotoPath != "") {
//                    MainScope().launch {
//                        val size = withContext(Dispatchers.IO) {
//                            model.downloadMetadataPhoto(trip.carPhotoPath!!).sizeBytes
//                        }
//                        val bitmap = withContext(Dispatchers.IO) {
//                            model.downloadPhoto(size, trip.carPhotoPath!!)
//                        }
//
//                        model.bitmaps[trip.id] = bitmap
//
//                        when(itemView.context.resources.configuration.orientation){
//                            Configuration.ORIENTATION_PORTRAIT -> {
//                                itemView.findViewById<ImageView>(R.id.carPhoto)
//                                    .setImageBitmap(model.bitmaps[trip.id])
//                            }
//                        }
//                    }
//                }
//            } else {
//                when(itemView.context.resources.configuration.orientation){
//                    Configuration.ORIENTATION_PORTRAIT -> {
//                        itemView.findViewById<ImageView>(R.id.carPhoto)
//                            .setImageBitmap(model.bitmaps[trip.id])
//                    }
//                }
//            }
//
//
//            card.setOnClickListener {
//                clickListener(CARD_CLICKED, trip, bindingAdapterPosition)
//            }
//
//            //SHOW INTEREST BUTTON
//            if(model.bookingIsAccepted(trip.id)){ //user already show favorite and owner accepted
//                showInterestButton.text = itemView.context.getString(R.string.trip_already_booked)
//                showInterestButton.isClickable = false
//            }else{
//                if(model.userIsInterested(trip)){
//                    showInterestButton.text = itemView.context.getString(R.string.trip_remove_interest)
//                }else{
//                    showInterestButton.text = itemView.context.getString(R.string.trip_show_interest)
//                }
//
//                showInterestButton.setOnClickListener {
//                    if(model.userIsInterested(trip)){ // Already interested, but she would to cancel
//                        model.updateTripInterestedUser(trip, false, null)
//                        showInterestButton.text = itemView.context.getString(R.string.trip_show_interest)
//                    }
//                    else {
//                        model.updateTripInterestedUser(trip, true, null)
//                        showInterestButton.text = itemView.context.getString(R.string.trip_remove_interest)
//                    }
//                }
//
//                if(trip.availableSeats == 0){
//                    showInterestButton.text = itemView.context.getString(R.string.trip_no_seats)
//                    showInterestButton.isClickable = false
//                }
//            }
//        }
//
//        fun unbind() {
//            card.setOnClickListener { null }
//            showInterestButton.setOnClickListener { null }
//        }
//    }
//
//    override fun getItemCount() = tripsAdapter.size
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
//        val v = LayoutInflater.from(parent.context).inflate(R.layout.trip_item_layout, parent, false)
//        return TripViewHolder(v)
//    }
//
//    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
//        holder.bind(tripsAdapter[position], model, clickListener)
//    }
//
//    override fun onViewRecycled(holder: TripViewHolder) {
//        super.onViewRecycled(holder)
//        holder.unbind()
//    }
//}