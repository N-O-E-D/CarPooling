package it.polito.mad.group08.carpooling.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import it.polito.mad.group08.carpooling.R
import java.math.BigDecimal


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

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        val textView: TextView = root.findViewById(R.id.text_home)
        homeViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //val fab: FloatingActionButton? = view.findViewById(R.id.fab)
        val button = view.findViewById<Button>(R.id.button)
        button.setOnClickListener {
            //fab?.hide()
            val checkpoints = listOf(
                    CheckPoint("Via Roma 32, Torino", "2021-04-25\n08:00"),
                    CheckPoint("Via Milano 23, Firenze", "2021-04-25\n11:00"),
                    CheckPoint("Via Torino 44, Roma", "2021-04-25\n17:30"),
                    CheckPoint("Via Firenze 33, Napoli", "25/03/2021\n19:00")
            )

            val trip = Trip("carPhotoPath", "Toyota Le mans 3000 Diesel",
                    "Pino Guidatutto", 4.2f, checkpoints, "22h30m",
                    3, BigDecimal(35.50),
                    "In this Trip you will travel with a young driver which has" +
                            " a good sense of humor. You have the possibility to take no more than 1 " +
                            " trolley and 1 small bag because of the small space. See you soon.")

            val bundle = bundleOf("trip" to Gson().toJson(trip))
            Log.d("AAA", bundle.toString())
            setFragmentResult("KeyFragment", bundle)
            findNavController().navigate(R.id.action_nav_home_to_tripDetailsFragment)
        }
    }
}