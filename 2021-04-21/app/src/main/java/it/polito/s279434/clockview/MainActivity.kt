package it.polito.s279434.clockview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val clock = findViewById<ClockView>(R.id.clockView)
        val button = findViewById<Button>(R.id.button)

        button.setOnClickListener {
            clock.m = clock.m+1
            if(clock.m==0)
                clock.h = clock.h+1
        }
    }
}