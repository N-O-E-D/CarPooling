package it.polito.s279434.myfirestoreapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore


class MainActivity : AppCompatActivity() {
    var counter = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //PART 1
        val tv = findViewById<TextView>(R.id.tv)
        val et = findViewById<EditText>(R.id.et)
        val b = findViewById<Button>(R.id.button)
        val db = FirebaseFirestore.getInstance()

        //POPULATING DB
        val addresses = db.collection("addresses")
        addresses.document("a1")
            .set(Address("roadName",
                        3,
                        "city",
                        listOf("Alpha", "Beta", "Gamma")
            ))
            .addOnSuccessListener {
                Toast.makeText(this, "Address saved", Toast.LENGTH_SHORT)
                    .show()
            }

        val trips = db.collection("trips")
        val trip = trips.document("trip1")

        //READ FROM DB
        trip.addSnapshotListener{   //called many time (once for each modification in DB)
            value, error ->
                if (error != null) throw error
                if (value != null){
                    tv.text = value["message"].toString()
                }
        }

        //WRITE ON DB
        b.setOnClickListener {
            val message = et.text.toString()
            trip.set(mapOf("message" to message))
        }

        //PART 2
//        val b1  = findViewById<Button>(R.id.button1)
//        val b2  = findViewById<Button>(R.id.button2)
//        val tv  = findViewById<TextView>(R.id.tv)
//
//        b1.setOnClickListener {
//            //simulate very slow operation
//            Thread.sleep(4000)
//            counter++
//            tv.text = "Counter is $counter"
//        }
//
//        b2.setOnClickListener {
//            Thread.sleep(5000)
//            counter--
//            tv.text = "Counter is $counter"
//        }
    }

    data class Address(
        val street: String,
        val number: Int,
        val city: String,
        val names: List<String>)
}