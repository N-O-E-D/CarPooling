package it.polito.mad.group08.carpooling

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject

class ShowProfileActivity : AppCompatActivity() {
    private lateinit var photoIV: ImageView
    private lateinit var fullNameTV : TextView
    private lateinit var nicknameTV : TextView
    private lateinit var emailTV : TextView
    private lateinit var locationTV : TextView
    //TODO RatingBar for user status

    private lateinit var sharedPref : SharedPreferences

    val REQUEST_CODE_EDIT_ACTIVITY = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //TODO a06widget - ellipsize() replace exceeded text with ...
        //TODO a06widget - autolink-email insert a link in the textView
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

        val jsonObject = sharedPref.getString("userInfo", jsonObjectDefault.toString())

        val deserializedJSON = JSONObject(jsonObject)

        fullNameTV.text = deserializedJSON.getString("fullName")
        nicknameTV.text = deserializedJSON.getString("nickname")
        emailTV.text = deserializedJSON.getString("email")
        locationTV.text = deserializedJSON.getString("location")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("group08.lab1.fullName", fullNameTV.text.toString())
        outState.putString("group08.lab1.nickname", nicknameTV.text.toString())
        outState.putString("group08.lab1.email", emailTV.text.toString())
        outState.putString("group08.lab1.location", locationTV.text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        fullNameTV.text = savedInstanceState.getString("group08.lab1.fullName")
        nicknameTV.text = savedInstanceState.getString("group08.lab1.nickname")
        emailTV.text = savedInstanceState.getString("group08.lab1.email")
        locationTV.text = savedInstanceState.getString("group08.lab1.location")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.option_menu_edit_profile, menu)
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

    private fun editProfile(){
        val intent = Intent(this, EditProfileActivity::class.java)
                .also {
                    //it.putExtra("group08.lab1.currentPhotoPath", R.drawable.photo_default)
                    it.putExtra("group08.lab1.fullName", fullNameTV.text.toString())
                    it.putExtra("group08.lab1.nickname", nicknameTV.text.toString())
                    it.putExtra("group08.lab1.email", emailTV.text.toString())
                    it.putExtra("group08.lab1.location", locationTV.text.toString())
                }
        startActivityForResult(intent,REQUEST_CODE_EDIT_ACTIVITY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==REQUEST_CODE_EDIT_ACTIVITY){
            if(resultCode == Activity.RESULT_OK && data != null){
                fullNameTV.text = data.getStringExtra("group08.lab1.fullName")
                nicknameTV.text = data.getStringExtra("group08.lab1.nickname")
                emailTV.text = data.getStringExtra("group08.lab1.email")
                locationTV.text = data.getStringExtra("group08.lab1.location")

                sharedPref = this.getPreferences(Context.MODE_PRIVATE)
                with (sharedPref.edit()) {
                    val jsonObject = JSONObject()
                    jsonObject.put("fullName", fullNameTV.text)
                    jsonObject.put("nickname", nicknameTV.text)
                    jsonObject.put("email", emailTV.text)
                    jsonObject.put("location", locationTV.text)


                    Log.d("TEST", "OBJECT: ${jsonObject.toString()}")
                    putString("userInfo", jsonObject.toString())
                    apply()
                }
            }else{
                Toast.makeText(applicationContext, "Error in editing", Toast.LENGTH_LONG).show()
            }
        }
    }
}