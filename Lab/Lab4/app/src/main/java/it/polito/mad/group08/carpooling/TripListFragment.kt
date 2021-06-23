package it.polito.mad.group08.carpooling

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton

const val CARD_BUTTON_CLICKED = 1 //Edit in MyTrips; ShowInterest in OtherTripList
const val CARD_CLICKED = 2
const val FAB_CLICKED = 3

class TripListFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TripAdapter
    private lateinit var emptyTextView: TextView
    private lateinit var addFab: FloatingActionButton
    private lateinit var shimmerFrameLayout: ShimmerFrameLayout
    private lateinit var shimmerFrameLayout2: ShimmerFrameLayout
    private var first_time = true
    private var old_trip_list = mutableListOf<Trip>()

    private val model: SharedViewModel by activityViewModels()

    private fun navigationClickListener(mode: Int, trip: Trip?, position: Int?) {
        val navController = findNavController()
        if (mode == CARD_BUTTON_CLICKED && position != null && trip != null) {
            model.setPosition(position)
            navController.navigate(R.id.action_tripListFragment_to_tripEditFragment)
        } else if (mode == CARD_CLICKED && trip != null) {
            model.setPosition(position!!)
            navController.navigate(
                R.id.action_tripListFragment_to_tripDetailsFragment,
                bundleOf("parent" to MY_TRIP_LIST_IS_PARENT)
            )
        } else if (mode == FAB_CLICKED) {
            model.getMyTrips()
                .observe(viewLifecycleOwner, { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            model.setPosition(resource.data.size)
                            navController.navigate(R.id.action_tripListFragment_to_tripEditFragment)
                        }
                        else -> {
                            Toast.makeText(context, "Please wait...", Toast.LENGTH_SHORT).show()
                        }
                    }
                })
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_trip_list, container, false)
        emptyTextView = view.findViewById(R.id.emptyTextView)
        recyclerView = view.findViewById(R.id.tripListRecyclerView)
        addFab = view.findViewById(R.id.add_fab)
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
        }

        return view
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (scrollY > oldScrollY && addFab.visibility == View.VISIBLE) {
                addFab.hide()
            } else if (scrollY < oldScrollY && addFab.visibility != View.VISIBLE) {
                addFab.show()
            }
        }

        addFab.setOnClickListener {
            val anim: Animation = AnimationUtils.loadAnimation(addFab.context, R.anim.zoom)
            anim.duration = 150
            addFab.startAnimation(anim)
            navigationClickListener(FAB_CLICKED, null, null)
        }

        // DECOUPLE DATA FROM UI
        model.getMyTrips()
            .observe(viewLifecycleOwner, { resource ->
                // update UI
                when (resource) {
                    is Resource.Loading -> {
                        shimmerFrameLayout.startShimmer()
                        addFab.visibility = View.GONE
                        when (resources.configuration.orientation) {
                            Configuration.ORIENTATION_LANDSCAPE -> {
                                shimmerFrameLayout2.startShimmer()
                            }
                        }
                    }
                    is Resource.Success -> {
                        addFab.visibility = View.VISIBLE

                        if (resource.data.isEmpty()) {
                            recyclerView.visibility = View.GONE
                            emptyTextView.visibility = View.VISIBLE
                            emptyTextView.text = getString(R.string.no_trips)
                        } else {
                            recyclerView.visibility = View.VISIBLE
                            emptyTextView.visibility = View.GONE
                        }

                        shimmerFrameLayout.hideShimmer()
                        shimmerFrameLayout.visibility = View.GONE

                        when (resources.configuration.orientation) {
                            Configuration.ORIENTATION_LANDSCAPE -> {
                                shimmerFrameLayout2.hideShimmer()
                                shimmerFrameLayout2.visibility = View.GONE
                            }
                        }

                        if (first_time) {
                            old_trip_list = resource.data.toMutableList()
                            adapter = TripAdapter(
                                resource.data.toMutableList(),
                                model,
                                MY_TRIP_LIST_IS_PARENT
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
                                for (i in 0 until new_trip_list.size - 1) {
                                    if(old_trip_list[i] != new_trip_list[i]) {
                                        index = i
                                        break
                                    }
                                }
                                old_trip_list = resource.data.toMutableList()
                                if(index == -1)
                                    index = 0
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
                                if(index == -1)
                                    index = 0
                                adapter.onItemAdded(new_trip_list[index], index)
                            }
                        }

                    }
                    is Resource.Failure -> {
                        shimmerFrameLayout.hideShimmer()
                        shimmerFrameLayout.visibility = View.GONE
                        when (resources.configuration.orientation) {
                            Configuration.ORIENTATION_LANDSCAPE -> {
                                shimmerFrameLayout2.hideShimmer()
                                shimmerFrameLayout2.visibility = View.GONE
                            }
                        }
                        emptyTextView.visibility = View.VISIBLE
                        emptyTextView.text = getString(R.string.error_occur)
                    }
                }
            })
    }
}