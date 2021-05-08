package it.polito.mad.group08.carpooling

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.firestore.FirebaseFirestore


class SharedViewModel : ViewModel() {
    private var db = FirebaseFirestore.getInstance()

    private val trips: MutableLiveData<MutableList<Trip>> by lazy {
        MutableLiveData<MutableList<Trip>>().also {
            loadTrips()
        }
    }

    private val othersTrips: MutableLiveData<MutableList<Trip>> by lazy {
        MutableLiveData<MutableList<Trip>>().also {
            loadOthersTrips()
        }
    }

    private val user = MutableLiveData<User>()

    private val position = MutableLiveData(0)

    private val account = MutableLiveData<GoogleSignInAccount>()

    fun setUser(user: User) {
        db.collection("users").document(account.value?.email!!).addSnapshotListener { userDB, err ->
            if (err != null)
                Log.d("BBBB", "Error getting documents.", err)
            if (userDB != null) {
                if (userDB.data == null) {
                    db.collection("users").document(user.email).set(user)
                        .addOnSuccessListener { this.user.value = user }
                } else {
                    this.user.value = userDB.toObject(User::class.java)
                }
            }
        }
    }

    fun editUser(user: User) {
        db.collection("users").document(account.value?.email!!).set(user)
            .addOnSuccessListener {}
    }

    fun getUser(): MutableLiveData<User> {
        return user
    }

    fun setAccount(account: GoogleSignInAccount) {
        this.account.value = account
    }

    fun getAccount(): GoogleSignInAccount {
        return account.value!!
    }


    fun addOrReplaceTrip(newTrip: Trip) {
        db.collection("trips")
                .document("${account.value?.email!!}_${position.value}")
                .set(newTrip)
                .addOnSuccessListener {
                    Log.d("AAAA", "DATO AGGIORNATO")
                }
                .addOnFailureListener {
                    Log.d("AAAA", "ERROREEE")
                }
    }

    fun getTrips() : LiveData<MutableList<Trip>>{
        return trips
    }

    fun getOthersTrips() : LiveData<MutableList<Trip>>{
        return othersTrips
    }

    fun getPosition() : LiveData<Int>{
        return position
    }

    fun setPosition(pos: Int){
        position.value = pos
    }

    private fun loadTrips(){         // Do an asynchronous operation to fetch trips.
        db.collection("trips").whereEqualTo("driverEmail",account.value?.email)
                .addSnapshotListener { tasks, error ->
                    if(error != null)
                        Log.w("AAAA", "Error getting documents.", error)

                    if(tasks != null) {
                        val tmpTrips = mutableListOf<Trip>()
                        for (document in tasks.documents) {
                            Log.d("AAAA", document.id + " => " + document.data)
                            val tmp = document.toObject(Trip::class.java)!!
                            Log.d("AAAA", tmp.toString())
                            tmpTrips.add(tmp)
                        }
                        trips.value = tmpTrips
                    }else
                        Log.d("AAAA", "success but empty trips")
                }
    }


    private fun loadOthersTrips(){         // Do an asynchronous operation to fetch trips.
        db.collection("trips").whereNotEqualTo("driverEmail",account.value?.email)
                .addSnapshotListener { tasks, error ->
                    if(error != null)
                        Log.w("OTHERTRIPSAAAA", "Error getting documents.", error)

                    if(tasks != null) {
                        val tmpTrips = mutableListOf<Trip>()
                        for (document in tasks.documents) {
                            Log.d("OTHERTRIPSAAAA", document.id + " => " + document.data)
                            val tmp = document.toObject(Trip::class.java)!!
                            Log.d("OTHERTRIPSAAAA", tmp.toString())
                            tmpTrips.add(tmp)
                        }
                        othersTrips.value = tmpTrips
                    }else
                        Log.d("OTHERTRIPSAAAA", "success but empty trips")
                }
    }
}

data class User(val name:String = "", val nickname: String = "",
                val email: String = "", val location: String = "",
                val phone_number: String = "", val rating: Float = 0f)

data class CheckPoint(var location: String = "", var timestamp: String = "")

data class Trip(var carPhotoPath: String? = "",
                var carDescription: String = "",
                var driverName: String = "",
                var driverEmail: String = "",
                var driverRate: Float = 0f,
                var checkPoints: MutableList<CheckPoint> = mutableListOf(),
                var estimatedDuration: String = "",
                var availableSeats: Int = 0,
                var seatPrice: Float = 0f,
                var description: String = ""
)