package it.polito.mad.group08.carpooling

import android.app.DatePickerDialog
import android.content.Context
import android.content.res.Configuration
import android.icu.text.NumberFormat
import android.icu.util.Currency
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.RangeSlider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class OthersTripListFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TripAdapter
    private lateinit var emptyTextView: TextView
    private lateinit var shimmerFrameLayout: ShimmerFrameLayout
    private lateinit var shimmerFrameLayout2: ShimmerFrameLayout
    private var first_time = true
    private var old_trip_list = mutableListOf<Trip>()

    private val model: SharedViewModel by activityViewModels()

    override fun onResume() {
        super.onResume()
        shimmerFrameLayout.startShimmer()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                activity!!.finish()
            }
        })
    }

    private fun navigationClickListener(mode: Int, trip: Trip?, position: Int?) {
        val navController = findNavController()
        if (mode == CARD_CLICKED && trip != null) {
            model.setPosition(position!!)
            navController.navigate(
                R.id.action_othersTripListFragment_to_tripDetailsFragment,
                bundleOf("parent" to OTHERS_TRIP_LIST_IS_PARENT)
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_others_trip_list, container, false)
        emptyTextView = view.findViewById(R.id.emptyTextView)
        recyclerView = view.findViewById(R.id.othersTripListRecyclerView)
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

        setHasOptionsMenu(true)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // DECOUPLE DATA FROM UI
        model.getOthersTrips()
            .observe(viewLifecycleOwner, { resource ->
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
                                for (i in new_trip_list.indices) {
                                    if(old_trip_list[i] != new_trip_list[i]) {
                                        index = i
                                        break
                                    }
                                }
                                old_trip_list = resource.data.toMutableList()
                                if (index == -1)
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

                                if(index == -1)
                                    index = new_trip_list.size - 1
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
                        shimmerFrameLayout.stopShimmer()
                        shimmerFrameLayout.visibility = View.GONE
                        emptyTextView.visibility = View.VISIBLE
                        emptyTextView.text = getString(R.string.error_occur)
                    }
                }
            })
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun initDialog(dialogFilter: androidx.appcompat.app.AlertDialog) {
        val rangeSlider: RangeSlider? = dialogFilter.findViewById(R.id.rangeSlider)
        val departureET: EditText? = dialogFilter.findViewById(R.id.departureET)
        val arrivalET: EditText? = dialogFilter.findViewById(R.id.arrivalET)
        val dateDepartureET: EditText? = dialogFilter.findViewById(R.id.dateDepartureET)
        val dateArrivalET: EditText? = dialogFilter.findViewById(R.id.dateArrivalET)
        val cal = Calendar.getInstance()

        val filter: Filter = model.getFilter()!!

        if (filter.departureLocation != null)
            departureET?.setText(filter.departureLocation)

        if (filter.arrivalLocation != null)
            arrivalET?.setText(filter.arrivalLocation)

        if (filter.departureDate != null)
            dateDepartureET?.setText(filter.departureDate)

        if (filter.arrivalDate != null)
            dateArrivalET?.setText(filter.arrivalDate)

        rangeSlider?.values = listOf(filter.minPrice, filter.maxPrice)

        fun updateDateInView(timestamp: EditText, cal: Calendar) {
            val myFormat = "MM/dd/yyyy" // mention the format you need
            val sdf = SimpleDateFormat(myFormat, Locale.US)
            timestamp.setText(sdf.format(cal.time))
        }

        val dateDepartureSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateInView(dateDepartureET!!, cal)
            }

        val dateArrivalSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateInView(dateArrivalET!!, cal)
            }

        dateDepartureET?.setOnClickListener {
            DatePickerDialog(
                it.context,
                dateDepartureSetListener,
                // set DatePickerDialog to point to today's date when it loads up
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
                .apply {
                    datePicker.minDate = System.currentTimeMillis() - 1000
                }.show()
        }

        dateArrivalET?.setOnClickListener {
            DatePickerDialog(
                it.context,
                dateArrivalSetListener,
                // set DatePickerDialog to point to today's date when it loads up
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
                .apply {
                    datePicker.minDate = System.currentTimeMillis() - 1000
                }.show()
        }

        rangeSlider?.setLabelFormatter { value: Float ->
            val format = NumberFormat.getCurrencyInstance()
            format.maximumFractionDigits = 0
            format.currency = Currency.getInstance("USD")
            format.format(value.toDouble())
        }
    }

    private fun filterListener(dialogView: View) {
        val rangeSlider: RangeSlider? = dialogView.findViewById(R.id.rangeSlider)
        val departureET: EditText? = dialogView.findViewById(R.id.departureET)
        val arrivalET: EditText? = dialogView.findViewById(R.id.arrivalET)
        val dateDepartureET: EditText? = dialogView.findViewById(R.id.dateDepartureET)
        val dateArrivalET: EditText? = dialogView.findViewById(R.id.dateArrivalET)

        val filter = Filter()
        filter.departureLocation =
            if (departureET?.text.toString() == "") null else departureET?.text.toString()
        filter.arrivalLocation =
            if (arrivalET?.text.toString() == "") null else arrivalET?.text.toString()
        filter.departureDate =
            if (dateDepartureET?.text.toString() == "") null else dateDepartureET?.text.toString()
        filter.arrivalDate =
            if (dateArrivalET?.text.toString() == "") null else dateArrivalET?.text.toString()
        filter.minPrice = rangeSlider?.values?.first()!!
        filter.maxPrice = rangeSlider.values.last()!!

        model.setFilter(filter)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.search_menu, menu)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        val inflater = context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_filter, null)
        return when (item.itemId) {
            R.id.searchButton -> {
                first_time = true
                val dialogFilter = MaterialAlertDialogBuilder(requireContext())
                    .setView(dialogView)
                    .setNegativeButton(getString(R.string.reset_filters)) { _, _ ->
                        model.setFilter(Filter())
                    }
                    .setNeutralButton(getString(R.string.cancel_filters)) { _, _ ->
                        // Respond to neutral button press
                    }
                    .setPositiveButton(getString(R.string.apply_filters)) { _, _ ->
                        filterListener(dialogView)
                    }
                    .show()
                initDialog(dialogFilter)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (activity?.isChangingConfigurations == false) {
            model.setFilter(Filter())
        }
    }
}

class TripAdapter(
    private val tripsAdapter: MutableList<Trip>,
    private val model: SharedViewModel,
    private val parent: String,
    private val clickListener: (Int, Trip, Int?) -> Unit
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val departureLocation: TextView = itemView.findViewById(R.id.departureLocation)
        private val arrivalLocation: TextView = itemView.findViewById(R.id.arrivalLocation)
        private val departureTimestamp: TextView = itemView.findViewById(R.id.departureTimestamp)
        private val arrivalTimestamp: TextView = itemView.findViewById(R.id.arrivalTimestamp)
        private val cardButton: Button = itemView.findViewById(R.id.editButton)
        private val card: CardView = itemView.findViewById(R.id.card)

        fun bind(
            trip: Trip,
            model: SharedViewModel,
            parent: String,
            clickListener: (Int, Trip, Int?) -> Unit
        ) {
            departureLocation.text = trip.checkPoints[0].location
            arrivalLocation.text = trip.checkPoints[trip.checkPoints.size - 1].location
            departureTimestamp.text = trip.checkPoints[0].timestamp
            arrivalTimestamp.text = trip.checkPoints[trip.checkPoints.size - 1].timestamp

            if (parent == OTHERS_TRIP_LIST_IS_PARENT) // Also INTERESTED and BOUGHT
                cardButton.text = itemView.context.getString(R.string.trip_show_interest)
            else
                cardButton.text = itemView.context.getString(R.string.edit)

            if (model.bitmaps[trip.id] == null) {
                if (trip.carPhotoPath != null && trip.carPhotoPath != "") {

                    itemView.findViewById<ShimmerFrameLayout>(R.id.shimmer).startShimmer()


                    MainScope().launch {
                        val size = withContext(Dispatchers.IO) {
                            model.downloadMetadataPhoto(trip.carPhotoPath!!).sizeBytes
                        }
                        val bitmap = withContext(Dispatchers.IO) {
                            model.downloadPhoto(size, trip.carPhotoPath!!)
                        }

                        model.bitmaps[trip.id] = bitmap

                        itemView.findViewById<ShimmerFrameLayout>(R.id.shimmer).hideShimmer()
                        itemView.findViewById<ImageView>(R.id.carPhoto)
                            .setImageBitmap(model.bitmaps[trip.id])

                    }
                } else {
                    itemView.findViewById<ShimmerFrameLayout>(R.id.shimmer).hideShimmer()
                }
            } else {
                itemView.findViewById<ShimmerFrameLayout>(R.id.shimmer).hideShimmer()
                itemView.findViewById<ImageView>(R.id.carPhoto)
                    .setImageBitmap(model.bitmaps[trip.id])
            }

            card.setOnClickListener {
                clickListener(CARD_CLICKED, trip, bindingAdapterPosition)
            }

            if (parent == MY_TRIP_LIST_IS_PARENT) { //EDIT BUTTON
                cardButton.setOnClickListener {
                    clickListener(CARD_BUTTON_CLICKED, trip, bindingAdapterPosition)
                }
            } else { //SHOW INTEREST BUTTON
                if (model.userIsAccepted(trip)) { //user already show favorite and owner accepted
                    cardButton.text = itemView.context.getString(R.string.trip_already_booked)
                    cardButton.isClickable = false
                } else {
                    if (model.userIsInterested(trip)) {
                        cardButton.text =
                            itemView.context.getString(R.string.trip_remove_interest)
                    } else {
                        cardButton.text =
                            itemView.context.getString(R.string.trip_show_interest)
                    }

                    cardButton.setOnClickListener {
                        if (model.userIsInterested(trip)) { // Already interested, but she would to cancel
                            model.updateTripInterestedUser(trip, false, null)
                            cardButton.text =
                                itemView.context.getString(R.string.trip_show_interest)
                        } else {
                            model.updateTripInterestedUser(trip, true, null)
                            cardButton.text =
                                itemView.context.getString(R.string.trip_remove_interest)
                        }
                    }

                    if (trip.availableSeats == 0) {
                        cardButton.text = itemView.context.getString(R.string.trip_no_seats)
                        cardButton.isClickable = false
                    }
                }
            }
        }

        fun unbind() {
            card.setOnClickListener { null }
            cardButton.setOnClickListener { null }
        }
    }

    override fun getItemCount() = tripsAdapter.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val v =
            LayoutInflater.from(parent.context).inflate(R.layout.trip_item_layout, parent, false)
        return TripViewHolder(v)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(tripsAdapter[position], model, parent, clickListener)
    }

    override fun onViewRecycled(holder: TripViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    fun onItemChange(tripEdited: Trip, position: Int) {
        tripsAdapter[position] = tripEdited
        notifyItemChanged(position)
    }

    fun onItemDeleted(position: Int) {
        tripsAdapter.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, tripsAdapter.size - 1)
    }

    fun onItemAdded(tripAdded: Trip, position: Int) {
        tripsAdapter.add(position, tripAdded)
        notifyItemInserted(position)
    }

}

