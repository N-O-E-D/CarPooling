package it.polito.mad.group08.carpooling

import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ScrollView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.*
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.ArrayList


class GeoMap {

    companion object StaticMethods {
        fun customizeMap(map: MapView, view: View, context: Context?) {
            map.setTileSource(TileSourceFactory.MAPNIK)

            val controller = map.controller
            map.isHorizontalMapRepetitionEnabled = false
            map.isVerticalMapRepetitionEnabled = false
            controller.setZoom(3)

            val scrollView = view.findViewById<ScrollView>(R.id.scrollView)


            map.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_MOVE -> scrollView?.requestDisallowInterceptTouchEvent(true)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> scrollView?.requestDisallowInterceptTouchEvent(
                        false
                    )
                }

                map.onTouchEvent(event)
            }

            val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), map);
            locationOverlay.enableMyLocation();
            map.overlays.add(locationOverlay)

            val compassOverlay =
                CompassOverlay(context, InternalCompassOrientationProvider(context), map)
            compassOverlay.enableCompass()
            map.overlays.add(compassOverlay)

            val rotationGestureOverlay = RotationGestureOverlay(map)
            rotationGestureOverlay.isEnabled
            map.setMultiTouchControls(true)
            map.overlays.add(rotationGestureOverlay)

            val dm: DisplayMetrics = context?.resources!!.displayMetrics
            val scaleBarOverlay = ScaleBarOverlay(map)
            scaleBarOverlay.setCentred(true)
            scaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 10)
            map.overlays.add(scaleBarOverlay)
        }

        fun drawPath(
            map: MapView,
            geoPoints: MutableList<GeoPoint>,
            context: Context?,
            items: ArrayList<OverlayItem>
        ) {

            val polyline = Polyline()

            for ((index, geopoint) in geoPoints.withIndex()) {
                if (index == 0) {
                    items.add(
                        OverlayItem(
                            "${context?.getString(R.string.departure)}",
                            "",
                            geopoint
                        )
                    )
                } else if (index == geoPoints.size - 1) {
                    items.add(OverlayItem("${context?.getString(R.string.arrival)}", "", geopoint))
                } else {
                    items.add(
                        OverlayItem(
                            "${context?.getString(R.string.stop)} $index",
                            "",
                            geopoint
                        )
                    )
                }

                polyline.addPoint(geopoint)
            }

            val overlay = ItemizedOverlayWithFocus<OverlayItem>(items, object :
                ItemizedIconOverlay.OnItemGestureListener<OverlayItem> {
                override fun onItemSingleTapUp(index: Int, item: OverlayItem): Boolean {
                    return false
                }

                override fun onItemLongPress(index: Int, item: OverlayItem): Boolean {
                    return false
                }
            }, context)
            overlay.setFocusItemsOnTap(true)

            map.overlays.add(overlay)
            map.overlays.add(polyline)
        }

        fun setUpPinPoint(map: MapView, geoPoints: MutableList<GeoPoint>, context: Context?, items: ArrayList<OverlayItem>) {

            map.overlays.add(object : Overlay() {
                override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {

                    val projection = mapView.projection
                    val geoPoint = projection.fromPixels(
                        e.x.toInt(),
                        e.y.toInt()
                    ) as GeoPoint



                    geoPoints.add(geoPoint)

                    items.add(OverlayItem("Ancona", "Description", geoPoint))

                    GeoMap.drawPath(map, geoPoints, context, items)

                    return true
                }
            })
        }

        fun clearPath(map: MapView, view: View, context: Context?) {
            map.overlays.forEach { elem ->
                if ((elem is ItemizedOverlayWithFocus<*>) || (elem is Polyline)) map.overlays.remove(
                    elem
                )
            }
            map.invalidate()
        }
    }
}
