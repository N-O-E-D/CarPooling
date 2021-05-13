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
    //val h = Handler() // Created when the main activity is created -> in the main thread -> has reference to main queue
    lateinit var tv: TextView
    var n = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tv = findViewById<TextView>(R.id.tv)
        val button = findViewById<Button>(R.id.button)
        val button2 = findViewById<Button>(R.id.button2)

        val looper = MyLooper() //creating new thread
        looper.start()
        val h = looper.getHandler()

        button.setOnClickListener {
            // Without sleep it appears 3
//            tv.text = "1"
//            tv.text = "2"
//            tv.text = "3"

            // with sleep it appers 3 again
//            tv.text = "1"
//            Log.d("looper", "1")
//            Thread.sleep(2000)
//            Log.d("looper", "sleep ended")
//            tv.text = "2"
//            Log.d("looper", "2")
//            Thread.sleep(2000)
//            Log.d("looper", "sleep ended 2")
//            tv.text = "3"
//            Log.d("looper", "3")

            // 1 then 2 then 3
//            h.post(Runnable{tv.text="1"})
//            h.postDelayed(Runnable{tv.text="2"}, 1000L)
//            h.postDelayed(Runnable{tv.text="3"}, 2000L)

            // do it every X milli
//            count()

            // start new thread
            h.postShortTask(
                    Runnable { tv.text = "Short Task" }, this
            )
        }

        button2.setOnClickListener {
            h.postLongTask(
                    Runnable { tv.text = "Long Task" }, this
            )
        }
    }

    fun count() {
        tv.text = "$n"
        n++
        tv.postDelayed(Runnable{count()}, 1000)
    }
}

class MyHandler: Handler(){
    fun postShortTask(r: Runnable, a: Activity){
        val m = Message.obtain()
        m.what = 1
        m.obj = Pair(r, a)
        sendMessage(m)
    }

    fun postLongTask(r: Runnable, a: Activity){
        val m = Message.obtain()
        m.what = 2
        m.obj = Pair(r, a)
        sendMessage(m)
    }

    override fun handleMessage(msg: Message) {
        when (msg.what){
            1 -> {
                Thread.sleep(1000)
                Log.d("looper", "Performed a short task")
                //Looper.myLooper()
                //Looper.getMainLooper()
                val p = msg.obj as Pair<Runnable, Activity>
                p.second.runOnUiThread(p.first)
            }
            2 ->{
                Thread.sleep(5000)
                Log.d("looper", "Performed a long task")
                val p = msg.obj as Pair<Runnable, Activity>
                p.second.runOnUiThread(p.first)
            }
            else -> super.handleMessage(msg)
        }
//        msg.recycle() // someone could reuse it
    }
}

class MyLooper : Thread(){
    private var cdl = CountDownLatch(1)
    private lateinit var h: MyHandler

    override fun run() {
//        super.run()
        Looper.prepare()
        h = MyHandler()
        cdl.countDown()
        Looper.loop()
    }

    fun getHandler(): MyHandler{
        cdl.await()
        return h
    }
}