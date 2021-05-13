package it.polito.s279434.looperapplication

import android.app.Activity
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import java.util.concurrent.CountDownLatch

class MainActivity : AppCompatActivity() {
    lateinit var tv: TextView
    lateinit var taskProcessor: TaskProcessor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tv = findViewById<TextView>(R.id.tv)
        val button = findViewById<Button>(R.id.button)
        val button2 = findViewById<Button>(R.id.button2)


        taskProcessor = TaskProcessor("tp")
        taskProcessor.start()

        val handler = taskProcessor.handler

        button.setOnClickListener {
            // start new thread
            handler.postShortTask(
                    Runnable { tv.text = "Short Task" }, this
            )
        }

        button2.setOnClickListener {
            handler.postLongTask(
                    Runnable { tv.text = "Long Task" }, this
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        taskProcessor.quit()
    }
}

class MyHandler(looper: Looper): Handler(looper){
    val mainHandler = Handler(Looper.getMainLooper())

    fun postShortTask(r: Runnable, a: Activity){
        val m = Message.obtain()
        m.what = 1
        m.obj = r
        sendMessage(m)
    }

    fun postLongTask(r: Runnable){
        val m = Message.obtain()
        m.what = 2
        m.obj = r
        sendMessage(m)
    }

    override fun handleMessage(msg: Message) {
        when (msg.what){
            1 -> {
                Thread.sleep(1000)
                Log.d("looper", "Performed a short task")
                mainHandler.post(msg.obj as Runnable)
            }
            2 ->{
                Thread.sleep(5000)
                //throw RuntimeException("failure")
                Log.d("looper", "Performed a long task")
                mainHandler.post(msg.obj as Runnable)
            }
            else -> super.handleMessage(msg)
        }
    }
}


class TaskProcessor(name: String)

class MyHandlerThread(name: String) : HandlerThread(name){
    private val cdl = CountDownLatch(1)
    val handler: MyHandler by lazy {
        cdl.await()
        MyHa
    }
}

//THIS SOLUTION NOT PERMIT TO CANCEL EVENT FROM THE QUEUE