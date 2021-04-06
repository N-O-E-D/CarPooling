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
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileNotFoundException
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
    private val REQUEST_IMAGE_CAPTURE = 1
    private lateinit var currentPhotoPath: String

    //Pick from galley
    private val REQUEST_IMAGE_GALLERY = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        photoIV = findViewById<ImageView>(R.id.photoImage)
        changePhotoIB = findViewById<ImageButton>(R.id.changeImageButton)
        fullNameET = findViewById<EditText>(R.id.fullnameET)
        nicknameET = findViewById<EditText>(R.id.nicknameET)
        emailET = findViewById<EditText>(R.id.emailET)
        locationET = findViewById<EditText>(R.id.locationET)

        fullNameET.setText(intent.getStringExtra("group08.lab1.fullName"))
        nicknameET.setText(intent.getStringExtra("group08.lab1.nickname"))
        emailET.setText(intent.getStringExtra("group08.lab1.email"))
        locationET.setText(intent.getStringExtra("group08.lab1.location"))

        currentPhotoPath = ""

        changePhotoIB.setOnClickListener {
            registerForContextMenu(it);
            openContextMenu(it);
            unregisterForContextMenu(it);
        }

        retriveUserImage()
    }

    private fun retriveUserImage(){
        try {
            applicationContext.openFileInput("image_from_camera").use {
                val imageBitmap = BitmapFactory.decodeStream(it)
                if (imageBitmap != null)
                    photoIV.setImageBitmap(imageBitmap)
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            // the application can continue without image. It will continue with default image
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
//        outState.putString("group08.lab1.currentPhotoPath", currentPhotoPath)
//        outState.putString("group08.lab1.fullName", fullNameET.text.toString())
//        outState.putString("group08.lab1.nickname", nicknameET.text.toString())
//        outState.putString("group08.lab1.email", emailET.text.toString())
//        outState.putString("group08.lab1.location", locationET.text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
//        currentPhotoPath = savedInstanceState.getString("group08.lab1.currentPhotoPath").toString()
//        fullNameET.setText(savedInstanceState.getString("group08.lab1.fullName"))
//        nicknameET.setText(savedInstanceState.getString("group08.lab1.nickname"))
//        emailET.setText( savedInstanceState.getString("group08.lab1.email"))
//        locationET.setText(savedInstanceState.getString("group08.lab1.location"))
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
        return when (item.itemId) {
            R.id.phoneGallery -> {
                val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
                startActivityForResult(gallery, REQUEST_IMAGE_GALLERY)
                true
            }
            R.id.phoneCamera -> {
                dispatchTakePictureIntent()
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

    private fun dispatchTakePictureIntent() {
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
                    val photoURI = FileProvider.getUriForFile(this,
                            "it.polito.mad.group08.carpooling", it)
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }


    //return from "change image" Intent
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode){
            REQUEST_IMAGE_CAPTURE -> {
                if (resultCode == RESULT_OK) {
                    val imageBitmap = BitmapFactory.decodeFile(currentPhotoPath)

                    //savePersistent
                    try {
                        applicationContext.openFileOutput("image_from_camera", Context.MODE_PRIVATE).use {
                            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                        }

                        //set in EditProfile
                        rotateAndSet(imageBitmap)
                    } catch (e: IOException) {
                        Toast.makeText(applicationContext, "Not enough space for save the image", Toast.LENGTH_LONG).show()
                    }
                }
            }
            REQUEST_IMAGE_GALLERY -> {
                if (resultCode == RESULT_OK) {
                    val imageUri = data?.data!!

                    val imageBitmap = when {
                        Build.VERSION.SDK_INT < 28 -> MediaStore.Images.Media.getBitmap(
                                this.contentResolver,
                                imageUri
                        )
                        else -> {
                            val source = ImageDecoder.createSource(this.contentResolver, imageUri)
                            ImageDecoder.decodeBitmap(source)
                        }
                    }

                    try{
                        applicationContext.openFileOutput("image_from_camera", Context.MODE_PRIVATE)
                                .use{
                                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                                }

                        photoIV.setImageURI(imageUri)
                    }catch (e: IOException){
                        Toast.makeText(this, "Not enough space for save the image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun rotateAndSet(imageBitmap: Bitmap){
        val ei = ExifInterface(currentPhotoPath)
        val orientation: Int = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED)

        val rotatedBitmap: Bitmap = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(imageBitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(imageBitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(imageBitmap, 270f)
            ExifInterface.ORIENTATION_NORMAL -> imageBitmap
            else -> imageBitmap
        }
        photoIV.setImageBitmap(rotatedBitmap)
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height,
                matrix, true)
    }

    //Option Menu is the "save" button
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.option_menu_save_edit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.saveEdit -> {
                saveAndReturn()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveAndReturn(){
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