package it.polito.mad.group08.carpooling

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.FileNotFoundException


class ShowProfileActivity : AppCompatActivity() {
    private lateinit var photoIV: ImageView
    private lateinit var fullNameTV : TextView
    private lateinit var nicknameTV : TextView
    private lateinit var emailTV : TextView
    private lateinit var locationTV : TextView
    private lateinit var sharedPref : SharedPreferences
    private val EDIT_PROFILE_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        photoIV = findViewById<ImageView>(R.id.photoImage)
        fullNameTV = findViewById<TextView>(R.id.fullNameTV)
        nicknameTV = findViewById<TextView>(R.id.nicknameTV)
        emailTV = findViewById<TextView>(R.id.emailTV)
        locationTV = findViewById<TextView>(R.id.locationTV)

        sharedPref = this.getPreferences(Context.MODE_PRIVATE)

        val jsonObjectDefault = JSONObject()
        jsonObjectDefault.put("fullName", getString(R.string.fullName))
        jsonObjectDefault.put("nickname", getString(R.string.nickname))
        jsonObjectDefault.put("email", getString(R.string.email))
        jsonObjectDefault.put("location", getString(R.string.location))

        val jsonObject = sharedPref.getString("profile", jsonObjectDefault.toString())!!

        val deserializedJSON = JSONObject(jsonObject)

        fullNameTV.text = deserializedJSON.getString("fullName")
        nicknameTV.text = deserializedJSON.getString("nickname")
        emailTV.text = deserializedJSON.getString("email")
        locationTV.text = deserializedJSON.getString("location")

        retrieveUserImage()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("fullNameTV", fullNameTV.text.toString())
        outState.putString("nicknameTV", nicknameTV.text.toString())
        outState.putString("emailTV", emailTV.text.toString())
        outState.putString("locationTV", locationTV.text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        fullNameTV.text = savedInstanceState.getString("fullNameTV")
        nicknameTV.text = savedInstanceState.getString("nicknameTV")
        emailTV.text = savedInstanceState.getString("emailTV")
        locationTV.text = savedInstanceState.getString("locationTV")
    }

    // starts the EditProfileActivity putting extras in the intent
    private fun editProfile(){
        val intent = Intent(this, EditProfileActivity::class.java).also {
            it.putExtra("group08.lab1.fullName", fullNameTV.text.toString())
            it.putExtra("group08.lab1.nickname", nicknameTV.text.toString())
            it.putExtra("group08.lab1.email", emailTV.text.toString())
            it.putExtra("group08.lab1.location", locationTV.text.toString())
        }
        // it calls onPause()
        startActivityForResult(intent, EDIT_PROFILE_REQUEST_CODE)
    }


    private fun retrieveUserImage(){
        try{
            applicationContext.openFileInput("userProfileImage").use{
                val bitmap: Bitmap? = BitmapFactory.decodeStream(it)
                if(bitmap != null){
                    photoIV.setImageBitmap(bitmap)
                }
            }
        }
        catch(e: FileNotFoundException){
            e.printStackTrace()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.edit_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.editButton -> {
                editProfile()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == EDIT_PROFILE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null){
            val fullNameString = data.getStringExtra("group08.lab1.fullName")
            val nicknameString = data.getStringExtra("group08.lab1.nickname")
            val emailString = data.getStringExtra("group08.lab1.email")
            val locationString = data.getStringExtra("group08.lab1.location")

            fullNameTV.text =  if(fullNameString.isNullOrBlank()) getString(R.string.fullName) else fullNameString
            nicknameTV.text = if(nicknameString.isNullOrBlank()) getString(R.string.nickname) else nicknameString
            emailTV.text = if(emailString.isNullOrBlank()) getString(R.string.email) else emailString
            locationTV.text = if(locationString.isNullOrBlank()) getString(R.string.location) else locationString

            with (sharedPref.edit()) {
                val jsonObject = JSONObject()
                jsonObject.put("fullName", fullNameTV.text)
                jsonObject.put("nickname", nicknameTV.text)
                jsonObject.put("email", emailTV.text)
                jsonObject.put("location", locationTV.text)

                putString("profile", jsonObject.toString())
                apply()
            }

            retrieveUserImage()
        }
        else if(requestCode == EDIT_PROFILE_REQUEST_CODE && resultCode != Activity.RESULT_CANCELED){
            Toast.makeText(this, "Error detected!", Toast.LENGTH_LONG).show()
        }
    }
}