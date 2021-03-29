package it.polito.mad.group08.carpooling

import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity


class EditProfileActivity : AppCompatActivity() {
    private lateinit var photoIV: ImageView
    private lateinit var changePhotoIB: ImageButton
    private lateinit var fullNameET : EditText
    private lateinit var nicknameET : EditText
    private lateinit var emailET : EditText
    private lateinit var locationET : EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        photoIV = findViewById<ImageView>(R.id.photoImage)
        changePhotoIB = findViewById<ImageButton>(R.id.changeImageButton)
        fullNameET = findViewById<EditText>(R.id.fullnameET)
        nicknameET = findViewById<EditText>(R.id.nicknameET)
        emailET = findViewById<EditText>(R.id.emailET)
        locationET = findViewById<EditText>(R.id.locationET)


        //TODO a06widget - focus() on the fullName editText ?
        fullNameET.hint = intent.getStringExtra("fullNameTV")
        nicknameET.hint = intent.getStringExtra("nicknameTV")
        emailET.hint = intent.getStringExtra("emailTV")
        locationET.hint = intent.getStringExtra("locationTV")

        changePhotoIB.setOnClickListener {
            registerForContextMenu(it)
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menuInflater.inflate(R.menu.floating_context_menu_edit, menu)
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
//                val takePicture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//                startActivityForResult(takePicture, 1)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }
}