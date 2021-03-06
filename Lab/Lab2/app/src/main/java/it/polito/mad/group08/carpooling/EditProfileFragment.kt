package it.polito.mad.group08.carpooling

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.jvm.Throws

class EditProfileFragment : Fragment(R.layout.fragment_edit_profile) {

    private lateinit var photoIV: ImageView
    private lateinit var changePhotoIB: ImageButton
    private lateinit var fullNameET : TextInputEditText
    private lateinit var nicknameET : TextInputEditText
    private lateinit var emailET : TextInputEditText
    private lateinit var locationET : TextInputEditText
    private lateinit var phonenumberET : TextInputEditText
    private lateinit var photoURI: Uri
    private var currentPhotoPath: String = ""
    private val OPEN_CAMERA_REQUEST_CODE = 1
    private val OPEN_GALLERY_REQUEST_CODE = 2
    private var bitmap: Bitmap? = null

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        setHasOptionsMenu(true)

        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        photoIV = view.findViewById<ImageView>(R.id.photoImage)
        changePhotoIB = view.findViewById<ImageButton>(R.id.changeImageButton)

        fullNameET = view.findViewById<TextInputEditText>(R.id.fullNameET)
        fullNameET.setText(arguments?.getString("fullname"))

        nicknameET = view.findViewById<TextInputEditText>(R.id.nicknameET)
        nicknameET.setText(arguments?.getString("nickname"))

        emailET = view.findViewById<TextInputEditText>(R.id.emailET)
        emailET.setText(arguments?.getString("email"))

        locationET = view.findViewById<TextInputEditText>(R.id.locationET)
        locationET.setText(arguments?.getString("location"))

        phonenumberET = view.findViewById<TextInputEditText>(R.id.phonenumberET)
        phonenumberET.setText(arguments?.getString("phonenumber"))

        changePhotoIB.setOnClickListener{
            registerForContextMenu(it)
            activity?.openContextMenu(it)
            unregisterForContextMenu(it)
        }

        retrieveUserImage()
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val cacheFile = File.createTempFile("cacheImage",null,context?.cacheDir)
        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, cacheFile.outputStream())
        outState.putString("cacheFilePath",cacheFile.name)
        outState.putString("photoPath", currentPhotoPath)
    }

    //called after onViewCreated
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if(savedInstanceState!=null){
            currentPhotoPath = savedInstanceState.getString("photoPath")!!
            val cacheFile = File(context?.cacheDir, savedInstanceState.getString("cacheFilePath")!!)
            bitmap = BitmapFactory.decodeFile(cacheFile.path)
            if(bitmap!=null)
                photoIV.setImageBitmap(bitmap)
            cacheFile.delete()
        }
    }

    private fun dispatchTakePictureIntent() {

        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(requireActivity().packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    Toast.makeText(activity, "Can't save the image", Toast.LENGTH_SHORT).show()
                    null
                }

                // Continue only if the File was successfully created
                photoFile?.also {
                    photoURI = FileProvider.getUriForFile(
                        requireActivity(),
                        "it.polito.mad.group08.carpooling",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, OPEN_CAMERA_REQUEST_CODE)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.save_menu, menu)
    }


    // listeners for the save button
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.saveButton -> {
                val bundle = bundleOf("fullname" to fullNameET.text.toString(),
                    "nickname" to nicknameET.text.toString(),
                    "email" to emailET.text.toString(),
                    "location" to locationET.text.toString(),
                    "phonenumber" to phonenumberET.text.toString())


                if(bitmap != null){
                    // it saves the bitmap into the internal storage
                    try{
                        requireActivity().applicationContext.openFileOutput("userProfileImage", Context.MODE_PRIVATE).use{
                            bitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, it)
                        }
                    }
                    catch(e: IOException){
                        Toast.makeText(activity, "Not enough space to store the photo!", Toast.LENGTH_LONG).show()
                    }
                }
                findNavController().navigate(R.id.action_editProfileFragment_to_showProfileFragment,bundle)
                Snackbar.make(view?.findViewById(R.id.fullNameET)!!,R.string.changes_applied_successfully,Snackbar.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun retrieveUserImage(){
        try{
            requireActivity().applicationContext.openFileInput("userProfileImage").use{
                bitmap = BitmapFactory.decodeStream(it)
                if(bitmap != null){
                    photoIV.setImageBitmap(bitmap)
                }
            }
        }
        catch(e: FileNotFoundException){
            e.printStackTrace()
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        requireActivity().menuInflater.inflate(R.menu.floating_menu, menu)
    }


    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.openCameraOption -> {
                if(activity?.packageManager?.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) == true){
                    println("Has camera! Intent dispatched")
                    dispatchTakePictureIntent()
                    true
                }
                else{
                    Toast.makeText(activity, "Camera not available", Toast.LENGTH_LONG).show()
                    true
                }
            }
            R.id.openGalleryOption -> {
                val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
                startActivityForResult(gallery, OPEN_GALLERY_REQUEST_CODE)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == OPEN_CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            // it retrieves the bitmap from currentPhotoPath. If needed it rotates the bitmap and set it in the imageview.
            bitmap = BitmapFactory.decodeFile(currentPhotoPath)
            rotateAndSet(bitmap!!)
        }
        else if (requestCode == OPEN_GALLERY_REQUEST_CODE && resultCode == AppCompatActivity.RESULT_OK){
            // it retrieves the bitmap from imageUri and set it in the imageview.
            val imageUri = data?.data!!
            photoURI = imageUri

            bitmap = when {
                Build.VERSION.SDK_INT < 28 -> MediaStore.Images.Media.getBitmap(
                    activity?.contentResolver,
                    imageUri
                )
                else -> {
                    val source = ImageDecoder.createSource(requireActivity().contentResolver, imageUri)
                    ImageDecoder.decodeBitmap(source)
                }
            }

            photoIV.setImageBitmap(bitmap)
        }
        else if((requestCode == OPEN_CAMERA_REQUEST_CODE || requestCode == OPEN_GALLERY_REQUEST_CODE) && resultCode != Activity.RESULT_CANCELED){
            Toast.makeText(activity, "Error detected!", Toast.LENGTH_LONG).show()
        }
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height,
            matrix, true)
    }


    // if the camera rotates automatically the picture taken, it will adjust it properly
    private fun rotateAndSet(imageBitmap: Bitmap){
        val ei = ExifInterface(currentPhotoPath)
        val orientation: Int = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED)

        val rotatedBitmap: Bitmap? = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(imageBitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(imageBitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(imageBitmap, 270f)
            ExifInterface.ORIENTATION_NORMAL -> imageBitmap
            else -> imageBitmap
        }
        photoIV.setImageBitmap(rotatedBitmap)
    }

}