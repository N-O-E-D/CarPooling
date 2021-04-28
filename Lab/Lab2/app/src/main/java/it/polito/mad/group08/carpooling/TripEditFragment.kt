package it.polito.mad.group08.carpooling

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.jvm.Throws

class TripEditFragment : Fragment() {


    lateinit var carNameET: EditText
    lateinit var driverNameET: EditText
    lateinit var seatPriceET: EditText
    lateinit var availableSeatsET: EditText
    lateinit var informationsET: EditText
    lateinit var imageView: ImageView
    lateinit var ratingBar: RatingBar
    lateinit var recyclerView: RecyclerView
    lateinit var adapter: ItemEditAdapter
    var filename: String? = null
    lateinit var trip: TripListFragment.Trip
    lateinit var button_stop: Button
    var position: Int = -1
    private lateinit var pickImageContract: ActivityResultContract<Uri, Uri?>
    private lateinit var pickImageCallback: ActivityResultCallback<Uri?>
    private lateinit var pickImageLauncher: ActivityResultLauncher<Uri>
    private lateinit var takeImageContract: ActivityResultContract<Any, Any?>
    private lateinit var takeImageCallback: ActivityResultCallback<Any?>
    private lateinit var takeImageLauncher: ActivityResultLauncher<Any>
    lateinit var currentPhotoPath: String

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pickImageContract = object : ActivityResultContract<Uri, Uri?>() {
            override fun createIntent(context: Context, input: Uri): Intent {
                return Intent(Intent.ACTION_PICK, input)
            }
            override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
                if(resultCode != Activity.RESULT_OK || intent == null)
                    return null
                return intent.data
            }
        }

        pickImageCallback = ActivityResultCallback { imageUri: Uri? ->
            if(imageUri != null) {
                val bitmap = when {
                    Build.VERSION.SDK_INT < 28 -> MediaStore.Images.Media.getBitmap(
                        activity?.contentResolver,
                        imageUri
                    )
                    else -> {
                        val source = ImageDecoder.createSource(activity?.contentResolver!!, imageUri!!)
                        ImageDecoder.decodeBitmap(source)
                    }
                }

                imageView.setImageBitmap(bitmap)

                filename = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

                activity?.applicationContext?.openFileOutput(filename, Context.MODE_PRIVATE).use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }

        pickImageLauncher = registerForActivityResult(pickImageContract, pickImageCallback)

        takeImageContract = object : ActivityResultContract<Any, Any?>() {
            override fun createIntent(context: Context, input: Any): Intent {
                val photoFile: File? = try {
                    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                    val storageDir: File = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
                    File.createTempFile(
                            "JPEG_${timeStamp}_", /* prefix */
                            ".jpg", /* suffix */
                            storageDir /* directory */
                    ).apply {
                        // Save a file: path for use with ACTION_VIEW intents
                        currentPhotoPath = absolutePath
                    }
                } catch (ex: IOException) {
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                            context,
                            "it.polito.mad.group08.carpooling",
                            it
                    )
                    return Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                }
                return Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            }
            override fun parseResult(resultCode: Int, intent: Intent?): Bitmap? {
                if(resultCode != Activity.RESULT_OK || intent == null)
                    return null
                return intent.getParcelableExtra("data")
            }


        }

        takeImageCallback = ActivityResultCallback {  noop ->
            val bitmap = BitmapFactory.decodeFile(currentPhotoPath)
            imageView.setImageBitmap(bitmap)

            filename = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

            activity?.applicationContext?.openFileOutput(filename, Context.MODE_PRIVATE).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }

        takeImageLauncher = registerForActivityResult(takeImageContract, takeImageCallback)

        setFragmentResultListener("fromDetailsToEdit") { requestKey, bundle ->
            if (requestKey == "fromDetailsToEdit") {
                val tripJSON = bundle.getString("trip")
                position = bundle.getInt("pos")
                val type: Type = object : TypeToken<TripListFragment.Trip?>() {}.type
                trip = GsonBuilder().create().fromJson(tripJSON, type)
                filename = trip.carPhotoPath
                carNameET.setText(trip.carDescription)
                driverNameET.setText(trip.driverName)
                seatPriceET.setText(trip.seatPrice.toString())
                availableSeatsET.setText(trip.availableSeats.toString())
                informationsET.setText(trip.description)
                ratingBar.rating = trip.driverRate
                adapter = ItemEditAdapter(trip.checkPoints){position -> removeAt(position)}
                recyclerView.adapter = adapter
                takeSavedPhoto(filename, imageView)
                //estimatedDuration.text = "Durata stimata: " + calcDuration(trip.checkPoints[0], trip.checkPoints[trip.checkPoints.size - 1])
            }
        }

        setFragmentResultListener("tripEdit") { requestKey, bundle ->
            if (requestKey == "tripEdit") {
                val tripJSON = bundle.getString("trip")
                val type: Type = object : TypeToken<TripListFragment.Trip?>() {}.type
                trip = GsonBuilder().create().fromJson(tripJSON, type)
                filename = trip.carPhotoPath
                carNameET.setText(trip.carDescription)
                driverNameET.setText(trip.driverName)
                seatPriceET.setText(trip.seatPrice.toString())
                availableSeatsET.setText(trip.availableSeats.toString())
                informationsET.setText(trip.description)
                ratingBar.rating = trip.driverRate
                adapter = ItemEditAdapter(trip.checkPoints){position -> removeAt(position)}
                recyclerView.adapter = adapter
                position = bundle.getInt("pos")
                takeSavedPhoto(filename, imageView)
            }
        }

        setFragmentResultListener("tripAdd") { requestKey, bundle ->
            if (requestKey == "tripAdd") {
                trip = TripListFragment.Trip(null, "", "", 4.5f,
                        mutableListOf(), "", 0, 0.toBigDecimal(), "")
                adapter = ItemEditAdapter(trip.checkPoints){position -> removeAt(position)}
                recyclerView.adapter = adapter
            }
        }
    }

    fun removeAt(position: Int) {
        trip.checkPoints.removeAt(position)
        adapter.onItemEditDeleted(position)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if(view != null) {
            outState.putString("tripSaved", Gson().toJson(trip))
            outState.putInt("pos", position)
            outState.putString("filename", filename)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if(savedInstanceState!=null){
            val type: Type = object : TypeToken<TripListFragment.Trip?>() {}.type
            trip = GsonBuilder().create().fromJson(savedInstanceState.getString("tripSaved"), type)
            filename = savedInstanceState.getString("filename")
            takeSavedPhoto(filename, imageView)
            adapter = ItemEditAdapter(trip.checkPoints){position -> removeAt(position)}
            recyclerView.adapter = adapter
            position = savedInstanceState.getInt("pos")
            filename = savedInstanceState.getString("filename")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_trip_edit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val button: ImageButton = view.findViewById(R.id.imageButton)
        button.setOnClickListener {
            registerForContextMenu(it);
            activity?.openContextMenu(it);
            unregisterForContextMenu(it);}

        carNameET = view.findViewById(R.id.carNameET)
        driverNameET = view.findViewById(R.id.driverNameET)
        seatPriceET = view.findViewById(R.id.seatPriceET)
        availableSeatsET = view.findViewById(R.id.availableSeatsET)
        informationsET = view.findViewById(R.id.tripDescriptionET)
        imageView = view.findViewById(R.id.carPhoto)
        ratingBar = view.findViewById(R.id.driverRate)
        button_stop = view.findViewById(R.id.new_stop)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        button_stop.setOnClickListener{
            trip.checkPoints.add(TripListFragment.CheckPoint("", ""))
            adapter.onItemEditAdded()
        }
    }

    /*fun calcDuration(dep: TripListFragment.CheckPoint, arr: TripListFragment.CheckPoint): String {
        val dep_ts = dep.timestamp
        val arr_ts = arr.timestamp
        val format = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US)
        val date_dep = format.parse(dep_ts)
        val date_arr = format.parse(arr_ts)

        val diff: Long = date_arr.getTime() - date_dep.getTime()
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        println("" + days + " " + hours + " " + minutes)
        return concatenate(days.toInt(), hours.toInt(), minutes.toInt())
    }

    fun concatenate(days: Int, hours: Int, minutes: Int): String {
        var final_string = ""
        if(days != 0) {
            final_string = final_string + "${days} g"
        }
        if(hours != 0) {
            val new_hours = hours - days*24
            if (new_hours != 0)
                final_string = final_string + " ${new_hours} h"
        }
        if(minutes != 0) {
            val new_minutes = minutes - hours*60
            if(new_minutes != 0)
                final_string = final_string + " ${new_minutes} m"
        }

        return final_string
    }*/

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        activity?.menuInflater?.inflate(R.menu.floating_menu, menu)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.save_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun checkCheckpoint(): Boolean {
        for (checkpoint in trip.checkPoints)
            if(checkpoint.location == "" || checkpoint.timestamp == "")
                return true
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            /*16908332 -> {
                Toast.makeText(activity?.applicationContext,"Back Premuto", Toast.LENGTH_SHORT).show()
                return true
            }*/
            R.id.saveButton -> {
                if(carNameET.text.toString() == "" || driverNameET.text.toString() == "" ||
                        availableSeatsET.text.toString() == "" || seatPriceET.text.toString() == "" || checkCheckpoint()) {

                    Toast.makeText(activity?.applicationContext, "Please fill all the fields before saving!", Toast.LENGTH_SHORT).show()
                    return true
                }

                if (trip.checkPoints.size < 2) {
                    Toast.makeText(activity?.applicationContext, "Please insert at least departure and arrival!", Toast.LENGTH_SHORT).show()
                    return true
                }
                trip.carPhotoPath = filename
                trip.carDescription = carNameET.text.toString()
                trip.driverName = driverNameET.text.toString()
                trip.availableSeats = availableSeatsET.text.toString().toInt()
                trip.seatPrice = seatPriceET.text.toString().toBigDecimal()
                trip.description = informationsET.text.toString()


                Log.d("Trip-prova", trip.toString())

                val bundle = bundleOf("pos" to position, "trip" to Gson().toJson(trip))
                setFragmentResult("tripEditedAdded", bundle)
                findNavController().navigate(R.id.action_tripEditFragment_to_tripListFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun takeSavedPhoto(name: String?, imageView: ImageView) {
        try {
            if(name != null) {
                activity?.applicationContext?.openFileInput(name).use {
                    val imageBitmap = BitmapFactory.decodeStream(it)
                    if (imageBitmap != null)
                        imageView.setImageBitmap(imageBitmap)
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        //val info = item.menuInfo
        return when (item.itemId) {
            R.id.openGalleryOption -> {
                pickImageLauncher.launch(MediaStore.Images.Media.INTERNAL_CONTENT_URI)
                true
            }
            R.id.openCameraOption -> {
                takeImageLauncher.launch(Any())
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }
}

class ItemEditAdapter(private val items: MutableList<TripListFragment.CheckPoint>,
                      private val clickListener: (Int) -> Unit) : RecyclerView.Adapter<ItemEditAdapter.ItemEditViewHolder>() {

    class ItemEditViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val location = v.findViewById<EditText>(R.id.addressET)
        private val timestamp = v.findViewById<EditText>(R.id.itemEditTimestampET)
        private val delete_button = v.findViewById<ImageButton>(R.id.imageButton)
        private val cal = Calendar.getInstance()

        private val timeSetListener = TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
            cal.set(Calendar.HOUR, hourOfDay)
            cal.set(Calendar.MINUTE, minute)
            updateTimeInView(timestamp, cal)
        }
        private val dateSetListener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, monthOfYear)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView(timestamp, cal)
            val timePickerDialog = TimePickerDialog(v.context, timeSetListener, cal.get(Calendar.HOUR), cal.get(Calendar.MINUTE),
                    DateFormat.is24HourFormat(v.context))
            timePickerDialog.show()
        }

        fun bind(i: TripListFragment.CheckPoint, clickListener: (Int) -> Unit, items: MutableList<TripListFragment.CheckPoint>) {
            location.setText(i.location)
            location.addTextChangedListener(object: TextWatcher{
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    items[bindingAdapterPosition].location = s.toString()
                }

                override fun afterTextChanged(s: Editable?) {
                }
            })
            timestamp.setText(i.timestamp)
            timestamp.setOnClickListener {
                    DatePickerDialog(timestamp.context,
                            dateSetListener,
                            // set DatePickerDialog to point to today's date when it loads up
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)).show()
            }
            timestamp.addTextChangedListener(object: TextWatcher{
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    items[bindingAdapterPosition].timestamp = s.toString()
                }

                override fun afterTextChanged(s: Editable?) {
                }
            })

            delete_button.setOnClickListener {
                clickListener(bindingAdapterPosition)

            }
        }

        private fun updateDateInView(timestamp: EditText, cal: Calendar) {
            val myFormat = "MM/dd/yyyy" // mention the format you need
            val sdf = SimpleDateFormat(myFormat, Locale.US)
            timestamp.setText(sdf.format(cal.time))
        }

        private fun updateTimeInView(timestamp: EditText, cal: Calendar) {
            val myFormat = "HH:mm" // mention the format you need
            val sdf = SimpleDateFormat(myFormat, Locale.US)
            timestamp.setText(timestamp.text.toString() + " " + sdf.format(cal.time))
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemEditViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val layout = when (viewType) {
            R.layout.departure_item_edit -> {
                layoutInflater.inflate(R.layout.departure_item_edit, parent, false)
            }

            R.layout.arrival_item_edit -> {
                layoutInflater.inflate(R.layout.arrival_item_edit, parent, false)
            }
            else -> { //R.layout.intermediate_item -> {
                layoutInflater.inflate(R.layout.intermediate_item_edit, parent, false)
            }
        }
        return ItemEditViewHolder(layout)
    }

    override fun onBindViewHolder(holder: ItemEditViewHolder, position: Int) {
        holder.bind(items[position], clickListener, items)
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> {
                return R.layout.departure_item_edit
            }
            items.size - 1 -> {
                return R.layout.arrival_item_edit
            }
            else -> {
                return R.layout.intermediate_item_edit
            }
        }
    }

    fun onItemEditDeleted(position: Int) {
        //items.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, items.size)
    }

    fun onItemEditAdded() {
        notifyItemInserted(items.size - 1)
    }
}