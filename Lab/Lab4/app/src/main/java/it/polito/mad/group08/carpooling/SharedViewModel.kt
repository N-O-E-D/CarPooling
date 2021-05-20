package it.polito.mad.group08.carpooling

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SharedViewModel : ViewModel() {

    var auth: FirebaseAuth = Firebase.auth

    private var db = FirebaseFirestore.getInstance()

    private val filter = MutableLiveData<Filter>(Filter())

    val bitmaps: MutableMap<String, Bitmap?> = mutableMapOf()

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

    private val myBookedTrips: MutableLiveData<Resource<List<Trip>>> by lazy {
        MutableLiveData<Resource<List<Trip>>>().also {
            MainScope().launch {
                loadBookedTrips()
            }
        }
    }

    private suspend fun loadBookedTrips() {
        withContext(Dispatchers.IO) {
            try {
                //Return loading at beginning (while DB not finish)
                myBookedTrips.postValue(Resource.Loading())

                //Return Success/Failure when DB finish
                db.collection("trips")
                    .addSnapshotListener { tripsDB, error ->
                        if (error != null)
                            myBookedTrips.postValue(Resource.Failure(error))

                        if (tripsDB != null) {
                            val tmpBookedTrips = mutableListOf<Trip>()
                            for (document in tripsDB.documents) {
                                val tmp = document.toObject(Trip::class.java)!!
                                tmpBookedTrips.add(tmp)
                            }
                            //myBookedTrips.value = Resource.Success(tmpBookedTrips)
                            myBookedTrips.postValue(Resource.Success(tmpBookedTrips))
                        }
                    }
            } catch (e: Exception) {
                myBookedTrips.postValue(Resource.Failure(e))
            }
        }
    }

    fun getMyBookedTrips(): LiveData<Resource<List<Trip>>> {
        return myBookedTrips
    }

    private val bookings: MutableLiveData<MutableList<Booking>> by lazy {
        MutableLiveData<MutableList<Booking>>().also {
            loadBookings()
        }
    }

    // FAKE MAIL USED IN THE APP
    private val user = MutableLiveData<User>()

    fun setUser(user: User) {
        db.collection("users").document(auth.currentUser!!.email!!).addSnapshotListener { userDB, err ->
            /*if (err != null) {

            }*/
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

    fun setUserBitmap(bitmap: Bitmap) {
        this.user.value?.bitmap = bitmap
    }

    fun editUser(user: User) {
        db.collection("users").document(auth.currentUser!!.email!!).set(user)
                .addOnSuccessListener {}
    }

    fun getUser(): MutableLiveData<User> {
        return user
    }

    private val otherUser = MutableLiveData<User>()

    fun setOtherUser(email: String) {
        db.collection("users").document(email).addSnapshotListener { userDB, err ->
            /*if (err != null) {

            }*/
            if (userDB != null) {
                this.otherUser.value = userDB.toObject(User::class.java)
            }
        }
    }

    fun getOtherUser(): MutableLiveData<User> {
        return otherUser
    }

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

    //TODO handle error case
    private fun loadTrips() {         // Do an asynchronous operation to fetch trips.
        db.collection("trips").whereEqualTo("driverEmail", auth.currentUser!!.email)
                .addSnapshotListener { tasks, error ->
                    if (error != null)
                        Log.w("AAAA", "Error getting documents.", error)

                    if (tasks != null) {
                        val tmpTrips = mutableListOf<Trip>()
                        for (document in tasks.documents) {
                            val tmp = document.toObject(Trip::class.java)!!
                            tmpTrips.add(tmp)
                        }
                        trips.value = tmpTrips
                    }
                }
    }

    fun addOrReplaceTrip(newTrip: Trip) {
        if(newTrip.id == "") {
            var max = 0
            for (trip in trips.value!!) {
                val num = trip.id.split("_").last().toInt()
                if (num != max) {
                    //max = num
                    break
                } else max++
            }
            newTrip.id = "${auth.currentUser!!.email!!}_${max}"
        }

        db.collection("trips")
                .document(newTrip.id)
                .set(newTrip)
                .addOnSuccessListener {
                }
                .addOnFailureListener {
                }
    }

    fun getTrips(): LiveData<MutableList<Trip>> {
        return trips
    }

    private fun loadOthersTrips() {         // Do an asynchronous operation to fetch trips.
        db.collection("trips")
                .whereNotEqualTo("driverEmail", auth.currentUser!!.email)
                .addSnapshotListener { tasks, error ->
                    if (error != null)
                        Log.w("OTHERTRIPSAAAA", "Error getting documents.", error)

                    if (tasks != null) {
                        val tmpTrips = mutableListOf<Trip>()
                        tasks.documents.forEachIndexed { _, document ->
                            val tmp = document.toObject(Trip::class.java)!!
                            if (othersTrips.value != null) {
                                for (trip in othersTrips.value!!) {
                                    if (tmp.id == trip.id &&
                                            tmp.carPhotoPath != trip.carPhotoPath) {
                                        bitmaps[tmp.id] = null
                                        break
                                    }
                                }
                            }
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
                    }
                }
    }

    fun getOthersTrips(): LiveData<MutableList<Trip>> {
        return othersTrips
    }

    fun deleteTrip(tripID: String) {
        db.collection("trips").document(tripID).delete().addOnSuccessListener {
        }
                .addOnFailureListener {
                }
    }

    fun userIsInterested(tripToCheck: Trip): Boolean {
        val myself = User(email = auth.currentUser!!.email!!, name = auth.currentUser!!.displayName!!)
        return tripToCheck.interestedUsers.contains(myself)
    }

    fun removeFromBookings(bookingID: String) {
        db.collection("bookings")
                .document(bookingID)
                .delete()
                .addOnSuccessListener {
                }.addOnFailureListener {
                }
    }

    fun updateTripInterestedUser(tripToUpdate: Trip, isInterested: Boolean, userToUpdate: User?) {
        val userTarget: User?
        val myself = User(email = auth.currentUser!!.email!!, name = auth.currentUser!!.displayName!!)
        userTarget = userToUpdate ?: myself
        var accepted = false

        if (isInterested)
            tripToUpdate.interestedUsers.add(userTarget)
        else {
            tripToUpdate.interestedUsers.map {
                if (it.email == userTarget.email && it.isAccepted) {
                    it.isAccepted = false //it's useless because it will be removed from the list
                    accepted = true
                    removeFromBookings("${tripToUpdate.id}_${userTarget.email}")
                }
            }
            tripToUpdate.interestedUsers.remove(userTarget)
        }

        if (accepted)
            tripToUpdate.availableSeats++

        db.collection("trips")
                .document(tripToUpdate.id)
                .set(tripToUpdate)
                .addOnSuccessListener {
                }
                .addOnFailureListener {
                }
    }

    fun acceptUser(targetTrip: Trip, targetUser: User) {
        val booking = Booking(targetTrip.id, targetUser.email)
        db.collection("bookings")
                .document("${targetTrip.id}_${targetUser.email}")
                .set(booking)
                .addOnSuccessListener {
                    targetTrip.interestedUsers.map {
                        if (it.email == targetUser.email)
                            it.isAccepted = true
                    }

                    targetTrip.availableSeats--

                    if (targetTrip.availableSeats <= 0) {
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
                            }
                            .addOnFailureListener {
                            }
                }
                .addOnFailureListener {
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
        val possibleBooking = Booking(tripID, auth.currentUser!!.email!!)
        return if (bookings.value != null)
            bookings.value?.contains(possibleBooking)!!
        else
            false
    }

    fun downloadMetadataPhoto(path: String): StorageMetadata {
        val storage = Firebase.storage
        val storageRef = storage.reference
        val testRef = storageRef.child(path)
        return Tasks.await(testRef.metadata)
    }

    fun downloadPhoto(size: Long, path: String): Bitmap  {
        val storage = Firebase.storage
        val storageRef = storage.reference
        val testRef = storageRef.child(path)
        val ONE_MEGABYTE: Long = 1024 * 1024
        val byteArray = Tasks.await(testRef.getBytes(ONE_MEGABYTE))
        return BitmapFactory.decodeByteArray(byteArray, 0, size.toInt())
    }

}

data class User(val name: String = "", val nickname: String = "",
                val email: String = "", val location: String = "",
                val phone_number: String = "", val rating: Float = 0f,
                var isAccepted: Boolean = false, var bitmap: Bitmap? = null)

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

sealed class Resource<out T> {
    class Loading<out T> : Resource<T>()
    data class Success<out T>(val data: T) : Resource<T>()
    data class Failure<out T>(val throwable: Throwable) : Resource<T>()
}

///**
// * Observable manager for saving the cart's resource information.
// */
//class TripManager : LiveData<Resource<Trip?>>() {
//
//    init {
//        value = Resource.Success(null)
//    }
//
//    /**
//     * Set the [Trip] value and notifies observers.
//     */
//    internal fun set(trip: Trip) {
//        val resource = Resource.Success(trip)
//        postValue(resource)
//    }
//
//    internal fun clear() {
//        val resource = Resource.Success(null)
//        postValue(resource)
//    }
//
//    internal fun loading() {
//        val resource: Resource<Trip?> = Resource.Loading()
//        postValue(resource)
//    }
//
//    internal fun error(t: Throwable) {
//        val resource: Resource<Trip?> = Resource.Failure(t)
//        postValue(resource)
//    }
//}