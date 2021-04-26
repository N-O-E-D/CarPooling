package it.polito.mad.group08.carpooling

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import androidx.fragment.app.Fragment
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.math.BigDecimal

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

data class Trip(
    val carPhotoPath: String,
    val carDescription: String,

    val driverName: String,
    val driverRate: Float,

    //List[0] = departure; list[0+i] = intermediateStops; List[N] = arrival
    val checkPoints: List<CheckPoint>,

    val estimatedDuration: String,//  (hh:mm)

    val availableSeats: Int,
    val seatPrice: BigDecimal,
    val description: String
)

data class CheckPoint(val location: String, val timestamp: String)
// location: String (Via AAA 32, Torino),
// timestamp: String (hh:mm, dd/mm/yyyy),

class TripDetailsFragment : Fragment() {
    private lateinit var carPhotoPath: ImageView
    private lateinit var carDescription: TextView
    private lateinit var driverName: TextView
    private lateinit var driverRate: RatingBar

    //List[0] = departure; list[0+i] = intermediateStops; List[N-1] = arrival
    private lateinit var checkPoints: List<CheckPoint>

    private lateinit var showHideButton: Button

    private lateinit var estimatedDuration: TextView

    private lateinit var availableSeats: TextView
    private lateinit var seatPrice: TextView
    private lateinit var description: TextView

    private lateinit var trip: Trip


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        arguments?.let {
//            param1 = it.getString(ARG_PARAM1)
//            param2 = it.getString(ARG_PARAM2)
//        }


        //TODO it doesn't work because it's asynchronous
//        setFragmentResultListener("keyFragment"){requestKey, bundle ->
//            if(requestKey == "keyFragment"){
//                val jsonObj = bundle.getString("trip")
//                val type: Type = object : TypeToken<Trip?>() {}.type
//                trip = GsonBuilder().create().fromJson(jsonObj, type)
//                Log.d("AAA", trip.toString())
//            }
//        }

        trip = Trip("carPhotoPath", "Toyota Le mans 3000 Diesel",
            "Pino Guidatutto", 4.2f, listOf(), "22h30m",
            3, BigDecimal(35.50),
            "In this Trip you will travel with a young driver which has" +
                    " a good sense of humor. You have the possibility to take no more than 1 " +
                    " trolley and 1 small bag because of the small space. See you soon.")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val v = inflater.inflate(R.layout.fragment_trip_details, container, false);

        setHasOptionsMenu(true);
        return v;
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        carPhotoPath = view.findViewById(R.id.carPhoto)
        //carPhotoPath.setImageURI()

        carDescription = view.findViewById(R.id.carName)
        carDescription.text = trip.carDescription

        driverName = view.findViewById(R.id.driverName)
        driverName.text = trip.driverName

        driverRate = view.findViewById(R.id.driverRate)
        driverRate.rating = trip.driverRate

        val departureItem = DepartureItem("Via Roma 32, Torino", "2021-04-25\n08:00")
        val intermediateItem = IntermediateItem("Via Milano 23, Firenze", "2021-04-25\n11:00")
        val intermediateItem2 = IntermediateItem("Via Torino 44, Roma", "2021-04-25\n17:30")
        val arrivalItem = ArrivalItem("Via Firenze 33, Napoli", "25/03/2021\n19:00")

        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        val trip1 = mutableListOf(departureItem, intermediateItem, intermediateItem2, arrivalItem)
        val trip2 = mutableListOf(departureItem, arrivalItem)
        recyclerView.adapter = ItemAdapter(trip2)

        estimatedDuration = view.findViewById(R.id.estimatedDuration)
        estimatedDuration.text = "Estimated duration: ${trip.estimatedDuration}"

        availableSeats = view.findViewById(R.id.availableSeats)
        availableSeats.text = "Available Seats: ${trip.availableSeats}"

        seatPrice = view.findViewById(R.id.seatPrice)
        seatPrice.text = "Price/Person: €${trip.seatPrice}"

        description = view.findViewById(R.id.tripDescription)
        description.text = trip.description

        showHideButton = view.findViewById<Button>(R.id.show_hide)
        showHideButton.text = "Show Intermediate Stops"
        var i = 0
        showHideButton.setOnClickListener {
            if(i%2 == 0){
                showHideButton.text = "Hide Intermediate Stops"
                recyclerView.adapter = ItemAdapter(trip1)
            }
            else{
                showHideButton.text = "Show Intermediate Stops"
                recyclerView.adapter = ItemAdapter(trip2)
            }
            i++
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.edit_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.editButton -> {
//                val bundle = bundleOf("trip" to Gson().toJson(Trip))
//                setFragmentResult("KeyFragment", bundle)
                //findNavController().navigate(R.id.action_nav_home_to_tripDetailsFragment)
                println("Hello EditFragment")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

open class Item(val location: String, val timestamp: String)

class DepartureItem(location: String, timestamp: String): Item(location, timestamp)

class IntermediateItem(location: String, timestamp: String): Item(location, timestamp)

class ArrivalItem(location: String, timestamp: String): Item(location, timestamp)

class ItemAdapter(private val items: List<Item>): RecyclerView.Adapter<ItemAdapter.ItemViewHolder>(){

    class ItemViewHolder(v: View): RecyclerView.ViewHolder(v){
        private val location = v.findViewById<TextView>(R.id.itemDetailsLocation)
        private val timestamp = v.findViewById<TextView>(R.id.itemDetailsTimestamp)

        fun bind(i: Item){
            location.text = i.location
            timestamp.text = i.timestamp
        }
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val layout = when(viewType){
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
        return when(items[position]){
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