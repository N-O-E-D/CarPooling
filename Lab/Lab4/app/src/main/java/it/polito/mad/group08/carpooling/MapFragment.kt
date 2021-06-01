package it.polito.mad.group08.carpooling

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.OverlayItem

class MapFragment : Fragment(R.layout.fragment_map) {
    private val model: SharedViewModel by activityViewModels()
    private lateinit var map: MapView

    override fun onResume() {
        super.onResume()
        if (this::map.isInitialized)
            map.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (this::map.isInitialized)
            map.onPause()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val geoPoints = model.getGeoPoints()
        val items = ArrayList<OverlayItem>()
        map = view.findViewById(R.id.mapFullScreen)
        GeoMap.customizeMap(map, view, context)
        GeoMap.drawPath(map, geoPoints.map { elem -> GeoPoint(elem.latitude, elem.longitude) }.toMutableList(), context, items)

    }
}