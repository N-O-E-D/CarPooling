package it.polito.mad.group08.carpooling

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.firestore.FirebaseFirestore


class SharedViewModel : ViewModel() {
    private var db = FirebaseFirestore.getInstance()

    private val trips: MutableLiveData<MutableList<TripListFragment.Trip>> by lazy {
        MutableLiveData<MutableList<TripListFragment.Trip>>().also {
            loadTrips()
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


    fun addOrReplaceTrip(newTrip: TripListFragment.Trip) {
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

data class User(val name:String = "", val nickname: String = "",
                val email: String = "", val location: String = "",
                val phone_number: String = "", val rating: Float = 0f)