package it.polito.mad.group08.carpooling

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.sql.Timestamp

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class TripDetailsFragment : Fragment() {
    private lateinit var carPhoto: ImageView
    private lateinit var carName: TextView
    private lateinit var driverName: TextView
    private val departureLocation: String? = null
    private val departureTimestamp: Timestamp? = null
    private val arrivalLocation: String? = null
    private val estimatedDuration: Timestamp? = null
    private lateinit var availableSeats: TextView
    private lateinit var seatPrice: TextView
    private lateinit var description: TextView
    private val intermediateStops = mutableSetOf<IntermediateStops>()

    //TODO what i should receive from caller(TripListFragment)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        arguments?.let {
//            param1 = it.getString(ARG_PARAM1)
//            param2 = it.getString(ARG_PARAM2)
//        }
    }

// Note: onCreateView() is not needed since the layout is passed in the constructor
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val v = inflater.inflate(R.layout.fragment_trip_details, container, false);

        //TODO only the owner has edit option
        setHasOptionsMenu(true);
        return v;
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.edit_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.editButton -> {
                //findNavController().navigate(R.id.action_nav_home_to_tripDetailsFragment)
                println("Hello EditFragment")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

// Note: companion object{} should not be needed since no one will instantiate TripDetailsFragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        carPhoto = view.findViewById<ImageView>(R.id.carPhoto)

        carName = view.findViewById<TextView>(R.id.carName)
        carName.text = "Toyota Le mans 3000 Diesel"

        driverName = view.findViewById<TextView>(R.id.driverName)
        driverName.text = "Pino Guidatutto"

        val departureItem = DepartureItem("Torino, Via Roma, 32", "08:00")
        val intermediateItem = IntermediateItem("Firenze, Via Milano, 23", "11:00")
        val intermediateItem2 = IntermediateItem("Roma, Via Torino, 44", "17:30")
        val arrivalItem = ArrivalItem("Napoli, Via Firenze, 33", "19:00")

        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        val trip = listOf(departureItem, intermediateItem, intermediateItem2, arrivalItem)
        recyclerView.adapter = ItemAdapter(trip)

        availableSeats = view.findViewById<TextView>(R.id.availableSeats)
        availableSeats.text = "Available Seats: 3"

        seatPrice = view.findViewById<TextView>(R.id.seatPrice)
        seatPrice.text = "Price/Person €35.50"

        description = view.findViewById<TextView>(R.id.tripDescription)
        description.text = "In this Trip you will travel with a young driver which has" +
                " a good sense of humor. You have the possibility to take no more than 1 " +
                " trolley and 1 small bag because of the small space. See you soon."


    }
}

data class IntermediateStops(val location: String, val timestamp: Timestamp)


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


