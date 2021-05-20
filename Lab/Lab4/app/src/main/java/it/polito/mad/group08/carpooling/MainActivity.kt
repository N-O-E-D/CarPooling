package it.polito.mad.group08.carpooling

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import java.io.File


class MainActivity : AppCompatActivity(), ShowProfileFragment.InfoManager{
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var headerMainInfo: TextView
    private lateinit var headerSecInfo: TextView
    private lateinit var headerProfilePhoto: ImageView

    private lateinit var headerProfilePhotoBitmap: Bitmap
    private lateinit var headerNameText: String
    private lateinit var headerEmailText: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        val drawerLayout: DrawerLayout = findViewById(R.id.drawerLayout)
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(
                setOf(R.id.tripListFragment, R.id.othersTripListFragment,
                R.id.tripsOfInterestListFragment, R.id.boughtTripsListFragment), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navigationView.setupWithNavController(navController)

        headerMainInfo = navigationView.getHeaderView(0).findViewById(R.id.nav_main_info)
        headerSecInfo = navigationView.getHeaderView(0).findViewById(R.id.nav_sec_info)
        headerProfilePhoto = navigationView.getHeaderView(0).findViewById(R.id.nav_profile_photo)

        headerProfilePhoto.setOnClickListener {
            navController.navigate(R.id.showProfileFragment)
            drawerLayout.closeDrawers()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("name", headerMainInfo.text.toString())
        outState.putString("email", headerSecInfo.text.toString())
        val bitmap = (headerProfilePhoto.drawable as BitmapDrawable).bitmap
        val cacheFile = File.createTempFile("cacheImageHeader",null, applicationContext.cacheDir)
        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, cacheFile.outputStream())
        outState.putString("cacheFilePathHeader", cacheFile.name)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val cacheFile = File(applicationContext.cacheDir, savedInstanceState.getString("cacheFilePathHeader")!!)
        val bitmap = BitmapFactory.decodeFile(cacheFile.path)
        if(bitmap!=null)
            headerProfilePhoto.setImageBitmap(bitmap)
        headerMainInfo.text = savedInstanceState.getString("name")
        headerSecInfo.text = savedInstanceState.getString("email")
        cacheFile.delete()
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
}