package it.polito.mad.group08.carpooling

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView


class MainActivity : AppCompatActivity(), ShowProfileFragment.InfoManager{
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var headerMainInfo: TextView
    private lateinit var headerSecInfo: TextView
    private lateinit var headerProfilePhoto: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        val drawerLayout: DrawerLayout = findViewById(R.id.drawerLayout)
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(
                setOf(R.id.tripListFragment, R.id.othersTripListFragment), drawerLayout
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