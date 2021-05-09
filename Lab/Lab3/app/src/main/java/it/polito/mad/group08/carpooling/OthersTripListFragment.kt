package it.polito.mad.group08.carpooling

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.FileNotFoundException


class OthersTripListFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TripAdapter
    private lateinit var emptyTextView: TextView

    private val model: SharedViewModel by activityViewModels()

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

        when(resources.configuration.orientation){
            Configuration.ORIENTATION_PORTRAIT -> {
                recyclerView.layoutManager = LinearLayoutManager(context)
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                recyclerView.layoutManager = GridLayoutManager(context, 3)
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // DECOUPLE DATA FROM UI
        model.getOthersTrips().observe(viewLifecycleOwner, Observer<MutableList<Trip>>{ tripsDB ->
            // update UI

            Log.d("OTHERSAAAA", tripsDB.toString())
            if(tripsDB.isEmpty()){
                recyclerView.visibility = View.GONE
                emptyTextView.visibility = View.VISIBLE
            }
            else{
                recyclerView.visibility = View.VISIBLE
                emptyTextView.visibility = View.GONE
            }

            adapter = TripAdapter(tripsDB) { mode: Int, tripItem: Trip, position: Int? -> navigationClickListener(mode, tripItem, position) }
            recyclerView.adapter = adapter
        })
    }



    class TripAdapter(private val tripsAdapter: MutableList<Trip>, private val clickListener: (Int, Trip, Int?) -> Unit) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

        class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val departureLocation: TextView = itemView.findViewById(R.id.departureLocation)
            private val arrivalLocation: TextView = itemView.findViewById(R.id.arrivalLocation)
            private val departureTimestamp: TextView = itemView.findViewById(R.id.departureTimestamp)
            private val arrivalTimestamp: TextView = itemView.findViewById(R.id.arrivalTimestamp)
            private val editButton: Button = itemView.findViewById(R.id.editButton)
            private val card: CardView = itemView.findViewById(R.id.card)

            fun bind(trip: Trip, clickListener: (Int, Trip, Int?) -> Unit) {
                departureLocation.text = trip.checkPoints[0].location
                arrivalLocation.text = trip.checkPoints[trip.checkPoints.size - 1].location
                departureTimestamp.text = trip.checkPoints[0].timestamp
                arrivalTimestamp.text = trip.checkPoints[trip.checkPoints.size-1].timestamp
                editButton.text = itemView.context.getString(R.string.trip_show_interest)
                val bitmap: Bitmap? = takeSavedPhoto(trip.carPhotoPath, itemView)
                when(itemView.context.resources.configuration.orientation){
                    Configuration.ORIENTATION_PORTRAIT -> {
                        if(bitmap != null){
                            itemView.findViewById<ImageView>(R.id.carPhoto).setImageBitmap(bitmap)
                        }
                    }
                }


                card.setOnClickListener {
                    clickListener(CARD_CLICKED, trip, bindingAdapterPosition)
                }
                editButton.setOnClickListener {
                    //TODO
                    //clickListener(EDIT_BUTTON_CLICKED, trip, bindingAdapterPosition)
                }
            }

            fun unbind() {
                card.setOnClickListener { null }
                editButton.setOnClickListener { null }
            }

            private fun takeSavedPhoto(name: String?, v: View): Bitmap? {
                var imageBitmap: Bitmap? = null
                try {
                    if(name != null) {
                        v.context.applicationContext?.openFileInput(name).use {
                            imageBitmap = BitmapFactory.decodeStream(it)
                        }
                    }
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                }
                return imageBitmap
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