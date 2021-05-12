package it.polito.mad.group08.carpooling

import android.app.DatePickerDialog
import android.content.Context
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.icu.text.NumberFormat
import android.icu.util.Currency
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.RangeSlider
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.text.SimpleDateFormat
import java.util.*


class OthersTripListFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TripAdapter
    private lateinit var emptyTextView: TextView
    private val model: SharedViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object: OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                activity!!.finish()
            }
        })
    }
    private fun navigationClickListener(mode: Int, trip: Trip?, position: Int?) {
        val navController = findNavController()
        if (mode == CARD_CLICKED && trip != null) {
            model.setPosition(position!!)
            navController.navigate(R.id.action_othersTripListFragment_to_tripDetailsFragment, bundleOf("parent" to "OTHERS_TRIPS"))
            //Toast.makeText(context, "DETAILS: From ${trip.departureLocation} to ${trip.arrivalLocation}!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_others_trip_list, container, false)
        emptyTextView = view.findViewById(R.id.emptyTextView)
        recyclerView = view.findViewById(R.id.othersTripListRecyclerView)

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
        model.getOthersTrips().observe(viewLifecycleOwner, Observer<MutableList<Trip>> { tripsDB ->
            // update UI

            Log.d("OTHERSAAAA", tripsDB.toString())
            if (tripsDB.isEmpty()) {
                recyclerView.visibility = View.GONE
                emptyTextView.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyTextView.visibility = View.GONE
            }

            adapter = TripAdapter(tripsDB, model) { mode: Int, tripItem: Trip, position: Int? -> navigationClickListener(mode, tripItem, position) }
            recyclerView.adapter = adapter
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

        if(filter.departureLocation != null)
            departureET?.setText(filter.departureLocation)

        if(filter.arrivalLocation != null)
            arrivalET?.setText(filter.arrivalLocation)

        if(filter.departureDate != null)
            dateDepartureET?.setText(filter.departureDate)

        if(filter.arrivalDate != null)
            dateArrivalET?.setText(filter.arrivalDate)

        rangeSlider?.values = listOf(filter.minPrice, filter.maxPrice)

        fun updateDateInView(timestamp: EditText, cal: Calendar) {
            val myFormat = "MM/dd/yyyy" // mention the format you need
            val sdf = SimpleDateFormat(myFormat, Locale.US)
            timestamp.setText(sdf.format(cal.time))
        }

        val dateDepartureSetListener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, monthOfYear)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView(dateDepartureET!!, cal)
        }

        val dateArrivalSetListener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, monthOfYear)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView(dateArrivalET!!, cal)
        }

        dateDepartureET?.setOnClickListener {
            DatePickerDialog(it.context,
                    dateDepartureSetListener,
                    // set DatePickerDialog to point to today's date when it loads up
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH))
                    .apply {
                        datePicker.minDate = System.currentTimeMillis() - 1000
                    }.show()
        }

        dateArrivalET?.setOnClickListener {
            DatePickerDialog(it.context,
                    dateArrivalSetListener,
                    // set DatePickerDialog to point to today's date when it loads up
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH))
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
        filter.departureLocation = if(departureET?.text.toString() == "") null else departureET?.text.toString()
        filter.arrivalLocation = if(arrivalET?.text.toString() == "") null else arrivalET?.text.toString()
        filter.departureDate = if(dateDepartureET?.text.toString() == "") null else dateDepartureET?.text.toString()
        filter.arrivalDate = if(dateArrivalET?.text.toString() == "") null else dateArrivalET?.text.toString()
        filter.minPrice = rangeSlider?.values?.first()!!
        filter.maxPrice = rangeSlider.values.last()!!

        model.setFilter(filter)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        //TODO filter icon
        inflater.inflate(R.menu.search_menu, menu)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        val inflater = context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_filter, null)
        return when (item.itemId) {
            R.id.searchButton -> {
                val dialogFilter = MaterialAlertDialogBuilder(requireContext())
                        .setView(dialogView)
                        .setNegativeButton(getString(R.string.reset_filters)){dialog, which ->
                            model.setFilter(Filter())
                        }
                        .setNeutralButton(getString(R.string.cancel_filters)) { dialog, which ->
                            // Respond to neutral button press
                        }
                        .setPositiveButton(getString(R.string.apply_filters)) { dialog, which ->
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
        if(activity?.isChangingConfigurations == false){
            model.setFilter(Filter())
        }
    }
}

class TripAdapter(private val tripsAdapter: MutableList<Trip>,
                  private val model: SharedViewModel,
                  private val clickListener: (Int, Trip, Int?) -> Unit
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val departureLocation: TextView = itemView.findViewById(R.id.departureLocation)
        private val arrivalLocation: TextView = itemView.findViewById(R.id.arrivalLocation)
        private val departureTimestamp: TextView = itemView.findViewById(R.id.departureTimestamp)
        private val arrivalTimestamp: TextView = itemView.findViewById(R.id.arrivalTimestamp)
        private val showInterestButton: Button = itemView.findViewById(R.id.editButton)
        private val card: CardView = itemView.findViewById(R.id.card)


        fun bind(trip: Trip, model: SharedViewModel, clickListener: (Int, Trip, Int?) -> Unit) {
            departureLocation.text = trip.checkPoints[0].location
            arrivalLocation.text = trip.checkPoints[trip.checkPoints.size - 1].location
            departureTimestamp.text = trip.checkPoints[0].timestamp
            arrivalTimestamp.text = trip.checkPoints[trip.checkPoints.size - 1].timestamp
            showInterestButton.text = itemView.context.getString(R.string.trip_show_interest)
            when(itemView.context.resources.configuration.orientation){
                Configuration.ORIENTATION_PORTRAIT -> {
                    if (model.bitmaps[trip.id] == null) {
                        val storage = Firebase.storage
                        val storageRef = storage.reference
                        if(trip.carPhotoPath != null) {
                            val testRef = storageRef.child(trip.carPhotoPath!!)
                            testRef.metadata.addOnSuccessListener { metadata ->
                                val size = metadata.sizeBytes
                                val ONE_MEGABYTE: Long = 1024 * 1024
                                testRef.getBytes(ONE_MEGABYTE).addOnSuccessListener {
                                    val imageBitmap = BitmapFactory.decodeByteArray(it, 0, size.toInt())
                                    if (imageBitmap != null){
                                        itemView.findViewById<ImageView>(R.id.carPhoto)
                                                .setImageBitmap(imageBitmap)
                                        model.bitmaps[trip.id] = imageBitmap
                                    }
                                }.addOnFailureListener {
                                    // Handle any errors
                                }
                            }.addOnFailureListener {
                                // Uh-oh, an error occurred!
                            }
                        }
                    } else {
                        itemView.findViewById<ImageView>(R.id.carPhoto)
                                .setImageBitmap(model.bitmaps[trip.id])
                    }
                }
            }


            card.setOnClickListener {
                clickListener(CARD_CLICKED, trip, bindingAdapterPosition)
            }

            //SHOW INTEREST BUTTON
            if(model.bookingIsAccepted(trip.id)){ //user already show favorite and owner accepted
                //TODO TEST IT
                showInterestButton.text = itemView.context.getString(R.string.trip_already_booked)
                showInterestButton.setOnClickListener { null }
            }else{
                if(model.userIsInterested(trip)){
                    showInterestButton.text = itemView.context.getString(R.string.trip_remove_interest)
                }else{
                    showInterestButton.text = itemView.context.getString(R.string.trip_show_interest)
                }

                showInterestButton.setOnClickListener {
                    if(model.userIsInterested(trip)){ // Already interested, but she would to cancel
                        model.updateTripInterestedUser(trip, false, null)
                        showInterestButton.text = itemView.context.getString(R.string.trip_show_interest)
                    }
                    else {
                        model.updateTripInterestedUser(trip, true, null)
                        showInterestButton.text = itemView.context.getString(R.string.trip_remove_interest)
                    }
                }
            }
        }

        fun unbind() {
            card.setOnClickListener { null }
            showInterestButton.setOnClickListener { null }
        }

//        private fun takeSavedPhoto(name: String?, v: View): Bitmap? {
//            var imageBitmap: Bitmap? = null
//            try {
//                if (name != null) {
//                    v.context.applicationContext?.openFileInput(name).use {
//                        imageBitmap = BitmapFactory.decodeStream(it)
//                    }
//                }
//            } catch (e: FileNotFoundException) {
//                e.printStackTrace()
//            }
//            return imageBitmap
//        }
    }

    override fun getItemCount() = tripsAdapter.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.trip_item_layout, parent, false)
        return TripViewHolder(v)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(tripsAdapter[position], model, clickListener)
    }

    override fun onViewRecycled(holder: TripViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

//    fun onItemChange(tripEdited: Trip, position: Int) {
//        tripsAdapter[position] = tripEdited
//        notifyItemChanged(position)
//    }
//
//    fun onItemAdded(tripAdded: Trip) {
//        tripsAdapter.add(tripAdded)
//        notifyItemInserted(tripsAdapter.size - 1)
//    }
}

