package it.polito.mad.group08.carpooling

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView

class ShowProfileActivity : AppCompatActivity() {
    private lateinit var photoIV: ImageView
    private lateinit var fullNameTV : TextView
    private lateinit var nicknameTV : TextView
    private lateinit var emailTV : TextView
    private lateinit var locationTV : TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        photoIV = findViewById<ImageView>(R.id.photoImage)
        fullNameTV = findViewById<TextView>(R.id.fullNameTV)
        nicknameTV = findViewById<TextView>(R.id.nicknameTV)
        emailTV = findViewById<TextView>(R.id.emailTV)
        locationTV = findViewById<TextView>(R.id.locationTV)

        fullNameTV.text = "Full Name"
        nicknameTV.text = "Nickname"
        emailTV.text = "Email address"
        locationTV.text = "Location"
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
        menuInflater.inflate(R.menu.menu_bar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.editButton -> {
                fullNameTV.text = "Domenico Bini"
                nicknameTV.text = "polyhedral artist"
                emailTV.text = "domenicobini@gmail.com"
                locationTV.text = "Aosta"
            }

        }

        return true
    }
}