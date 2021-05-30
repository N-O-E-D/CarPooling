package it.polito.mad.group08.carpooling

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
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
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*

class TripEditFragment : Fragment() {
    //VIEW

    private lateinit var carNameET: EditText
    private lateinit var driverNameET: EditText
    private lateinit var seatPriceET: EditText
    private lateinit var availableSeatsET: EditText
    private lateinit var informationsET: EditText
    private lateinit var carPhotoET: ImageView
    private lateinit var changeCarPhotoET: ImageButton
    private lateinit var ratingBar: RatingBar
    private lateinit var button_stop: Button

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ItemEditAdapter

    // TEMPORARY VARIABLE BEFORE SAVE
    private var filename: String? = null
    private lateinit var trip: Trip
    private var tmp_checkpoints: MutableList<CheckPoint> = mutableListOf()
    private var currentPhotoPath: String = ""

    private var position: Int = -1

    // USED FOR INTENT
    private lateinit var pickImageContract: ActivityResultContract<Uri, Uri?>
    private lateinit var pickImageCallback: ActivityResultCallback<Uri?>
    private lateinit var pickImageLauncher: ActivityResultLauncher<Uri>
    private lateinit var takeImageContract: ActivityResultContract<Any, Any?>
    private lateinit var takeImageCallback: ActivityResultCallback<Any?>
    private lateinit var takeImageLauncher: ActivityResultLauncher<Any>

    private lateinit var map: MapView
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    private var geoPoints: MutableList<GeoPoint> = mutableListOf()
    private var itemsGeoPoint: ArrayList<OverlayItem> = arrayListOf()
    private val model: SharedViewModel by activityViewModels()
    private lateinit var clearButton : Button

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID)

        pickImageContract = object : ActivityResultContract<Uri, Uri?>() {
            override fun createIntent(context: Context, input: Uri): Intent {
                return Intent(Intent.ACTION_PICK, input)
            }

            override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
                if (resultCode != Activity.RESULT_OK || intent == null)
                    return null
                return intent.data
            }
        }

        pickImageCallback = ActivityResultCallback { imageUri: Uri? ->
            if (imageUri != null) {
                val bitmap = when {
                    Build.VERSION.SDK_INT < 28 -> MediaStore.Images.Media.getBitmap(
                        activity?.contentResolver,
                        imageUri
                    )
                    else -> {
                        val source =
                            ImageDecoder.createSource(activity?.contentResolver!!, imageUri)
                        ImageDecoder.decodeBitmap(source)
                    }
                }

                filename = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + "_" + model.auth.currentUser!!.email!!
                carPhotoET.setImageBitmap(bitmap)
            }
        }

        pickImageLauncher = registerForActivityResult(pickImageContract, pickImageCallback)

        takeImageContract = object : ActivityResultContract<Any, Any?>() {
            override fun createIntent(context: Context, input: Any): Intent {
                val photoFile: File? = try {
                    val timeStamp: String =
                        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val storageDir: File =
                        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
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
                    return Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(
                        MediaStore.EXTRA_OUTPUT,
                        photoURI
                    )
                }
                return Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            }

            override fun parseResult(resultCode: Int, intent: Intent?): Bitmap? {
                if (resultCode != Activity.RESULT_OK || intent == null) {
                    currentPhotoPath = ""
                    return null
                }
                return intent.getParcelableExtra("data")
            }


        }

        takeImageCallback = ActivityResultCallback {
            if (currentPhotoPath != "") {
                val bitmap = BitmapFactory.decodeFile(currentPhotoPath)
                carPhotoET.setImageBitmap(bitmap)

                filename = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())+ "_" + model.auth.currentUser!!.email!!
            }
        }

        takeImageLauncher = registerForActivityResult(takeImageContract, takeImageCallback)
    }

    private fun removeAt(position: Int) {
        tmp_checkpoints.removeAt(position)
        adapter.onItemEditDeleted(position)
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    @SuppressLint("WrongThread")
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (view != null) {
            outState.putString("currentPhotoPath", currentPhotoPath)
            outState.putString("filename", filename)
            outState.putString("tmp_checkpoints", Gson().toJson(tmp_checkpoints))
            outState.putString("geopoints", Gson().toJson(geoPoints))
            val cacheFile = File.createTempFile("cacheImage", null, context?.cacheDir)
            val tmpBitmap = (carPhotoET.drawable as BitmapDrawable).bitmap
            tmpBitmap?.compress(Bitmap.CompressFormat.PNG, 100, cacheFile.outputStream())
            outState.putString("cacheFilePath", cacheFile.name)
            outState.putString("photoPath", currentPhotoPath)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if(savedInstanceState!=null){
            val cacheFile = File(context?.cacheDir, savedInstanceState.getString("cacheFilePath")!!)
            val bitmap = BitmapFactory.decodeFile(cacheFile.path)
            if (bitmap != null) {
                carPhotoET.setImageBitmap(bitmap)
            }
            cacheFile.delete()
            val type_checkpoints: Type = object : TypeToken<MutableList<CheckPoint?>?>() {}.type
            val type_geopoints: Type = object : TypeToken<MutableList<GeoPoint?>?>() {}.type
            currentPhotoPath = savedInstanceState.getString("currentPhotoPath")!!
            filename = savedInstanceState.getString("filename")
            tmp_checkpoints = GsonBuilder().create()
                .fromJson(savedInstanceState.getString("tmp_checkpoints"), type_checkpoints)
            geoPoints = GsonBuilder().create()
                    .fromJson(savedInstanceState.getString("geopoints"), type_geopoints)
            itemsGeoPoint = ArrayList<OverlayItem>()
            for(geopoint in geoPoints){
                itemsGeoPoint.add(OverlayItem("Ancona", "Descrizione", geopoint))
            }
            GeoMap.drawPath(map, geoPoints, context, itemsGeoPoint)
            GeoMap.setUpPinPoint(map, geoPoints, context, itemsGeoPoint)
            adapter = ItemEditAdapter(tmp_checkpoints) {position -> removeAt(position)}
            recyclerView.adapter = adapter
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_trip_edit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val button: ImageButton = view.findViewById(R.id.imageButton)
        button.setOnClickListener {
            registerForContextMenu(it)
            activity?.openContextMenu(it)
            unregisterForContextMenu(it)
        }

        carNameET = view.findViewById(R.id.carNameET)
        driverNameET = view.findViewById(R.id.driverNameET)
        seatPriceET = view.findViewById(R.id.seatPriceET)
        availableSeatsET = view.findViewById(R.id.availableSeatsET)
        informationsET = view.findViewById(R.id.tripDescriptionET)
        carPhotoET = view.findViewById(R.id.carPhoto)
        changeCarPhotoET = view.findViewById(R.id.imageButton)
        ratingBar = view.findViewById(R.id.driverRate)
        button_stop = view.findViewById(R.id.new_stop)
        recyclerView = view.findViewById(R.id.tripRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        button_stop.setOnClickListener {
            tmp_checkpoints.add(CheckPoint("", ""))
            adapter.onItemEditAdded()
        }

        map = view.findViewById(R.id.mapEdit)
        loadMyTrips()

        GeoMap.customizeMap(map, requireView(), context)

        clearButton = view.findViewById(R.id.clearButton)
        clearButton.setOnClickListener{
            geoPoints.clear()
            itemsGeoPoint.clear()
            GeoMap.clearPath(map, view, context)
        }
    }

    private fun loadMyTrips() {
        position = model.getPosition().value!!
        val trips = model.getMyTrips().value
        if(trips is Resource.Success){
            if (position == trips.data.size) { // ADD EMPTY VIEW
                trip = Trip()
                adapter =
                    ItemEditAdapter(tmp_checkpoints) { position -> removeAt(position) }
                recyclerView.adapter = adapter
                GeoMap.setUpPinPoint(map, geoPoints, context, itemsGeoPoint)
            } else {
                // EDIT EXISTING TRIP VIEW
                trip = trips.data[position]

                carNameET.setText(trip.carDescription)
                driverNameET.setText(trip.driverName)
                seatPriceET.setText(trip.seatPrice.toString())
                availableSeatsET.setText(trip.availableSeats.toString())
                informationsET.setText(trip.description)
                ratingBar.rating = trip.driverRate

                for (item in trip.checkPoints) {
                    tmp_checkpoints.add(CheckPoint(item.location, item.timestamp))
                }

                takeSavedPhoto(model.bitmaps[trip.id])

                adapter =
                    ItemEditAdapter(tmp_checkpoints) { position -> removeAt(position) }
                recyclerView.adapter = adapter

                val geoPointsCoord = trip.geoPoints
                for(geoPoint in geoPointsCoord){
                    geoPoints.add(GeoPoint(geoPoint.latitude, geoPoint.longitude))
                    Log.d("ABCDE", "$geoPoint")
                }
                itemsGeoPoint = ArrayList<OverlayItem>()
                GeoMap.drawPath(map, geoPointsCoord.map { elem -> GeoPoint(elem.latitude,elem.longitude) }.toMutableList(), context, itemsGeoPoint)
                GeoMap.setUpPinPoint(map, geoPoints, context, itemsGeoPoint)
            }
        }

        /*model.getMyTrips()
            .observe(viewLifecycleOwner, Observer<Resource<List<Trip>>> { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        showAllComponents(false)
                        editProgressBar.visibility = View.VISIBLE
                    }
                    is Resource.Success -> {
                        editProgressBar.visibility = View.GONE
                        showAllComponents(true)

                        if (position == resource.data.size) { // ADD EMPTY VIEW
                            trip = Trip()
                            adapter =
                                ItemEditAdapter(tmp_checkpoints) { position -> removeAt(position) }
                            recyclerView.adapter = adapter
                            GeoMap.setUpPinPoint(map, geoPoints, context, itemsGeoPoint)
                        } else {
                            // EDIT EXISTING TRIP VIEW
                            trip = resource.data[position]

                            carNameET.setText(trip.carDescription)
                            driverNameET.setText(trip.driverName)
                            seatPriceET.setText(trip.seatPrice.toString())
                            availableSeatsET.setText(trip.availableSeats.toString())
                            informationsET.setText(trip.description)
                            ratingBar.rating = trip.driverRate

                            for (item in trip.checkPoints) {
                                tmp_checkpoints.add(CheckPoint(item.location, item.timestamp))
                            }

                            takeSavedPhoto(model.bitmaps[trip.id])

                            adapter =
                                ItemEditAdapter(tmp_checkpoints) { position -> removeAt(position) }
                            recyclerView.adapter = adapter

                            val geoPointsCoord = trip.geoPoints
                            for(geoPoint in geoPointsCoord){
                                geoPoints.add(GeoPoint(geoPoint.latitude, geoPoint.longitude))
                                Log.d("ABCDE", "$geoPoint")
                            }
                            itemsGeoPoint = ArrayList<OverlayItem>()
                            GeoMap.drawPath(map, geoPointsCoord.map { elem -> GeoPoint(elem.latitude,elem.longitude) }.toMutableList(), context, itemsGeoPoint)
                            GeoMap.setUpPinPoint(map, geoPoints, context, itemsGeoPoint)
                        }
                    }
                    is Resource.Failure -> {
                        showAllComponents(false)
                        editProgressBar.visibility = View.GONE

                        Toast.makeText(
                            context,
                            getString(R.string.error_occur),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            })*/
    }

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
        for (checkpoint in tmp_checkpoints)
            if (checkpoint.location == "" || checkpoint.timestamp == "")
                return true
        return false
    }

    private fun checkCheckpointCoherency(): Boolean {
        val format = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US)
        for (i in 0..tmp_checkpoints.size - 2) {
            val date1 = format.parse(tmp_checkpoints[i].timestamp)
            val date2 = format.parse(tmp_checkpoints[i + 1].timestamp)
            if (date2!!.time <= date1!!.time)
                return true
        }
        return false
    }

    private fun checkCheckpointFormat(): Boolean {
        for (checkpoint in tmp_checkpoints)
            if (checkpoint.timestamp.length != 16)
                return true
        return false
    }

    @SuppressLint("WrongThread")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.saveButton -> {
                if (carNameET.text.toString() == "" || driverNameET.text.toString() == "" ||
                    availableSeatsET.text.toString() == "" || seatPriceET.text.toString() == "" ||
                    checkCheckpoint()
                ) {

                    Toast.makeText(
                        activity?.applicationContext,
                        "Please fill all the fields before saving!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return true
                }

                if (tmp_checkpoints.size < 2) {
                    Toast.makeText(
                        activity?.applicationContext,
                        "Please insert at least departure and arrival!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return true
                }

                if (checkCheckpointFormat()) {
                    Toast.makeText(
                        activity?.applicationContext,
                        "Please insert also the time!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return true
                }

                if (checkCheckpointCoherency()) {
                    Toast.makeText(
                        activity?.applicationContext,
                        "Please make sure the dates are in chronological order!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return true
                }

                trip.carDescription = carNameET.text.toString()
                trip.driverName = driverNameET.text.toString()
                trip.driverEmail = model.auth.currentUser!!.email!!
                trip.availableSeats = availableSeatsET.text.toString().toInt()
                trip.seatPrice = seatPriceET.text.toString().toFloat()
                trip.description = informationsET.text.toString()
                trip.checkPoints = tmp_checkpoints
                trip.geoPoints = geoPoints.map { elem -> Coordinate(elem.latitude,elem.longitude) }.toMutableList()


                if (filename != null) {
                    val tmpBitmap = (carPhotoET.drawable as BitmapDrawable).bitmap
                    model.updatePhotoTrip(filename!!, tmpBitmap, trip)
                } else
                    model.addOrReplaceTrip(trip)

                findNavController().navigate(R.id.action_tripEditFragment_to_tripListFragment)

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun takeSavedPhoto(bitmap: Bitmap?) {
        if (bitmap != null) {
            carPhotoET.setImageBitmap(bitmap)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
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

class ItemEditAdapter(
    private val items: MutableList<CheckPoint>,
    private val clickListener: (Int) -> Unit
) : RecyclerView.Adapter<ItemEditAdapter.ItemEditViewHolder>() {

    class ItemEditViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val location = v.findViewById<EditText>(R.id.addressET)
        private val timestamp = v.findViewById<EditText>(R.id.itemEditTimestampET)
        private val delete_button = v.findViewById<ImageButton>(R.id.imageButton)
        private val cal = Calendar.getInstance()

        private val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            cal.set(Calendar.HOUR, hourOfDay)
            cal.set(Calendar.MINUTE, minute)
            updateTimeInView(timestamp, cal)
        }
        private val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateInView(timestamp, cal)
                val timePickerDialog = TimePickerDialog(
                    v.context, timeSetListener, cal.get(Calendar.HOUR), cal.get(Calendar.MINUTE),
                    DateFormat.is24HourFormat(v.context)
                )
                timePickerDialog.show()
            }

        fun bind(i: CheckPoint, clickListener: (Int) -> Unit, items: MutableList<CheckPoint>) {
            location.setText(i.location)
            location.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    items[bindingAdapterPosition].location = s.toString()
                }

                override fun afterTextChanged(s: Editable?) {
                }
            })
            timestamp.setText(i.timestamp)
            timestamp.setOnClickListener {
                DatePickerDialog(
                    timestamp.context,
                    dateSetListener,
                    // set DatePickerDialog to point to today's date when it loads up
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                )
                    .apply {
                        datePicker.minDate = System.currentTimeMillis() - 1000
                    }.show()
            }
            timestamp.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
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

        fun unbind() {
            timestamp.setOnClickListener { null }
            delete_button.setOnClickListener { null }
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

    override fun onViewRecycled(holder: ItemEditViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> {
                R.layout.departure_item_edit
            }
            items.size - 1 -> {
                R.layout.arrival_item_edit
            }
            else -> {
                R.layout.intermediate_item_edit
            }
        }
    }

    fun onItemEditDeleted(position: Int) {
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, items.size)
    }

    fun onItemEditAdded() {
        notifyItemInserted(items.size - 1)
    }
}