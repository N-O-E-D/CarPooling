package it.polito.mad.group08.carpooling

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore


class SharedViewModel : ViewModel() {
    private var db = FirebaseFirestore.getInstance()

    private val trips: MutableLiveData<MutableList<TripListFragment.Trip>> by lazy {
        MutableLiveData<MutableList<TripListFragment.Trip>>().also {
            loadTrips()
        }
    }

    private val position = MutableLiveData(0)

    fun addOrReplaceTrip(newTrip: TripListFragment.Trip) {
        db.collection("trips")
                .document("${newTrip.driverName}_${position.value}")
                .set(newTrip)
                .addOnSuccessListener {
                    Log.d("AAAA", "DATO AGGIORNATO")
                }
    }

    fun getTrips() : LiveData<MutableList<TripListFragment.Trip>>{
        return trips
    }

    fun getPosition() : LiveData<Int>{
        return position
    }

    fun setPosition(pos: Int){
        position.value = pos
    }

    private fun loadTrips(){         // Do an asynchronous operation to fetch trips.
        db.collection("trips")
                .addSnapshotListener { tasks, error ->
                    if(error != null)
                        Log.w("AAAA", "Error getting documents.", error)

                    if(tasks != null) {
                        val tmpTrips = mutableListOf<TripListFragment.Trip>()
                        for (document in tasks.documents) {
                            Log.d("AAAA", document.id + " => " + document.data)
                            val tmp = document.toObject(TripListFragment.Trip::class.java)!!
                            Log.d("AAAA", tmp.toString())
                            tmpTrips.add(tmp)
                        }
                        trips.value = tmpTrips
                    }else
                        Log.d("AAAA", "success but empty trips")
                }
    }
}