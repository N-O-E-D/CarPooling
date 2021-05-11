package it.polito.mad.group08.carpooling

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.firestore.FirebaseFirestore


class SharedViewModel : ViewModel() {
    private var db = FirebaseFirestore.getInstance()

    private val filter = MutableLiveData<Filter>(Filter())

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

    private val bookings: MutableLiveData<MutableList<Booking>> by lazy {
        MutableLiveData<MutableList<Booking>>().also {
            loadBookings()
        }
    }

    // FAKE MAIL USED IN THE APP
    private val user = MutableLiveData<User>()

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

    private val otherUser = MutableLiveData<User>()

    fun setOtherUser(email: String) {
        db.collection("users").document(email).addSnapshotListener { userDB, err ->
            if (err != null)
                Log.d("BBBB", "Error getting documents.", err)
            if (userDB != null) {
                this.otherUser.value = userDB.toObject(User::class.java)
            }
        }
    }

    fun getOtherUser(): MutableLiveData<User> {
        return otherUser
    }

    // MAIL FROM WHICH YOU ARE LOGGED IN
    private val account = MutableLiveData<GoogleSignInAccount>()

    fun setAccount(account: GoogleSignInAccount) {
        this.account.value = account
    }

    fun getAccount(): GoogleSignInAccount {
        return account.value!!
    }

    //TODO I'm not sure position should be managed by means of sharedViewModel. Bundle is better
    private val position = MutableLiveData(0)

    fun getPosition(): LiveData<Int> {
        return position
    }

    fun setPosition(pos: Int) {
        position.value = pos
    }


    fun setFilter(filter: Filter) {
        this.filter.value = filter
        loadOthersTrips()
    }

    fun getFilter(): Filter? {
        return filter.value
    }

    //TODO merge loadTrips() with loadOtherTrips()
    //TODO handle error case
    private fun loadTrips() {         // Do an asynchronous operation to fetch trips.
        db.collection("trips").whereEqualTo("driverEmail", account.value?.email)
                .addSnapshotListener { tasks, error ->
                    if (error != null)
                        Log.w("AAAA", "Error getting documents.", error)

                    if (tasks != null) {
                        val tmpTrips = mutableListOf<Trip>()
                        for (document in tasks.documents) {
                            Log.d("AAAA", document.id + " => " + document.data)
                            val tmp = document.toObject(Trip::class.java)!!
                            Log.d("AAAA", tmp.toString())
                            tmpTrips.add(tmp)
                        }
                        trips.value = tmpTrips
                    } else
                        Log.d("AAAA", "success but empty trips")
                }
    }

    fun addOrReplaceTrip(newTrip: Trip) {
        newTrip.id = "${account.value?.email!!}_${position.value}"
        db.collection("trips")
                .document(newTrip.id)
                .set(newTrip)
                .addOnSuccessListener {
                    Log.d("AAAA", "DATO AGGIORNATO")
                }
                .addOnFailureListener {
                    Log.d("AAAA", "ERROREEE")
                }
    }

    fun getTrips(): LiveData<MutableList<Trip>> {
        return trips
    }

    private fun loadOthersTrips() {         // Do an asynchronous operation to fetch trips.
        db.collection("trips")
                .whereNotEqualTo("driverEmail", account.value?.email)
                .addSnapshotListener { tasks, error ->
                    if (error != null)
                        Log.w("OTHERTRIPSAAAA", "Error getting documents.", error)

                    if (tasks != null) {
                        val tmpTrips = mutableListOf<Trip>()
                        for (document in tasks.documents) {
                            Log.d("OTHERTRIPSAAAA", document.id + " => " + document.data)
                            val tmp = document.toObject(Trip::class.java)!!
                            Log.d("OTHERTRIPSAAAA", tmp.toString())
                            tmpTrips.add(tmp)
                        }
                        //othersTrips.value = tmpTrips
                        othersTrips.value = tmpTrips.filter {
                            if (filter.value?.departureLocation != null)
                                it.checkPoints[0].location == filter.value?.departureLocation
                            else
                                true
                        }.filter {
                            if (filter.value?.arrivalLocation != null)
                                it.checkPoints[it.checkPoints.size - 1].location == filter.value?.arrivalLocation
                            else
                                true
                        }.filter {
                            if (filter.value?.departureDate != null)
                                it.checkPoints[0].timestamp.subSequence(0, 10) == filter.value?.departureDate
                            else
                                true
                        }.filter {
                            if (filter.value?.arrivalDate != null)
                                it.checkPoints[it.checkPoints.size - 1].timestamp.subSequence(0, 10) == filter.value?.arrivalDate
                            else
                                true
                        }.filter {
                            it.seatPrice <= filter.value?.maxPrice!! && it.seatPrice >= filter.value?.minPrice!!
                        }.toMutableList()
                    } else
                        Log.d("OTHERTRIPSAAAA", "success but empty trips")
                }
    }

    fun getOthersTrips(): LiveData<MutableList<Trip>> {
        return othersTrips
    }

    fun deleteTrip(tripID: String){
        db.collection("trips").document(tripID).delete().addOnSuccessListener {
            Log.d("AAAA", "TRIP DELETED SUCCESSFULLY")
        }
        .addOnFailureListener {
            Log.d("AAAA", "ERROR DELETING TRIP")
         }
    }

    fun userIsInterested(tripToCheck: Trip): Boolean {
        val myself = User(email = account.value?.email!!, name = account.value?.displayName!!)
        return tripToCheck.interestedUsers.contains(myself)
    }

    fun removeFromBookings(bookingID: String){
        db.collection("bookings")
                .document(bookingID)
                .delete()
                .addOnSuccessListener {
                    Log.d("AAAA", "removeFromBooking $bookingID with success")
                }.addOnFailureListener {
                    Log.d("AAAA", "removeFromBooking $bookingID with success")
                }
    }

    fun updateTripInterestedUser(tripToUpdate: Trip, isInterested: Boolean, userToUpdate: User?) {
        var userTarget: User? = null
        val myself = User(email = account.value?.email!!, name = account.value?.displayName!!)
        userTarget = userToUpdate ?: myself
        var accepted = false

        if (isInterested)
            tripToUpdate.interestedUsers.add(userTarget)
        else {
            tripToUpdate.interestedUsers.map {
                if(it.email == userTarget.email && it.isAccepted){
                    it.isAccepted = false //it's useless because it will be removed from the list
                    accepted = true
                    removeFromBookings("${tripToUpdate.id}_${userTarget.email}")
                }
            }
            tripToUpdate.interestedUsers.remove(userTarget)
        }
        if(accepted){
            tripToUpdate.availableSeats++
        }

        if(accepted)
            tripToUpdate.availableSeats++

        db.collection("trips")
                .document(tripToUpdate.id)
                .set(tripToUpdate)
                .addOnSuccessListener {
                    Log.d("AAAA", "updateTripInterestedUser with success")
                }
                .addOnFailureListener {
                    Log.d("AAAA", "updateTripInterestedUser with error")
                }
    }

    fun acceptUser(targetTrip: Trip, targetUser: User) {
        val booking = Booking(targetTrip.id, targetUser.email)
        db.collection("bookings")
                .document("${targetTrip.id}_${targetUser.email}")
                .set(booking)
                .addOnSuccessListener {
                    Log.d("AAAA", "acceptUser with success")
                    targetTrip.interestedUsers.map {
                        if(it.email == targetUser.email)
                            it.isAccepted = true
                    }

                    targetTrip.availableSeats--

                    if(targetTrip.availableSeats <= 0){
                        val tmpInterestedUsers = mutableListOf<User>()
                        targetTrip.interestedUsers.map {
                            if (it.isAccepted)
                                tmpInterestedUsers.add(it)
                        }
                        targetTrip.interestedUsers = tmpInterestedUsers
                    }

                    db.collection("trips")
                            .document(targetTrip.id)
                            .set(targetTrip)
                            .addOnSuccessListener {
                                Log.d("AAAA", "update available seat with success")
                            }
                            .addOnFailureListener {
                                Log.d("AAAA", "Error in update available seat")
                            }
                }
                .addOnFailureListener {
                    Log.d("AAAA", "acceptUser with error")
                }
    }

    private fun loadBookings() {
        db.collection("bookings")
                .addSnapshotListener { bookingsDB, error ->
                    if (error != null)
                        Log.w("AAAA", "Error loadBookings", error)

                    if (bookingsDB != null) {
                        val tmpBookings = mutableListOf<Booking>()
                        for (document in bookingsDB.documents) {
                            val tmp = document.toObject(Booking::class.java)!!
                            tmpBookings.add(tmp)
                        }
                        bookings.value = tmpBookings
                    }
                }
    }

    fun getBookings(): LiveData<MutableList<Booking>> {
        return bookings
    }

    fun bookingIsAccepted(tripID: String): Boolean {
        val possibleBooking = Booking(tripID, account.value?.email!!)
        Log.d("AAABBB", "$possibleBooking")
        return bookings.value?.contains(possibleBooking)!!

    }

}

data class User(val name: String = "", val nickname: String = "",
                val email: String = "", val location: String = "",
                val phone_number: String = "", val rating: Float = 0f,
                var isAccepted: Boolean = false)

data class CheckPoint(var location: String = "", var timestamp: String = "")

data class Trip(var id: String = "",
                var carPhotoPath: String? = "",
                var carDescription: String = "",
                var driverName: String = "",
                var driverEmail: String = "",
                var driverRate: Float = 0f,
                var checkPoints: MutableList<CheckPoint> = mutableListOf(),
                var estimatedDuration: String = "",
                var availableSeats: Int = 0,
                var seatPrice: Float = 0f,
                var description: String = "",
                var interestedUsers: MutableList<User> = mutableListOf()
)

data class Booking(var tripID: String = "", var userEmail: String = "")

data class Filter(var departureLocation: String? = null,
                  var arrivalLocation: String? = null,
                  var departureDate: String? = null,
                  var arrivalDate: String? = null,
                  var minPrice: Float = 0f,
                  var maxPrice: Float = 100f)