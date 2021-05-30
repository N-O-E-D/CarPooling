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
import java.io.ByteArrayOutputStream
import kotlin.collections.set
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.OverlayItem

const val ONE_MEGABYTE: Long = 1024 * 1024

class SharedViewModel : ViewModel() {

    var auth: FirebaseAuth = Firebase.auth

    private var db = FirebaseFirestore.getInstance()

    val bitmaps: MutableMap<String, Bitmap?> = mutableMapOf()

    // Position inside Recycler view trip Lists. this decouple the arguments to pass in Navigation
    private val position = MutableLiveData(0)

    fun setPosition(pos: Int) {
        position.value = pos
    }

    fun getPosition(): LiveData<Int> {
        return position
    }

    // TripListFragment
    private val myTrips: MutableLiveData<Resource<List<Trip>>> by lazy {
        MutableLiveData<Resource<List<Trip>>>().also {
            MainScope().launch {
                loadMyTrips()
            }
        }
    }

    private suspend fun loadMyTrips() {
        try {
            withContext(Dispatchers.IO) {
                //Return loading at beginning (while DB not finish)
                myTrips.postValue(Resource.Loading())

                //Return Success/Failure when DB finish
                db.collection("trips")
                    .whereEqualTo("driverEmail", auth.currentUser!!.email)
                    .addSnapshotListener { tasks, error ->
                        if (error != null)
                            myTrips.postValue(Resource.Failure(error))

                        if (tasks != null) {
                            val tmpTrips = mutableListOf<Trip>()
                            for (document in tasks.documents) {
                                val tmp = document.toObject(Trip::class.java)!!
                                tmpTrips.add(tmp)
                            }
                            myTrips.value = Resource.Success(tmpTrips)
                        }
                    }
            }
        } catch (e: Exception) {
            myTrips.postValue(Resource.Failure(e))
        }
    }

    //TripEditFragment onClick on Save
    fun addOrReplaceTrip(newTrip: Trip) {
        if (newTrip.id == "") {
            var max = 0

            if (myTrips.value is Resource.Success) {
                val tmpMyTrips = myTrips.value as Resource.Success
                for (trip in tmpMyTrips.data) {
                    val num = trip.id.split("_").last().toInt()
                    if (num != max) {
                        break
                    } else max++
                }
                newTrip.id = "${auth.currentUser!!.email!!}_${max}"
            }
        }

        db.collection("trips")
            .document(newTrip.id)
            .set(newTrip) //TODO handle return ?
            .addOnSuccessListener {
            }
            .addOnFailureListener {
            }
    }

    fun updatePhotoTrip(filename: String, bitmap: Bitmap, targetTrip: Trip) {
        val storage = Firebase.storage
        val storageRef = storage.reference
        val testRef = storageRef.child(filename)

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()
        val uploadTask = testRef.putBytes(data)
        uploadTask
            .addOnSuccessListener {
                targetTrip.carPhotoPath = filename
                addOrReplaceTrip(targetTrip)
            }
            .addOnFailureListener {
                addOrReplaceTrip(targetTrip)
            }

        bitmaps[targetTrip.id] = bitmap
    }

    //TripDetailsFragment, The user could delete the trip
    fun deleteTrip(tripID: String) {
        db.collection("trips")
            .document(tripID)
            .delete() //TODO Should we report a result to user?
    }

    fun deletePhotoTrip(carPhotoPath: String) {
        val storage = Firebase.storage
        val storageRef = storage.reference
        val imageRef = storageRef.child(carPhotoPath)
        imageRef.delete() //TODO Should we report a result to user?
    }

    fun getMyTrips(): LiveData<Resource<List<Trip>>> {
        return myTrips
    }

    //OthersTripListFragment
    private val othersTrips: MutableLiveData<Resource<List<Trip>>> by lazy {
        MutableLiveData<Resource<List<Trip>>>().also {
            MainScope().launch {
                loadOthersTrips()
            }
        }
    }

    private suspend fun loadOthersTrips() {
        withContext(Dispatchers.IO) {
            try {
                //Return loading at beginning (while DB not finish)
                othersTrips.postValue(Resource.Loading())

                //Return Success/Failure when DB finish
                db.collection("trips")
                    .whereNotEqualTo("driverEmail", auth.currentUser!!.email)
                    .addSnapshotListener { tasksDB, error ->
                        if (error != null)
                            othersTrips.postValue(Resource.Failure(error))

                        if (tasksDB != null) {
                            val tmpTrips = mutableListOf<Trip>()
                            for (document in tasksDB.documents) {
                                val tmp = document.toObject(Trip::class.java)
                                if (getOthersTrips().value is Resource.Success) {
                                    val tmpOthersTrips = getOthersTrips().value as Resource.Success
                                    for (trip in tmpOthersTrips.data) {
                                        if (tmp != null && tmp.id == trip.id &&
                                            tmp.carPhotoPath != trip.carPhotoPath
                                        ) {
                                            bitmaps[tmp.id] = null
                                            break
                                        }
                                    }
                                }

                                if (tmp != null)
                                    tmpTrips.add(tmp)
                            }

                            othersTrips.value = Resource.Success(
                                tmpTrips.filter {
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
                                        it.checkPoints[0].timestamp.subSequence(
                                            0,
                                            10
                                        ) == filter.value?.departureDate
                                    else
                                        true
                                }.filter {
                                    if (filter.value?.arrivalDate != null)
                                        it.checkPoints[it.checkPoints.size - 1].timestamp.subSequence(
                                            0,
                                            10
                                        ) == filter.value?.arrivalDate
                                    else
                                        true
                                }.filter {
                                    it.seatPrice <= filter.value?.maxPrice!! && it.seatPrice >= filter.value?.minPrice!!
                                }.toList()
                            )
                        }
                    }
            } catch (e: Exception) {
                othersTrips.postValue(Resource.Failure(e))
            }
        }
    }

    fun getOthersTrips(): LiveData<Resource<List<Trip>>> {
        return othersTrips
    }

    //In Other Trips list, you can do a search using filters
    private val filter = MutableLiveData<Filter>(Filter())

    fun setFilter(filter: Filter) {
        this.filter.value = filter
        MainScope().launch {
            loadOthersTrips()
        }
    }

    fun getFilter(): Filter? {
        return filter.value
    }

    //BoughtTripsListFragment
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
                db.collection("bookings")
                    .whereEqualTo("userEmail", auth.currentUser!!.email!!)
                    .addSnapshotListener { bookingsDB, errorBookings ->
                        if (errorBookings != null)
                            myBookedTrips.postValue(Resource.Failure(errorBookings))

                        if (bookingsDB != null) {
                            val tmpTripsID = mutableListOf<String>()
                            for (document in bookingsDB.documents) {
                                val tmp = document.toObject(Booking::class.java)
                                if (tmp != null)
                                    tmpTripsID.add(tmp.tripID)
                            }

                            if (tmpTripsID.isEmpty())
                                myBookedTrips.postValue(Resource.Success(listOf()))
                            else {
                                db.collection("trips")
                                    .whereIn("id", tmpTripsID)
                                    .addSnapshotListener { tripsDB, errorTrips ->
                                        if (errorTrips != null)
                                            myBookedTrips.postValue(Resource.Failure(errorTrips))

                                        if (tripsDB != null) {
                                            val tmpBookedTrips = mutableListOf<Trip>()
                                            for (document in tripsDB.documents) {
                                                val tmp = document.toObject(Trip::class.java)
                                                if (tmp != null)
                                                    tmpBookedTrips.add(tmp)
                                            }
                                            myBookedTrips.postValue(Resource.Success(tmpBookedTrips))
                                        }
                                    }
                            }
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

    //TripsOfInterestListFragment
    private val myInterestedTrips: MutableLiveData<Resource<List<Trip>>> by lazy {
        MutableLiveData<Resource<List<Trip>>>().also {
            MainScope().launch {
                loadInterestedTrips()
            }
        }
    }

    private suspend fun loadInterestedTrips() {
        withContext(Dispatchers.IO) {
            try {
                //Return loading at beginning (while DB not finish)
                myInterestedTrips.postValue(Resource.Loading())

                //Return Success/Failure when DB finish
                db.collection("trips")
                    .whereNotEqualTo("driverEmail", auth.currentUser!!.email)
                    .addSnapshotListener { tasksDB, error ->
                        if (error != null)
                            myInterestedTrips.postValue(Resource.Failure(error))

                        if (tasksDB != null) {
                            val tmpTrips = mutableListOf<Trip>()
                            val tmpMyself = User(
                                email = auth.currentUser!!.email!!,
                                name = auth.currentUser!!.displayName!!,
                                isAccepted = false
                            )

                            for (document in tasksDB.documents) {
                                val tmp = document.toObject(Trip::class.java)
                                if (tmp != null && tmp.interestedUsers.contains(tmpMyself))
                                    tmpTrips.add(tmp)
                            }

                            myInterestedTrips.postValue(Resource.Success(tmpTrips))
                        }
                    }
            } catch (e: Exception) {
                myInterestedTrips.postValue(Resource.Failure(e))
            }
        }
    }

    fun getMyInterestedTrips(): LiveData<Resource<List<Trip>>> {
        return myInterestedTrips
    }

    // FAKE MAIL USED IN THE APP
    private val user = MutableLiveData<Resource<User>>()
        .also {
            it.postValue(Resource.Loading())
        }

    private val userPhoto = MutableLiveData<Resource<Bitmap>>()
        .also {
            it.postValue(Resource.Loading())
        }


    fun setUser(targetUser: User) {
        user.postValue(Resource.Loading())
        try {
            db.collection("users")
                .document(auth.currentUser!!.email!!)
                .addSnapshotListener { userDB, err ->
                    if (err != null)
                        user.postValue(Resource.Failure(err))

                    if (userDB != null) {
                        val tmpUser = userDB.toObject(User::class.java)
                        if (tmpUser != null) {
                            user.postValue(Resource.Success(tmpUser))
                            MainScope().launch {
                                downloadUserPhoto(auth.currentUser!!.email!!, true)
                            }
                        } else { // ADD USER
                            db.collection("users")
                                .document(targetUser.email)
                                .set(targetUser)
                                .addOnSuccessListener {
                                    user.postValue(Resource.Success(targetUser))
                                }
                                .addOnFailureListener {
                                    user.postValue(Resource.Failure(Exception("Error in setting the user")))
                                }
                        }
                    }
                }
        } catch (e: Exception) {
            user.postValue(Resource.Failure(e))
        }

    }

    fun setUserBitmap(bitmap: Bitmap) {
        val tmpUser = user.value
        if (tmpUser is Resource.Success) {
            tmpUser.data.bitmap = bitmap
            user.postValue(Resource.Success(tmpUser.data))
        }
    }

    fun editUser(user: User) {
        db.collection("users")
            .document(auth.currentUser!!.email!!)
            .set(user)
            .addOnSuccessListener {} //TODO Should we report a result to user?
    }

    fun getUser(): LiveData<Resource<User>> {
        return user
    }

    fun getUserPhoto(): LiveData<Resource<Bitmap>> {
        return userPhoto
    }

    // In Trip Details you can see other users iterested in on accepted
    private val otherUser = MutableLiveData<Resource<User>>()
        .also {
            it.postValue(Resource.Loading())
        }
    private val otherUserPhoto = MutableLiveData<Resource<Bitmap>>()
        .also {
            it.postValue(Resource.Loading())
        }

    fun downloadUserPhoto(email: String, currentUser: Boolean) {
        val storage = Firebase.storage
        val storageRef = storage.reference
        val testRef = storageRef.child(email)
        testRef.metadata
            .addOnSuccessListener { metadata ->
                val size = metadata.sizeBytes
                testRef.getBytes(ONE_MEGABYTE)
                    .addOnSuccessListener {
                        val imageBitmap = BitmapFactory.decodeByteArray(it, 0, size.toInt())
                        if (imageBitmap != null) {
                            if (!currentUser) {
                                otherUserPhoto.postValue(Resource.Success(imageBitmap))
                            } else {
                                userPhoto.postValue(Resource.Success(imageBitmap))
                            }
                        }

                    }.addOnFailureListener {
                        if (!currentUser)
                            otherUserPhoto.postValue(Resource.Failure(Exception("Error downloading the picture")))
                        else
                            userPhoto.postValue(Resource.Failure(Exception("Error downloading the picture")))
                    }
            }.addOnFailureListener {
                // User has not a picture yet
                if (!currentUser)
                    otherUserPhoto.postValue(Resource.Failure(Exception("No_Storage_For_New_User")))
                else
                    userPhoto.postValue(Resource.Failure(Exception("No_Storage_For_New_User")))
            }
    }

    fun uploadCurrentUserPhoto(bitmap: Bitmap) {
        val storage = Firebase.storage
        val storageRef = storage.reference
        val testRef = storageRef.child(auth.currentUser!!.email!!)

        // it saves the bitmap into the internal storage
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()
        val uploadTask = testRef.putBytes(data)
        uploadTask
            .addOnFailureListener {
            }
            .addOnSuccessListener {
                userPhoto.postValue(Resource.Success(bitmap))
            }
    }

    fun setOtherUser(email: String) {
        otherUser.postValue(Resource.Loading())
        try {
            db.collection("users")
                .document(email)
                .addSnapshotListener { userDB, err ->
                    if (err != null)
                        othersTrips.postValue(Resource.Failure(err))

                    if (userDB != null) {
                        val tmpOtherUser = userDB.toObject(User::class.java)
                        if (tmpOtherUser != null) {
                            otherUser.postValue(Resource.Success(tmpOtherUser))

                            downloadUserPhoto(email, false)
                        }
                    }
                }
        } catch (e: Exception) {
            otherUser.postValue(Resource.Failure(e))
        }
    }


    fun getOtherUser(): LiveData<Resource<User>> {
        return otherUser
    }

    fun getOtherUserPhoto(): LiveData<Resource<Bitmap>> {
        return otherUserPhoto
    }

    fun userIsInterested(tripToCheck: Trip): Boolean {
        // User shows his interest in the trip.
        val myself =
            User(email = auth.currentUser!!.email!!, name = auth.currentUser!!.displayName!!)
        return tripToCheck.interestedUsers.contains(myself)
    }

    // If a trip is deleted from owner, the users already booked it have to be deleted too
    fun removeFromBookings(bookingID: String) {
        db.collection("bookings")
            .document(bookingID)
            .delete() //TODO Should we report a result to user?
    }

    fun updateTripInterestedUser(tripToUpdate: Trip, isInterested: Boolean, userToUpdate: User?) {
        val userTarget: User?
        val myself =
            User(email = auth.currentUser!!.email!!, name = auth.currentUser!!.displayName!!)
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

    fun bookingIsAccepted(trip: Trip): Boolean {
        var tmpBookedTrips: Resource.Success<List<Trip>> = Resource.Success(listOf())
        if (myBookedTrips.value != null)
            tmpBookedTrips = myBookedTrips.value as Resource.Success
        return tmpBookedTrips.data.contains(trip)
    }

    fun downloadMetadataPhoto(path: String): StorageMetadata {
        val storage = Firebase.storage
        val storageRef = storage.reference
        val testRef = storageRef.child(path)
        return Tasks.await(testRef.metadata)
    }

    fun downloadPhoto(size: Long, path: String): Bitmap {
        val storage = Firebase.storage
        val storageRef = storage.reference
        val testRef = storageRef.child(path)
        val ONE_MEGABYTE: Long = 1024 * 1024
        val byteArray = Tasks.await(testRef.getBytes(ONE_MEGABYTE))
        return BitmapFactory.decodeByteArray(byteArray, 0, size.toInt())
    }

}

data class User(
    val name: String = "", val nickname: String = "",
    val email: String = "", val location: String = "",
    val phone_number: String = "", val rating: Float = 0f,
    var isAccepted: Boolean = false, var bitmap: Bitmap? = null
)

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
                var interestedUsers: MutableList<User> = mutableListOf(),
                var geoPoints: MutableList<Coordinate> = mutableListOf()
)

data class Coordinate(var latitude: Double = 0.0, var longitude: Double = 0.0)

data class Booking(var tripID: String = "", var userEmail: String = "")

data class Filter(
    var departureLocation: String? = null,
    var arrivalLocation: String? = null,
    var departureDate: String? = null,
    var arrivalDate: String? = null,
    var minPrice: Float = 0f,
    var maxPrice: Float = 100f
)

sealed class Resource<out T> {
    class Loading<out T> : Resource<T>()
    data class Success<out T>(val data: T) : Resource<T>()
    data class Failure<out T>(val throwable: Throwable) : Resource<T>()
}
