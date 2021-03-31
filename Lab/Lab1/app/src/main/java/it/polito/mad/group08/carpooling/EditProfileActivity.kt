package it.polito.mad.group08.carpooling

import android.R.attr.bitmap
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class EditProfileActivity : AppCompatActivity() {
    private lateinit var photoIV: ImageView
    private lateinit var changePhotoIB: ImageButton
    private lateinit var fullNameET : EditText
    private lateinit var nicknameET : EditText
    private lateinit var emailET : EditText
    private lateinit var locationET : EditText

    //Take new photo
    val REQUEST_IMAGE_CAPTURE = 1
    lateinit var currentPhotoPath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        photoIV = findViewById<ImageView>(R.id.photoImage)
        changePhotoIB = findViewById<ImageButton>(R.id.changeImageButton)
        fullNameET = findViewById<EditText>(R.id.fullnameET)
        nicknameET = findViewById<EditText>(R.id.nicknameET)
        emailET = findViewById<EditText>(R.id.emailET)
        locationET = findViewById<EditText>(R.id.locationET)

        fullNameET.setText(intent.getStringExtra("fullNameTV"))
        nicknameET.setText(intent.getStringExtra("nicknameTV"))
        emailET.setText(intent.getStringExtra("emailTV"))
        locationET.setText(intent.getStringExtra("locationTV"))

        changePhotoIB.setOnClickListener {
            registerForContextMenu(it)
        }
    }

    //Context Menu is the "change image" button
    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if(applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY))
            menuInflater.inflate(R.menu.floating_context_menu_edit, menu)
        else
            menuInflater.inflate(R.menu.floating_context_menu_edit_no_cam, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        super.onContextItemSelected(item)
        //val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
        return when (item.itemId) {
            R.id.phoneGallery -> {
//                val pickPhoto = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
//                startActivityForResult(pickPhoto, 0)
                true
            }
            R.id.phoneCamera -> {
                Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                    takePictureIntent.resolveActivity(packageManager)?.also {
                        val photoFile: File? = try {
                            createImageFile()
                        } catch (ex: IOException) {
                            Toast.makeText(applicationContext, "Error in creating Image File", Toast.LENGTH_LONG).show()
                            null
                        }

                        //if null not continue
                        photoFile?.also {
                            val photoURI: Uri = FileProvider.getUriForFile(this,
                                    "it.polito.mad.group08.carpooling", it)
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                        }
                    }
                }
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

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

    //return from "change image" Intent
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("TEST", "onActivityResult")
        if(requestCode==REQUEST_IMAGE_CAPTURE && resultCode== RESULT_OK && data != null){
            val ei = ExifInterface(currentPhotoPath)
            val orientation: Int = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED)

            //TODO understand why this line is bugged
            val imageBitmap = data.extras?.get("data") as Bitmap
            Log.d("TEST", imageBitmap.toString())
            val rotatedBitmap: Bitmap? = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(imageBitmap, 90)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(imageBitmap, 180)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(imageBitmap, 270)
                ExifInterface.ORIENTATION_NORMAL -> imageBitmap
                else -> imageBitmap
            }

            photoIV.setImageBitmap(rotatedBitmap)
        }
    }

    private fun rotateImage(source: Bitmap, angle: Int): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(angle.toFloat())
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height,
                matrix, true)
    }

    //Option Menu is the "save" button
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.option_menu_save_edit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.saveEdit -> {
                saveAndReturn()
                //TODO return true never done?
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveAndReturn(){
        val intent = Intent().also {
            // TODO seliazizzation?
            //it.putExtra("photoIV", photoIV. .toString())
            it.putExtra("fullNameET", fullNameET.text.toString())
            it.putExtra("nicknameET", nicknameET.text.toString())
            it.putExtra("emailET", emailET.text.toString())
            it.putExtra("locationET", locationET.text.toString())
        }

        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}