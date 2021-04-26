package it.polito.mad.group08.carpooling

import android.content.Context
import android.content.SharedPreferences
import android.opengl.Visibility
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.math.BigDecimal

val EDIT_BUTTON_CLICKED = 1
val CARD_CLICKED = 2
val FAB_CLICKED = 3

class TripListFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TripAdapter
    private lateinit var trips: MutableList<Trip>
    private lateinit var sharedPref: SharedPreferences

    private fun navigationClickListener(mode: Int, trip: Trip?, position: Int?) {
        val navController = findNavController()
        if (mode == EDIT_BUTTON_CLICKED && position != null && trip != null) {
            val bundle = bundleOf("mode" to EDIT_BUTTON_CLICKED, "pos" to position, "trip" to Gson().toJson(trip))
            setFragmentResult("tripEdit", bundle)
            //navController.navigate(R.id.action_tripListFragment_to_tripEditFragment)
            //Toast.makeText(context, "EDIT: From ${trip.departureLocation} to ${trip.arrivalLocation}!", Toast.LENGTH_SHORT).show()
        } else if (mode == CARD_CLICKED && trip != null) {
            val bundle = bundleOf("trip" to Gson().toJson(trip))
            setFragmentResult("tripDetails", bundle)
            navController.navigate(R.id.action_tripListFragment_to_tripDetailsFragment)
            //Toast.makeText(context, "DETAILS: From ${trip.departureLocation} to ${trip.arrivalLocation}!", Toast.LENGTH_SHORT).show()
        } else if (mode == FAB_CLICKED) {
            val bundle = bundleOf("mode" to FAB_CLICKED)
            setFragmentResult("tripAdd", bundle)
            //navController.navigate(R.id.action_tripListFragment_to_tripEditFragment)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)!!
        val tripsJSON = sharedPref.getString("trips", Gson().toJson(mutableListOf<Trip>()).toString())
        val type: Type = object : TypeToken<List<Trip?>?>() {}.type
        trips = GsonBuilder().create().fromJson(tripsJSON, type)
        val checkpoints = listOf(
                CheckPoint("Via Roma 32, Torino", "2021-04-25\n08:00"),
                CheckPoint("Via Milano 23, Firenze", "2021-04-25\n11:00"),
                CheckPoint("Via Torino 44, Roma", "2021-04-25\n17:30"),
                CheckPoint("Via Firenze 33, Napoli", "25/03/2021\n19:00")
        )

        val trip = Trip("carPhotoPath", "Toyota Le mans 3000 Diesel",
                "Pino Pino", 4.2f, checkpoints, "01h30m",
                3, BigDecimal(35.50),
                "In this Trip have th small space. See you soon.")
        trips.add(trip)
        trips.add(trip)

        setFragmentResultListener("tripEdited") { requestKey, bundle ->
            if (requestKey == "tripEdited") {
                val position = bundle.getInt("pos")
                val tripEditedJSON = bundle.getString("trip")
                val type: Type = object : TypeToken<Trip?>() {}.type
                val tripEdited: Trip = GsonBuilder().create().fromJson(tripEditedJSON, type)

                adapter.onItemChange(tripEdited, position)
                saveInPreferences()
            }
        }

        setFragmentResultListener("tripAdded") { requestKey, bundle ->
            if (requestKey == "tripAdded") {
                val tripAddedJSON = bundle.getString("trip")
                val type: Type = object : TypeToken<Trip?>() {}.type
                val tripAdded: Trip = GsonBuilder().create().fromJson(tripAddedJSON, type)
                println("CALLBACK")
                adapter.onItemAdded(tripAdded)
                saveInPreferences()
            }
        }


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //TODO if sharedPref empty message on the screen
        val view = inflater.inflate(R.layout.fragment_trip_list, container, false)
        val emptyTextView: TextView = view.findViewById(R.id.emptyTextView)
        recyclerView = view.findViewById(R.id.tripListRecyclerView)

        /*if(trips.size == 0){
            recyclerView.visibility = View.GONE
            emptyTextView.visibility = View.VISIBLE
        }
        else{
            recyclerView.visibility = View.VISIBLE
            emptyTextView.visibility = View.GONE
        }*/
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = TripAdapter(trips) { mode: Int, tripItem: Trip, position: Int? -> navigationClickListener(mode, tripItem, position) }
        recyclerView.adapter = adapter
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val addFab: FloatingActionButton = view.findViewById(R.id.add_fab)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && addFab.visibility == View.VISIBLE) {
                    addFab.hide()
                } else if (dy < 0 && addFab.visibility != View.VISIBLE) {
                    addFab.show()
                }
            }
        })

        addFab.setOnClickListener {
            val anim: Animation = AnimationUtils.loadAnimation(addFab.context, R.anim.zoom)
            anim.duration = 150
            addFab.startAnimation(anim)
            navigationClickListener(FAB_CLICKED, null, null)
        }
    }

    private fun saveInPreferences() {
        with(sharedPref.edit()) {
            val tripsJSON = Gson().toJson(trips)
            putString("trips", tripsJSON)
            apply()
        }
    }

    data class CheckPoint(var location: String, var timestamp: String)

    data class Trip(var carPhotoPath: String?,
                    var carDescription: String,
                    var driverName: String,
                    var driverRate: Float,
                    var checkPoints: List<CheckPoint>,
                    var estimatedDuration: String,
                    var availableSeats: Int,
                    var seatPrice: BigDecimal,
                    var description: String
    )


    class TripAdapter(private val tripsAdapter: MutableList<Trip>, private val clickListener: (Int, Trip, Int?) -> Unit) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

        class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val departureLocation: TextView = itemView.findViewById(R.id.departureLocation)
            private val arrivalLocation: TextView = itemView.findViewById(R.id.arrivalLocation)
            private val editButton: Button = itemView.findViewById(R.id.editButton)
            private val card: CardView = itemView.findViewById(R.id.card)


            fun bind(trip: Trip, clickListener: (Int, Trip, Int?) -> Unit) {
                departureLocation.text = trip.checkPoints[0].location
                arrivalLocation.text = trip.checkPoints[trip.checkPoints.size - 1].location
                card.setOnClickListener {
                    clickListener(CARD_CLICKED, trip, null)
                }
                editButton.setOnClickListener {
                    clickListener(EDIT_BUTTON_CLICKED, trip, bindingAdapterPosition)
                }
            }

            fun unbind() {
                card.setOnClickListener { null }
                editButton.setOnClickListener { null }
            }
        }

        override fun getItemCount() = tripsAdapter.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.trip_item_layout, parent, false)
            return TripViewHolder(v)
        }

        override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
            holder.bind(tripsAdapter[position], clickListener)
        }

        override fun onViewRecycled(holder: TripViewHolder) {
            super.onViewRecycled(holder)
            holder.unbind()
        }

        fun onItemChange(tripEdited: Trip, position: Int) {
            tripsAdapter[position] = tripEdited
            notifyItemChanged(position)
        }

        fun onItemAdded(tripAdded: Trip) {
            tripsAdapter.add(tripAdded)
            notifyItemInserted(tripsAdapter.size - 1)
        }
    }
}