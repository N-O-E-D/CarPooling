package it.polito.s279434.coroutineapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.RuntimeException

class MainActivity : AppCompatActivity() {
    lateinit var textView: TextView
    lateinit var button: Button

    var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)
        button = findViewById(R.id.button)

        //START COUNT DOWN ON CLICK OF BUTTON
//        button.setOnClickListener {
//            job?.cancel()
//            job = MainScope().launch { // CREATE A COROUTINE FUNCTION (BUILDER FUNCTION)
//                textView.text = "3"
//                //THREAD SLEEP BLOCK MAIN THREAD
//                //DELAY free everithing in stack and go ahead
//                delay(1000) // NOT CALLABLE HERE BECAUSE IT'S SUSPENDABLE
//                textView.text = "2"
//                delay(1000)
//                textView.text = "1"
//                delay(1000)
//                textView.text = "Hello coroutines!"
//            }
//        }

        button.setOnClickListener {
            //job?.cancel()
            if(job == null || job?.isActive == false )
                job = MainScope().launch { // CREATE A COROUTINE FUNCTION (BUILDER FUNCTION)

                    for(i in 10 downTo 1){
                        textView.text = "$i"
                        delay(1000)
                        // THE SUSPENSION REMEMBER THE STATE
                        // WHEN IT CAMES BACK IT WILL REMEMBER THE STATE

//                        if( i== 3) throw RuntimeException("error")
                        // I need to add ExceptionHandler to job
                        // to manage the exception
                    }
                    textView.text = "Hello coroutines!"
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //always destroy (possible) active thread
        job?.cancel()
    }
}