package it.polito.s279434.livedataapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.observe
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    val ld = MutableLiveData<String>()

    val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tv = findViewById<TextView>(R.id.tv)
        val button = findViewById<Button>(R.id.button)

        ld.observe(this) {
            if(it.length <= 20)
                tv.text = it
            else
                tv.text = it.substring(1..20)
        }

//        button.setOnClickListener {
//            scope.launch {
//                delay(3000)
//                ld.value = "Button pressed"
//            }
//        }

        button.setOnClickListener {
            GlobalScope.launch {
                var v = 1.toBigInteger()
                for (i in 2..2000){
                    v = v * i.toBigInteger()
                    //In global scope i can access directly in ld.value
                    ld.postValue(v.toString())
                }
            }
        }

    }
}