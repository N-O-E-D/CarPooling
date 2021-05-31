package it.polito.mad.group08.carpooling

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BoughtTripsListFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TripAdapter
    private lateinit var emptyTextView: TextView
    private lateinit var boughtTripsProgressBar: ProgressBar

    private val model: SharedViewModel by activityViewModels()

    private fun navigationClickListener(mode: Int, trip: Trip?, position: Int?) {
        val navController = findNavController()
        if (mode == CARD_CLICKED && trip != null) {
            model.setPosition(position!!)
            navController.navigate(
                R.id.action_boughtTripsListFragment_to_tripDetailsFragment,
                bundleOf("parent" to BOOKED_TRIPS_IS_PARENT)
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
        boughtTripsProgressBar = view.findViewById(R.id.boughtTripsProgressBar)

        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                recyclerView.layoutManager = LinearLayoutManager(context)
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                recyclerView.layoutManager = GridLayoutManager(context, 3)
            }
            else -> recyclerView.layoutManager = LinearLayoutManager(context)
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
                        boughtTripsProgressBar.visibility = View.VISIBLE
                    }
                    is Resource.Success -> {
                        boughtTripsProgressBar.visibility = View.GONE

                        if (resource.data.isEmpty()) {
                            recyclerView.visibility = View.GONE
                            emptyTextView.visibility = View.VISIBLE
                        } else {
                            recyclerView.visibility = View.VISIBLE
                            emptyTextView.visibility = View.GONE
                        }

                        adapter = TripAdapter( resource.data, model, OTHER_TRIP_LIST_IS_PARENT) {
                                mode: Int, tripItem: Trip, position: Int? ->
                                    navigationClickListener(
                                        mode,
                                        tripItem,
                                        position
                                    )
                                }
                        recyclerView.adapter = adapter
                    }
                    is Resource.Failure -> {
                        boughtTripsProgressBar.visibility = View.GONE
                        emptyTextView.text = getString(R.string.error_occur)
                    }
                }
            })
    }

}