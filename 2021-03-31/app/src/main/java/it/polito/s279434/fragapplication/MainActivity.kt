package it.polito.s279434.fragapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.commit

class MainActivity : AppCompatActivity(), InputFragment.InputManager {
    lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) //reserve space for all elements

        textView = findViewById(R.id.textView)

        if(savedInstanceState == null){ //first time i'm running on Create

            val button = findViewById<Button>(R.id.button2)
            button.setOnClickListener {
                val f = supportFragmentManager.findFragmentByTag("second")
                if(f==null){
                    supportFragmentManager.commit {
                        add(R.id.fragmentView2, InputFragment(), "second")
//                        addToBackStack("secondIn")
                    }
                }else{
                    supportFragmentManager.commit {
                        remove(f)
//                        addToBackStack("secondOut")
                    }
                }

            }

            //add fragment to our activity
            val inputFragment = InputFragment() //not possible with activity

            supportFragmentManager.commit {
                add(R.id.fragmentView, inputFragment)
            }
        }
    }

    override fun onValue(value: String) {
        textView.setText(value)
    }
}