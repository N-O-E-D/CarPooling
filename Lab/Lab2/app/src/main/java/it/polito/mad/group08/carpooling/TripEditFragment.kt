package it.polito.mad.group08.carpooling

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.FileNotFoundException
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*

class TripEditFragment : Fragment() {
    lateinit var carNameET: EditText
    lateinit var driverNameET: EditText
    lateinit var seatPriceET: EditText
    lateinit var availableSeatsET: EditText
    lateinit var informationsET: EditText
    lateinit var imageView: ImageView
    var filename: String? = null
    lateinit var trip: TripListFragment.Trip
    var position: Int? = null
    private lateinit var pickImageContract: ActivityResultContract<Uri, Uri?>
    private lateinit var pickImageCallback: ActivityResultCallback<Uri?>
    private lateinit var pickImageLauncher: ActivityResultLauncher<Uri>
    private lateinit var takeImageContract: ActivityResultContract<Any, Bitmap?>
    private lateinit var takeImageCallback: ActivityResultCallback<Bitmap?>
    private lateinit var takeImageLauncher: ActivityResultLauncher<Any>

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

        //val pickContactIntent = pickImageContract.createIntent(requireContext(), MediaStore.Images.Media.INTERNAL_CONTENT_URI)

        takeImageContract = object : ActivityResultContract<Any, Bitmap?>() {
            override fun createIntent(context: Context, input: Any): Intent {
                return Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            }
            override fun parseResult(resultCode: Int, intent: Intent?): Bitmap? {
                if(resultCode != Activity.RESULT_OK || intent == null)
                    return null
                return intent.getParcelableExtra("data")
            }


        }

        takeImageCallback = ActivityResultCallback { imageBitmap: Bitmap? ->
            if(imageBitmap != null) {
                imageView.setImageBitmap(imageBitmap)

                filename = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

                if (imageBitmap != null) {
                    activity?.applicationContext?.openFileOutput(filename, Context.MODE_PRIVATE).use {
                        imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                }
            }
        }

        takeImageLauncher = registerForActivityResult(takeImageContract, takeImageCallback)

        setFragmentResultListener("fromDetailsToEdit") { requestKey, bundle ->
            if (requestKey == "fromDetailsToEdit") {
                val tripJSON = bundle.getString("trip")
                val type: Type = object : TypeToken<TripListFragment.Trip?>() {}.type
                trip = GsonBuilder().create().fromJson(tripJSON, type)
                println(trip)
            }
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

        carNameET = view.findViewById(R.id.carName)
        driverNameET = view.findViewById(R.id.driverName)
        seatPriceET = view.findViewById(R.id.seatPriceET)
        availableSeatsET = view.findViewById(R.id.availableSeatsET)
        informationsET = view.findViewById(R.id.tripDescription)
        imageView = view.findViewById(R.id.carPhoto)

        if(arguments?.getString("type") == "modify") {
            val type: Type = object : TypeToken<TripListFragment.Trip?>() {}.type
            trip = GsonBuilder().create().fromJson(arguments?.getString("trip"), type)
            position = arguments?.getInt("position")

            carNameET.setText(trip.carDescription)
            driverNameET.setText(trip.driverName)
            seatPriceET.setText(trip.seatPrice.toString())
            availableSeatsET.setText(trip.availableSeats)
            informationsET.setText(trip.description)
            takeSavedPhoto(trip.carPhotoPath, imageView)
        }
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        /*if (arguments?.getString("type") == "add") {
            findNavController().navigate(R.id.action_tripEditFragment_to_tripListFragment,
                bundleOf("trip" to Gson().toJson(
                    TripListFragment.Trip(

                    )
                ), "type" to "add"))
        } else if (arguments?.getString("type") == "modify") {
            findNavController().navigate(R.id.action_tripEditFragment_to_tripListFragment,
                bundleOf("trip" to Gson().toJson(
                    TripListFragment.Trip(

                    )
                ), "type" to "modify", "position" to position))
        }*/
        return super.onOptionsItemSelected(item)
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