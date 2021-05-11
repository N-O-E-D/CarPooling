package it.polito.mad.group08.carpooling

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.material.navigation.NavigationView
import org.json.JSONObject
import java.io.FileNotFoundException


class MainActivity : AppCompatActivity(), ShowProfileFragment.InfoManager{
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var headerMainInfo: TextView
    private lateinit var headerSecInfo: TextView
    private lateinit var headerProfilePhoto: ImageView
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        val drawerLayout: DrawerLayout = findViewById(R.id.drawerLayout)
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(
                setOf(/*R.id.showProfileFragment,*/ R.id.tripListFragment, R.id.othersTripListFragment), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navigationView.setupWithNavController(navController)

        headerMainInfo = navigationView.getHeaderView(0).findViewById(R.id.nav_main_info)
        headerSecInfo = navigationView.getHeaderView(0).findViewById(R.id.nav_sec_info)
        headerProfilePhoto = navigationView.getHeaderView(0).findViewById(R.id.nav_profile_photo)
        sharedPref = getPreferences(Context.MODE_PRIVATE)!!

        headerProfilePhoto.setOnClickListener {
            navController.navigate(R.id.showProfileFragment)
            drawerLayout.closeDrawers()
        }

        val jsonObject = sharedPref.getString("profile", null)
        if (jsonObject != null) {
            val deserializedJSON = JSONObject(jsonObject)
            updateTexts(deserializedJSON.getString("fullName"), deserializedJSON.getString("email"))

        }
        retrieveUserImage()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun updateTexts(main: String, secondary: String) {
        headerMainInfo.text = main
        headerSecInfo.text = secondary
    }

    override fun updatePhoto(bitmap: Bitmap) {
        headerProfilePhoto.setImageBitmap(bitmap)
    }

    private fun retrieveUserImage() {
        try {
            applicationContext.openFileInput("userProfileImage").use {
                val bitmap: Bitmap? = BitmapFactory.decodeStream(it)
                if (bitmap != null) {
                    headerProfilePhoto.setImageBitmap(bitmap)
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }
}