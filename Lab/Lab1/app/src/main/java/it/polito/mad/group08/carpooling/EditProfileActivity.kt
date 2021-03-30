package it.polito.mad.group08.carpooling

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView

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
        fullNameET = findViewById<EditText>(R.id.fullNameET)
        fullNameET.hint = intent.getStringExtra("group08.lab1.fullName")
        nicknameET = findViewById<EditText>(R.id.nicknameET)
        nicknameET.hint = intent.getStringExtra("group08.lab1.nickname")
        emailET = findViewById<EditText>(R.id.emailET)
        emailET.hint = intent.getStringExtra("group08.lab1.email")
        locationET = findViewById<EditText>(R.id.locationET)
        locationET.hint = intent.getStringExtra("group08.lab1.location")
        registerForContextMenu(changePhotoIB)
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.floating_menu, menu)
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
}
