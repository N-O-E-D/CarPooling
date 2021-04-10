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
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.jvm.Throws

class EditProfileActivity : AppCompatActivity() {

    private lateinit var photoIV: ImageView
    private lateinit var changePhotoIB: ImageButton
    private lateinit var fullNameET : EditText
    private lateinit var nicknameET : EditText
    private lateinit var emailET : EditText
    private lateinit var locationET : EditText
    private lateinit var photoURI: Uri
    private var currentPhotoPath: String = ""
    private val OPEN_CAMERA_REQUEST_CODE = 1
    private val OPEN_GALLERY_REQUEST_CODE = 2
    private var bitmap: Bitmap? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        photoIV = findViewById<ImageView>(R.id.photoImage)
        changePhotoIB = findViewById<ImageButton>(R.id.changeImageButton)
        fullNameET = findViewById<EditText>(R.id.fullNameET)
        fullNameET.setText(intent.getStringExtra("group08.lab1.fullName"))
        nicknameET = findViewById<EditText>(R.id.nicknameET)
        nicknameET.setText(intent.getStringExtra("group08.lab1.nickname"))
        emailET = findViewById<EditText>(R.id.emailET)
        emailET.setText(intent.getStringExtra("group08.lab1.email"))
        locationET = findViewById<EditText>(R.id.locationET)
        locationET.setText(intent.getStringExtra("group08.lab1.location"))

        changePhotoIB.setOnClickListener{
            registerForContextMenu(it)
            openContextMenu(it)
            unregisterForContextMenu(it)
        }

        retrieveUserImage()
    }

    // Creates a temp file in which will be stored a new picture
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
                "JPEG_${timeStamp}_", /* prefix */
                ".jpg", /* suffix */
                storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    // It uses createImageFile() and retrieve the URI, then it starts the camera activity
    private fun dispatchTakePictureIntent() {

        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    Toast.makeText(this, "Can't save the image", Toast.LENGTH_SHORT).show()
                    null
                }

                // Continue only if the File was successfully created
                photoFile?.also {
                    photoURI = FileProvider.getUriForFile(
                            this,
                            "it.polito.mad.group08.carpooling",
                            it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, OPEN_CAMERA_REQUEST_CODE)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("path", currentPhotoPath)
        val stream = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val image = stream.toByteArray()
        outState.putByteArray("bitmap", image)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        currentPhotoPath = savedInstanceState.getString("path").toString()
        val byteArray: ByteArray? = savedInstanceState.getByteArray("bitmap")
        val bitmapRetrieved = BitmapFactory.decodeByteArray(byteArray, 0, byteArray?.size!!)
        bitmap = bitmapRetrieved
        photoIV.setImageBitmap(bitmap)
    }

    // It retrieve the user image from the internal storage
    private fun retrieveUserImage(){
        try{
            applicationContext.openFileInput("userProfileImage").use{
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

    // creates the floating context menu in order to select or take picture
    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menuInflater.inflate(R.menu.floating_menu, menu)
    }

    // listeners for floating context menu options
    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.openCameraOption -> {
                if(packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)){
                    dispatchTakePictureIntent()
                    true
                }
                else{
                    Toast.makeText(this, "Camera not available", Toast.LENGTH_LONG).show()
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

    // creates option menu in order to save changes
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.save_menu, menu)
        return true
    }

    // listeners for the save button
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.saveButton -> {
                val intent = Intent().also {
                    it.putExtra("group08.lab1.fullName", fullNameET.text.toString())
                    it.putExtra("group08.lab1.nickname", nicknameET.text.toString())
                    it.putExtra("group08.lab1.email", emailET.text.toString())
                    it.putExtra("group08.lab1.location", locationET.text.toString())
                }

                if(bitmap != null){
                    // it saves the bitmap into the internal storage
                    try{
                        applicationContext.openFileOutput("userProfileImage", Context.MODE_PRIVATE).use{
                            bitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, it)
                        }

                    }
                    catch(e: IOException){
                        Toast.makeText(this, "Not enough space to store the photo!", Toast.LENGTH_LONG).show()
                    }
                }


                setResult(Activity.RESULT_OK, intent)

                //calls onDestroy()
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
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


    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == OPEN_CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            // it retrieves the bitmap from currentPhotoPath. If needed it rotates the bitmap and set it in the imageview.

            bitmap = BitmapFactory.decodeFile(currentPhotoPath)
            rotateAndSet(bitmap!!)

        }
        else if (requestCode == OPEN_GALLERY_REQUEST_CODE && resultCode == RESULT_OK){
            // it retrieves the bitmap from imageUri and set it in the imageview.
            val imageUri = data?.data!!

            bitmap = when {
                Build.VERSION.SDK_INT < 28 -> MediaStore.Images.Media.getBitmap(
                        this.contentResolver,
                        imageUri
                )
                else -> {
                    val source = ImageDecoder.createSource(this.contentResolver, imageUri)
                    ImageDecoder.decodeBitmap(source)
                }
            }

            photoIV.setImageBitmap(bitmap)
        }
        else if((requestCode == OPEN_CAMERA_REQUEST_CODE || requestCode == OPEN_GALLERY_REQUEST_CODE) && resultCode != Activity.RESULT_CANCELED){
            Toast.makeText(this, "Error detected!", Toast.LENGTH_LONG).show()
        }
    }
}
