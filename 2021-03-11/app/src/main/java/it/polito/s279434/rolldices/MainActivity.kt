package it.polito.s279434.rolldices

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.TextView
import it.polito.s279434.rolldices.R
import kotlin.math.floor

class MainActivity : AppCompatActivity() {
    private lateinit var tv1: TextView
    //private var tv2: TextView = TextView(this)
    private lateinit var tv2: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("DEBUG", "onCreate()")
        /*
        // Programmical view Creation
        val tv = TextView(this)
        tv.text = "Bella Zii"
        tv.textSize = 40f;//pixel
        tv.gravity = Gravity.CENTER
        setContentView(tv)
        */
        // XML view Creation
        setContentView(R.layout.activity_main)

        //Hibrid view Creation

        tv1 = findViewById<TextView>(R.id.textView)
        tv2 = findViewById<TextView>(R.id.textView2)

        tv1.text = randomize()
        tv2.text = randomize()


        val btn = findViewById<Button>(R.id.button)

        btn.setOnClickListener {
            tv1.text = randomize()
            tv2.text = randomize()
        }

        val btn2 = findViewById<Button>(R.id.button2)

        btn2.setOnClickListener {
            //val intent = Intent(Intent.ACTION_VIEW, Uri.parse("www.google.it"))
            val intent = Intent(this, MainActivity2::class.java)
            intent.putExtra("URL", "www.google.it")
            startActivity(intent)
        }
    }

    private fun randomize() = (floor(Math.random()*6).toInt() + 1).toString()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("tv1", tv1.text.toString())
        outState.putString("tv2", tv2.text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        tv1.text = savedInstanceState.getString("tv1", "")
        tv2.text = savedInstanceState.getString("tv2", "")
    }

    override fun onStart() {
        super.onStart()
        println("DEBUG - onStart")
    }

    override fun onStop() {
        super.onStop()
        println("DEBUG - onStop")
    }

    override fun onResume() {
        super.onResume()
        println("DEBUG - onResume")
    }

    override fun onPause() {
        super.onPause()
        println("DEBUG - onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        println("DEBUG - onDestroy")
    }
}