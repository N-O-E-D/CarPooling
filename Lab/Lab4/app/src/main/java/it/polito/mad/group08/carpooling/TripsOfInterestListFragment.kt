package it.polito.mad.group08.carpooling

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout

class TripsOfInterestListFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TripAdapter
    private lateinit var emptyTextView: TextView
    private lateinit var shimmerFrameLayout: ShimmerFrameLayout
    private lateinit var shimmerFrameLayout2: ShimmerFrameLayout
    private var first_time = true
    private var old_trip_list = mutableListOf<Trip>()

    private val model: SharedViewModel by activityViewModels()

    private fun navigationClickListener(mode: Int, trip: Trip?, position: Int?) {
        val navController = findNavController()
        if (mode == CARD_CLICKED && trip != null) {
            model.setPosition(position!!)
            navController.navigate(
                R.id.action_tripsOfInterestListFragment_to_tripDetailsFragment,
                bundleOf("parent" to INTERESTED_TRIPS_IS_PARENT)
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_trips_of_interest_list, container, false)
        emptyTextView = view.findViewById(R.id.emptyTextView)
        recyclerView = view.findViewById(R.id.interestedTripListRecyclerView)
        shimmerFrameLayout = view.findViewById(R.id.shimmer_view_container)
        first_time = true

        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                recyclerView.layoutManager = LinearLayoutManager(context)
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                shimmerFrameLayout2 = view.findViewById(R.id.shimmer_view_container2)
                recyclerView.layoutManager = GridLayoutManager(context, 2)
            }
            else -> recyclerView.layoutManager = LinearLayoutManager(context)
        }

        setHasOptionsMenu(true)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // DECOUPLE DATA FROM UI
        model.getMyInterestedTrips()
            .observe(viewLifecycleOwner, Observer<Resource<List<Trip>>> { resource ->
                // update UI
                when (resource) {
                    is Resource.Loading -> {
                        shimmerFrameLayout.startShimmer()
                        when (resources.configuration.orientation) {
                            Configuration.ORIENTATION_LANDSCAPE -> {
                                shimmerFrameLayout2.startShimmer()
                            }
                        }
                    }
                    is Resource.Success -> {

                        if (resource.data.isEmpty()) {
                            recyclerView.visibility = View.GONE
                            emptyTextView.visibility = View.VISIBLE
                            emptyTextView.text = getString(R.string.no_trips)
                        } else {
                            recyclerView.visibility = View.VISIBLE
                            emptyTextView.visibility = View.GONE
                        }

                        when (resources.configuration.orientation) {
                            Configuration.ORIENTATION_LANDSCAPE -> {
                                shimmerFrameLayout2.hideShimmer()
                                shimmerFrameLayout2.visibility = View.GONE
                            }
                        }

                        shimmerFrameLayout.hideShimmer()
                        shimmerFrameLayout.visibility = View.GONE

                        if (first_time) {
                            old_trip_list = resource.data.toMutableList()
                            adapter = TripAdapter(
                                resource.data.toMutableList(),
                                model,
                                OTHERS_TRIP_LIST_IS_PARENT
                            ) { mode: Int, tripItem: Trip, position: Int? ->
                                navigationClickListener(
                                    mode,
                                    tripItem,
                                    position
                                )
                            }
                            recyclerView.adapter = adapter
                            first_time = false
                        } else {
                            val new_trip_list = resource.data
                            if ((old_trip_list.size - 1) == new_trip_list.size) { // Trip Delete
                                var index = -1
                                for (i in 0 until old_trip_list.size - 1) {
                                    if (old_trip_list[i] != new_trip_list[i]) {
                                        index = i
                                        break
                                    } else if (i == old_trip_list.size - 2) {
                                        index = old_trip_list.size - 1
                                    }
                                }
                                old_trip_list = resource.data.toMutableList()
                                if(index == -1)
                                    index = 0
                                adapter.onItemDeleted(index)
                            } else if (old_trip_list.size == new_trip_list.size) { // Trip Edited
                                var index = -1
                                for (i in 0..old_trip_list.size) {
                                    if(old_trip_list[i] != new_trip_list[i]) {
                                        index = i
                                        break
                                    }
                                }
                                old_trip_list = resource.data.toMutableList()
                                adapter.onItemChange(new_trip_list[index], index)
                            } else if ((old_trip_list.size + 1) == new_trip_list.size) { // Trip Added
                                var index = -1
                                for (i in 0 until new_trip_list.size - 1) {
                                    if (old_trip_list[i] != new_trip_list[i]) {
                                        index = i
                                        break
                                    } else if (i == new_trip_list.size - 2) {
                                        index = new_trip_list.size - 1
                                    }
                                }
                                old_trip_list = resource.data.toMutableList()
                                adapter.onItemAdded(new_trip_list[index], index)
                            }
                        }

                    }
                    is Resource.Failure -> {

                        when (resources.configuration.orientation) {
                            Configuration.ORIENTATION_LANDSCAPE -> {
                                shimmerFrameLayout2.hideShimmer()
                                shimmerFrameLayout2.visibility = View.GONE
                            }
                        }
                        shimmerFrameLayout.hideShimmer()
                        shimmerFrameLayout.visibility = View.GONE
                        emptyTextView.visibility = View.VISIBLE
                        emptyTextView.text = getString(R.string.error_occur)
                    }
                }
            })
    }
}