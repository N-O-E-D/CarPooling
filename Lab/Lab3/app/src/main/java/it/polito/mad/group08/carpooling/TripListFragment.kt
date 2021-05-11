package it.polito.mad.group08.carpooling

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.opengl.Visibility
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.w3c.dom.Text
import java.io.FileNotFoundException
import java.lang.reflect.Type
import java.math.BigDecimal

const val EDIT_BUTTON_CLICKED = 1
const val CARD_CLICKED = 2
const val FAB_CLICKED = 3

class TripListFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TripAdapter
    private lateinit var emptyTextView: TextView

    private val model: SharedViewModel by activityViewModels()

    private fun navigationClickListener(mode: Int, trip: Trip?, position: Int?) {
        val navController = findNavController()
        if (mode == EDIT_BUTTON_CLICKED && position != null && trip != null) {
            model.setPosition(position)
            navController.navigate(R.id.action_tripListFragment_to_tripEditFragment)
            //Toast.makeText(context, "EDIT: From ${trip.departureLocation} to ${trip.arrivalLocation}!", Toast.LENGTH_SHORT).show()
        } else if (mode == CARD_CLICKED && trip != null) {
            model.setPosition(position!!)
            navController.navigate(R.id.action_tripListFragment_to_tripDetailsFragment, bundleOf("parent" to "TRIPS"))
            //Toast.makeText(context, "DETAILS: From ${trip.departureLocation} to ${trip.arrivalLocation}!", Toast.LENGTH_SHORT).show()
        } else if (mode == FAB_CLICKED) {
            model.setPosition(model.getTrips().value!!.size)
            navController.navigate(R.id.action_tripListFragment_to_tripEditFragment)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_trip_list, container, false)
        emptyTextView = view.findViewById(R.id.emptyTextView)
        recyclerView = view.findViewById(R.id.tripListRecyclerView)

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

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val addFab: FloatingActionButton = view.findViewById(R.id.add_fab)

        recyclerView.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
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
        model.getTrips().observe(viewLifecycleOwner, Observer<MutableList<Trip>>{ tripsDB ->
            // update UI
            if(tripsDB.isEmpty()){
                recyclerView.visibility = View.GONE
                emptyTextView.visibility = View.VISIBLE
            }
            else{
                recyclerView.visibility = View.VISIBLE
                emptyTextView.visibility = View.GONE
            }

            adapter = TripAdapter(tripsDB, model) { mode: Int, tripItem: Trip, position: Int? -> navigationClickListener(mode, tripItem, position) }
            recyclerView.adapter = adapter
        })
    }



    class TripAdapter(private val tripsAdapter: MutableList<Trip>,
                      private val model: SharedViewModel,
                      private val clickListener: (Int, Trip, Int?) -> Unit) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

        class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val departureLocation: TextView = itemView.findViewById(R.id.departureLocation)
            private val arrivalLocation: TextView = itemView.findViewById(R.id.arrivalLocation)
            private val departureTimestamp: TextView = itemView.findViewById(R.id.departureTimestamp)
            private val arrivalTimestamp: TextView = itemView.findViewById(R.id.arrivalTimestamp)
            private val editButton: Button = itemView.findViewById(R.id.editButton)
            private val card: CardView = itemView.findViewById(R.id.card)



            fun bind(trip: Trip, model: SharedViewModel, clickListener: (Int, Trip, Int?) -> Unit) {
                departureLocation.text = trip.checkPoints[0].location
                arrivalLocation.text = trip.checkPoints[trip.checkPoints.size - 1].location
                departureTimestamp.text = trip.checkPoints[0].timestamp
                arrivalTimestamp.text = trip.checkPoints[trip.checkPoints.size-1].timestamp
                //val bitmap: Bitmap? = takeSavedPhoto(trip.carPhotoPath, itemView)
                when(itemView.context.resources.configuration.orientation){
                    Configuration.ORIENTATION_PORTRAIT -> {
                        if( model.bitmaps[trip.id] == null) {
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
                editButton.setOnClickListener {
                    clickListener(EDIT_BUTTON_CLICKED, trip, bindingAdapterPosition)
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
            holder.bind(tripsAdapter[position], model, clickListener)
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