package it.polito.mad.group08.carpooling

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PersistableBundle
import android.provider.MediaStore
import android.view.*
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
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

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
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


    /*private fun dispatchTakePictureIntent(){
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try{
            startActivityForResult(takePictureIntent, 1)
        }
        catch(e: ActivityNotFoundException){
            Toast.makeText(this, "Can't open camera!", Toast.LENGTH_LONG).show()
        }
    }*/

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
                    startActivityForResult(takePictureIntent, 1)
                }
            }
        }
    }

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
        registerForContextMenu(changePhotoIB)
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.floating_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.openCameraOption -> {
                if(packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)){
                    dispatchTakePictureIntent()
                }
                else{
                    Toast.makeText(this, "Camera not available", Toast.LENGTH_LONG).show()
                }
            }
        }

        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.save_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.saveButton -> {
                val intent = Intent().also {
                    it.putExtra("group08.lab1.fullName", fullNameET.text.toString())
                    it.putExtra("group08.lab1.nickname", nicknameET.text.toString())
                    it.putExtra("group08.lab1.email", emailET.text.toString())
                    it.putExtra("group08.lab1.location", locationET.text.toString())
                }

                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }

        return true
    }

    private fun rescalePic() : Bitmap {
        // Get the dimensions of the View
        println("WWWWWW: "+photoIV.width)
        println("HHHHHH: "+photoIV.height)
        val targetW: Int = photoIV.width
        val targetH: Int = photoIV.height

        val bmOptions = BitmapFactory.Options().apply {
            // Get the dimensions of the bitmap
            inJustDecodeBounds = true

            BitmapFactory.decodeFile(currentPhotoPath)

            val photoW: Int = outWidth
            val photoH: Int = outHeight

            // Determine how much to scale down the image
            val scaleFactor: Int = 1.coerceAtLeast((photoW / targetW).coerceAtMost(photoH / targetH))

            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
        }
        return BitmapFactory.decodeFile(currentPhotoPath, bmOptions)

    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height,
                matrix, true)
    }

    private fun rotateAndSet(imageBitmap: Bitmap){
        val ei = ExifInterface(currentPhotoPath)
        val orientation: Int = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED)

        var rotatedBitmap: Bitmap? = null
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotatedBitmap = rotateImage(imageBitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotatedBitmap = rotateImage(imageBitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotatedBitmap = rotateImage(imageBitmap, 270f)
            ExifInterface.ORIENTATION_NORMAL -> rotatedBitmap = imageBitmap
            else -> rotatedBitmap = imageBitmap
        }
        photoIV.setImageBitmap(rotatedBitmap)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("group08.lab1.currentPhotoPath", currentPhotoPath)

    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        currentPhotoPath = savedInstanceState.getString("group08.lab1.currentPhotoPath").toString()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 1 && resultCode == Activity.RESULT_OK && data!=null){
            //val source : ImageDecoder.Source = ImageDecoder.createSource(contentResolver, photoURI)
            //val bitmap: Bitmap = ImageDecoder.decodeBitmap(source)
            /*val bitmap : Bitmap = BitmapFactory.decodeFile(currentPhotoPath)
            photoIV.setImageBitmap(bitmap)*/
            rotateAndSet(rescalePic())

            //galleryAddPic()
        }
        else{
            Toast.makeText(this, "bbbbbb", Toast.LENGTH_LONG).show()
        }
    }
}
