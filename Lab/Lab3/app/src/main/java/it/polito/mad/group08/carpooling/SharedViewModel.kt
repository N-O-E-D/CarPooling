package it.polito.mad.group08.carpooling

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    private val trips: MutableLiveData<MutableList<TripListFragment.Trip>> = MutableLiveData(mutableListOf())/*by lazy {
        MutableLiveData<MutableList<TripListFragment.Trip>>().also {
            loadTrips()
        }
    }*/

    fun addNewTrip(newTrip: TripListFragment.Trip) {
        trips.value?.add(newTrip)
    }

    fun getTrips() : LiveData<MutableList<TripListFragment.Trip>>{
        return trips
    }

    private fun loadTrips(){
        // Do an asynchronous operation to fetch trips.
        // TODO read data from DB
    }
}