package it.polito.mad.group08.carpooling

import android.annotation.SuppressLint
import android.app.Activity
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
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


private const val OPEN_CAMERA_REQUEST_CODE = 1
private const val OPEN_GALLERY_REQUEST_CODE = 2

class EditProfileFragment : Fragment() {

    private lateinit var editProfileProgressBar: ProgressBar
    private lateinit var editProfilePhotoProgressBar: ProgressBar

    private lateinit var photoIV: ImageView
    private lateinit var changePhotoIB: ImageButton
    private lateinit var fullNameET: TextInputEditText
    private lateinit var nicknameET: TextInputEditText
    private lateinit var emailET: TextInputEditText
    private lateinit var locationET: TextInputEditText
    private lateinit var phonenumberET: TextInputEditText

    private lateinit var fullNameET_layout: TextInputLayout
    private lateinit var nicknameET_layout: TextInputLayout
    private lateinit var emailET_layout: TextInputLayout
    private lateinit var locationET_layout: TextInputLayout
    private lateinit var phonenumberET_layout: TextInputLayout


    private lateinit var photoURI: Uri
    private var currentPhotoPath: String = ""
    private var bitmap: Bitmap? = null  //Temporary Photo before user click "save"
    private var tmpRatingBar: Float = 0f

    //private lateinit var user: User
    private val model: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        setHasOptionsMenu(true)

        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editProfileProgressBar = view.findViewById(R.id.editProfileProgressBar)
        editProfilePhotoProgressBar = view.findViewById(R.id.editProfilePhotoProgressBar)

        photoIV = view.findViewById(R.id.photoImage)
        changePhotoIB = view.findViewById(R.id.changeImageButton)
        fullNameET = view.findViewById(R.id.fullNameET)
        nicknameET = view.findViewById(R.id.nicknameET)
        emailET = view.findViewById(R.id.emailET)
        locationET = view.findViewById(R.id.locationET)
        phonenumberET = view.findViewById(R.id.phonenumberET)

        fullNameET_layout = view.findViewById(R.id.fullNameET_layout)
        nicknameET_layout = view.findViewById(R.id.nicknameET_layout)
        emailET_layout = view.findViewById(R.id.emailET_layout)
        locationET_layout = view.findViewById(R.id.locationET_layout)
        phonenumberET_layout = view.findViewById(R.id.phonenumberET_layout)

        changePhotoIB.setOnClickListener {
            registerForContextMenu(it)
            activity?.openContextMenu(it)
            unregisterForContextMenu(it)
        }

        model.getUser()
            .observe(viewLifecycleOwner, Observer<Resource<User>> { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        showAllComponents(false)
                        editProfileProgressBar.visibility = View.VISIBLE
                        editProfilePhotoProgressBar.visibility = View.VISIBLE
                    }
                    is Resource.Success -> {
                        editProfileProgressBar.visibility = View.GONE
                        showAllComponents(true)

                        fullNameET.setText(resource.data.name)
                        nicknameET.setText(resource.data.nickname)
                        emailET.setText(resource.data.email)
                        locationET.setText(resource.data.location)
                        phonenumberET.setText(resource.data.phone_number)

                        tmpRatingBar = resource.data.rating

                        if (bitmap != null) {
                            editProfilePhotoProgressBar.visibility = View.GONE
                            photoIV.setImageBitmap(bitmap)
                        } else {
                            model.getUserPhoto()
                                .observe(
                                    viewLifecycleOwner,
                                    Observer<Resource<Bitmap>> { resPhotoDB ->
                                        when (resPhotoDB) {
                                            is Resource.Loading -> {
                                                editProfilePhotoProgressBar.visibility =
                                                    View.VISIBLE
                                            }
                                            is Resource.Success -> {
                                                editProfilePhotoProgressBar.visibility = View.GONE
                                                photoIV.setImageBitmap(resPhotoDB.data)
                                            }
                                            is Resource.Failure -> {
                                                editProfilePhotoProgressBar.visibility = View.GONE
                                                Toast.makeText(
                                                    context,
                                                    "Error in loading the new photo",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    })
                        }
                    }
                    is Resource.Failure -> {
                        showAllComponents(false)
                        editProfileProgressBar.visibility = View.GONE
                        editProfilePhotoProgressBar.visibility = View.GONE
                        Toast.makeText(
                            context,
                            getString(R.string.error_occur),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
    }

    private fun showAllComponents(showHide: Boolean) {
        photoIV.visibility = if (showHide) View.VISIBLE else View.GONE
        changePhotoIB.visibility = if (showHide) View.VISIBLE else View.GONE
        fullNameET_layout.visibility = if (showHide) View.VISIBLE else View.GONE
        nicknameET_layout.visibility = if (showHide) View.VISIBLE else View.GONE
        emailET_layout.visibility = if (showHide) View.VISIBLE else View.GONE
        locationET_layout.visibility = if (showHide) View.VISIBLE else View.GONE
        phonenumberET_layout.visibility = if (showHide) View.VISIBLE else View.GONE
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val cacheFile = File.createTempFile("cacheImage", null, context?.cacheDir)
        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, cacheFile.outputStream())
        outState.putString("cacheFilePath", cacheFile.name)
        outState.putString("photoPath", currentPhotoPath)
    }

    //called after onViewCreated
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState != null) {
            currentPhotoPath = savedInstanceState.getString("photoPath")!!
            val cacheFile = File(context?.cacheDir, savedInstanceState.getString("cacheFilePath")!!)
            bitmap = BitmapFactory.decodeFile(cacheFile.path)
            if (bitmap != null) {
                photoIV.setImageBitmap(bitmap)
            }
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

    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
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
    @SuppressLint("WrongThread")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.saveButton -> {
                //save new photo
                if (bitmap != null) {
                    model.uploadCurrentUserPhoto(bitmap!!)
                    model.setUserBitmap(bitmap!!)
                    (activity as? ShowProfileFragment.InfoManager)?.updatePhoto(bitmap!!)
                }

                model.editUser(
                    User(
                        name = fullNameET.text.toString(),
                        nickname = nicknameET.text.toString(),
                        email = emailET.text.toString(),
                        location = locationET.text.toString(),
                        phone_number = phonenumberET.text.toString(),
                        rating = tmpRatingBar
                    )
                )

                findNavController().popBackStack()
                Snackbar.make(
                    view?.findViewById(R.id.fullNameET)!!,
                    R.string.changes_applied_successfully,
                    Snackbar.LENGTH_SHORT
                ).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        requireActivity().menuInflater.inflate(R.menu.floating_menu, menu)
    }


    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.openCameraOption -> {
                if (activity?.packageManager?.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) == true) {
                    dispatchTakePictureIntent()
                    true
                } else {
                    Toast.makeText(activity, "Camera not available", Toast.LENGTH_LONG).show()
                    true
                }
            }
            R.id.openGalleryOption -> {
                val gallery =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
                startActivityForResult(gallery, OPEN_GALLERY_REQUEST_CODE)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == OPEN_CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // it retrieves the bitmap from currentPhotoPath. If needed it rotates the bitmap and set it in the imageview.
            bitmap = BitmapFactory.decodeFile(currentPhotoPath)
            rotateAndSet(bitmap!!)
        } else if (requestCode == OPEN_GALLERY_REQUEST_CODE && resultCode == AppCompatActivity.RESULT_OK) {
            // it retrieves the bitmap from imageUri and set it in the imageview.
            val imageUri = data?.data!!
            photoURI = imageUri

            bitmap = when {
                Build.VERSION.SDK_INT < 28 -> MediaStore.Images.Media.getBitmap(
                    activity?.contentResolver,
                    imageUri
                )
                else -> {
                    val source =
                        ImageDecoder.createSource(requireActivity().contentResolver, imageUri)
                    ImageDecoder.decodeBitmap(source)
                }
            }

            photoIV.setImageBitmap(bitmap)
        } else if ((requestCode == OPEN_CAMERA_REQUEST_CODE || requestCode == OPEN_GALLERY_REQUEST_CODE) && resultCode != Activity.RESULT_CANCELED) {
            Toast.makeText(activity, "Error detected!", Toast.LENGTH_LONG).show()
        }
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }


    // if the camera rotates automatically the picture taken, it will adjust it properly
    private fun rotateAndSet(imageBitmap: Bitmap) {
        val ei = ExifInterface(currentPhotoPath)
        val orientation: Int = ei.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )

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