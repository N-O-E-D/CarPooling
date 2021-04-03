package it.polito.mad.group08.carpooling

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject


class ShowProfileActivity : AppCompatActivity() {
    private lateinit var photoIV: ImageView
    private lateinit var fullNameTV : TextView
    private lateinit var nicknameTV : TextView
    private lateinit var emailTV : TextView
    private lateinit var locationTV : TextView
    private lateinit var sharedPref : SharedPreferences
    private fun editProfile(){
        val intent = Intent(this, EditProfileActivity::class.java).also {
            it.putExtra("group08.lab1.fullName", fullNameTV.text.toString())
            it.putExtra("group08.lab1.nickname", nicknameTV.text.toString())
            it.putExtra("group08.lab1.email", emailTV.text.toString())
            it.putExtra("group08.lab1.location", locationTV.text.toString())
        }
        startActivityForResult(intent, 1)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        photoIV = findViewById<ImageView>(R.id.photoImage)
        fullNameTV = findViewById<TextView>(R.id.fullNameTV)
        nicknameTV = findViewById<TextView>(R.id.nicknameTV)
        emailTV = findViewById<TextView>(R.id.emailTV)
        locationTV = findViewById<TextView>(R.id.locationTV)

        sharedPref = this?.getPreferences(Context.MODE_PRIVATE) ?: return

        val userInfoString: String? = sharedPref.getString("userInfo", null)
        if(userInfoString != null){
            val userInfoJSON : JSONObject = JSONObject(userInfoString)
            fullNameTV.text = userInfoJSON.getString(getString(R.string.fullName))
            nicknameTV.text = userInfoJSON.getString(getString(R.string.nickname))
            emailTV.text = userInfoJSON.getString(getString(R.string.email))
            locationTV.text = userInfoJSON.getString(getString(R.string.location))
        }
        else{
            fullNameTV.text = "Full Name"
            nicknameTV.text = "Nickname"
            emailTV.text = "Email address"
            locationTV.text = "Location"
        }



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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.edit_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.editButton -> {
                editProfile()
            }
        }
        
        return true
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 1 && resultCode == Activity.RESULT_OK){
            fullNameTV.text = data?.getStringExtra("group08.lab1.fullName")
            nicknameTV.text = data?.getStringExtra("group08.lab1.nickname")
            emailTV.text = data?.getStringExtra("group08.lab1.email")
            locationTV.text = data?.getStringExtra("group08.lab1.location")

            val jsonObj: JSONObject = JSONObject().put(getString(R.string.fullName), fullNameTV.text.toString()).
            put(getString(R.string.nickname), nicknameTV.text.toString()).
            put(getString(R.string.email), emailTV.text.toString()).
            put(getString(R.string.location), locationTV.text.toString())

            with (sharedPref.edit()) {
                putString("userInfo", jsonObj.toString())
                apply()
            }
        }
        else{
            Toast.makeText(this, "asfsafa", Toast.LENGTH_LONG).show()
        }

    }
}