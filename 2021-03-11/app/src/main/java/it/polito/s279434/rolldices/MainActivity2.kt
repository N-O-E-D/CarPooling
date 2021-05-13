package it.polito.s279434.rolldices

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        val btn3 = findViewById<Button>(R.id.button3)

        println(getIntent().extras.toString())

        btn3.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("www.google.it"))

        }
    }
}