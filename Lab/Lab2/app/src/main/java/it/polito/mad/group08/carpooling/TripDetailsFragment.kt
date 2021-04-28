package it.polito.mad.group08.carpooling

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.*
import android.widget.Button
import androidx.fragment.app.Fragment
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.FileNotFoundException
import java.lang.reflect.Type


class TripDetailsFragment : Fragment() {
    private lateinit var carPhotoPath: ImageView
    private lateinit var carDescription: TextView
    private lateinit var driverName: TextView
    private lateinit var driverRate: RatingBar
    private var position: Int = -1
    private lateinit var recyclerView: RecyclerView
    private lateinit var showHideButton: Button

    private lateinit var estimatedDuration: TextView
    private lateinit var availableSeats: TextView
    private lateinit var seatPrice: TextView

    private lateinit var description: TextView

    //List[0] = departure; list[0+i] = intermediateStops; List[N-1] = arrival
    private lateinit var trip: TripListFragment.Trip


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpResultListener()
    }


    private fun setUpResultListener() {
        setFragmentResultListener("tripDetails") { requestKey, bundle ->
            onFragmentResult(requestKey, bundle)
        }
    }

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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("trip", Gson().toJson(trip))
    }

    //called after onViewCreated
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if(savedInstanceState!=null){
            val tripJSON = savedInstanceState.getString("trip")
            val type: Type = object : TypeToken<TripListFragment.Trip?>() {}.type
            trip = GsonBuilder().create().fromJson(tripJSON, type)
            setTripInformation(trip)
        }
    }

    private fun setTripInformation(trip: TripListFragment.Trip){
        takeSavedPhoto(trip.carPhotoPath)
        carDescription.text = trip.carDescription
        driverName.text = trip.driverName
        driverRate.rating = trip.driverRate

        val departureCheckpoint = trip.checkPoints.first()
        val arrivalCheckpoint = trip.checkPoints.last()

        val departureItem = DepartureItem(departureCheckpoint.location, departureCheckpoint.timestamp)
        val arrivalItem = ArrivalItem(arrivalCheckpoint.location, arrivalCheckpoint.timestamp)

        val startEndCheckpoints = listOf(departureItem, arrivalItem)
        val allCheckpoints: MutableList<Item> = mutableListOf()
        if(trip.checkPoints.size > 2){
            trip.checkPoints.forEachIndexed { index, checkPoint ->
                when(index){
                    0 -> {
                        allCheckpoints.add(
                                DepartureItem(checkPoint.location, checkPoint.timestamp)
                        )
                    }
                    trip.checkPoints.size-1 -> {
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
            showHideButton.visibility = View.GONE
        }

        recyclerView.adapter = ItemAdapter(startEndCheckpoints)

        var i = 0
        showHideButton.setOnClickListener {
            if (i % 2 == 0) {
                showHideButton.text = getString(R.string.hide_intermediate_stops)
                recyclerView.adapter = ItemAdapter(allCheckpoints)
            } else {
                showHideButton.text = getString(R.string.show_intermediate_stops)
                recyclerView.adapter = ItemAdapter(startEndCheckpoints)
            }
            i++
        }

        estimatedDuration.text = getString(R.string.estimated_duration_msg, trip.estimatedDuration)
        availableSeats.text = getString(R.string.available_seats_msg, trip.availableSeats)
        seatPrice.text = getString(R.string.seat_price_msg, trip.seatPrice.toString())
        description.text = trip.description
    }

    private fun onFragmentResult(requestKey: String, bundle: Bundle) {
        if (requestKey === "tripDetails") {
            position = bundle.getInt("pos")
            val tripJSON = bundle.getString("trip")
            val type: Type = object : TypeToken<TripListFragment.Trip?>() {}.type
            trip = GsonBuilder().create().fromJson(tripJSON, type)

            setTripInformation(trip)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val v = inflater.inflate(R.layout.fragment_trip_details, container, false)

        setHasOptionsMenu(true)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        carPhotoPath = view.findViewById(R.id.carPhoto)
        carDescription = view.findViewById(R.id.carName)
        driverName = view.findViewById(R.id.driverName)
        driverRate = view.findViewById(R.id.driverRate)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        estimatedDuration = view.findViewById(R.id.estimatedDuration)
        availableSeats = view.findViewById(R.id.availableSeats)
        seatPrice = view.findViewById(R.id.seatPrice)
        description = view.findViewById(R.id.tripDescription)

        showHideButton = view.findViewById<Button>(R.id.show_hide)
        showHideButton.text = getString(R.string.show_intermediate_stops)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.edit_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.editButton -> {
                val bundle = bundleOf("pos" to position, "trip" to Gson().toJson(trip))
                setFragmentResult("fromDetailsToEdit", bundle)
                findNavController().navigate(R.id.action_tripDetailsFragment_to_tripEditFragment)
                println("Hello EditFragment")
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