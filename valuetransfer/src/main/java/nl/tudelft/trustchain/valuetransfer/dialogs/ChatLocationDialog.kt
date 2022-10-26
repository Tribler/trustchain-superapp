package nl.tudelft.trustchain.valuetransfer.dialogs

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import java.util.*

class ChatLocationDialog(
    val publicKey: PublicKey,
    private val location: Location? = null
) : VTDialogFragment(), OnMapReadyCallback, View.OnClickListener, GoogleMap.OnMapClickListener, Toolbar.OnMenuItemClickListener {
    private val contact: Contact? by lazy {
        getContactStore().getContactFromPublicKey(publicKey)
    }

    private lateinit var dialogView: View
    private lateinit var actionBar: Toolbar
    private lateinit var titleView: AppCompatTextView
    private lateinit var subTitleView: AppCompatTextView

    private lateinit var mapFragment: SupportMapFragment
    private lateinit var map: GoogleMap

    private lateinit var openDirectionsView: LinearLayout
    private lateinit var locationSendView: LinearLayout
    private lateinit var locationTypeSend: TextView
    private lateinit var locationNameSend: TextView

    private lateinit var bottomSheetDialog: Dialog

    private var mapMarker: Marker? = null
    private var mapInitialized = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            bottomSheetDialog = Dialog(requireContext(), R.style.FullscreenDialog)

            @Suppress("DEPRECATION")
            bottomSheetDialog.window?.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
            val view = layoutInflater.inflate(R.layout.dialog_contact_chat_location, null)
            dialogView = view

            // Set action bar
            setHasOptionsMenu(true)
            actionBar = view.findViewById(R.id.tbActionBar)
            actionBar.inflateMenu(R.menu.contact_chat_location)
            actionBar.setOnMenuItemClickListener(this)
            actionBar.setNavigationOnClickListener {
                bottomSheetDialog.dismiss()
            }

            titleView = view.findViewById(R.id.tvActionbarTitle)
            subTitleView = view.findViewById(R.id.tvActionbarSubTitle)

            if (location != null) {
                titleView.text = getString(R.string.text_location)
                subTitleView.text = getAddress(location.latitude, location.longitude) ?: resources.getString(R.string.text_address_not_found)
            } else {
                titleView.text = resources.getString(R.string.dialog_title_contact_chat_send_location)
                subTitleView.text = resources.getString(
                    R.string.text_to_contact,
                    contact?.name ?: resources.getString(R.string.text_unknown_contact_lowercase)
                )
                resources.getString(R.string.text_unknown_contact)
            }

            mapFragment = childFragmentManager.findFragmentById(R.id.fragmentMap) as SupportMapFragment
            mapFragment.getMapAsync(this)

            locationSendView = view.findViewById<LinearLayout>(R.id.llSendLocation).apply {
                setOnClickListener(this@ChatLocationDialog)
            }
            openDirectionsView = view.findViewById<LinearLayout>(R.id.llOpenDirections).apply {
                setOnClickListener(this@ChatLocationDialog)
            }

            locationTypeSend = view.findViewById(R.id.tvLocationTypeSend)
            locationNameSend = view.findViewById(R.id.tvLocationNameSend)

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            bottomSheetDialog
        } ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }

    @Suppress("Deprecation")
    override fun onClick(v: View?) {
        when (v?.id) {
            android.R.id.home -> bottomSheetDialog.dismiss()
            R.id.llSendLocation -> {
                Location("").apply {
                    this.latitude = mapMarker?.position?.latitude ?: map.myLocation.latitude
                    this.longitude = mapMarker?.position?.longitude ?: map.myLocation.longitude
                }.let {
                    getPeerChatCommunity().sendLocation(
                        it,
                        getAddress(it.latitude, it.longitude) ?: resources.getString(R.string.text_address_not_found),
                        publicKey,
                        getIdentityCommunity().getIdentityInfo(appPreferences.getIdentityFaceHash())
                    )
                    bottomSheetDialog.dismiss()
                }
            }
            R.id.llOpenDirections -> {
                val uri = Uri.parse("geo:0,0?q=${location!!.latitude},${location.longitude}()")
                val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                mapIntent.setPackage("com.google.android.apps.maps")
                startActivity(mapIntent)
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem) = when (item.itemId) {
        R.id.actionShowNormalMap -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.actionShowHybridMap -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.actionShowSatelliteMap -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.actionShowTerrainMap -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    @Suppress("Deprecation")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        enableMyLocation()

        if (location != null) {
            locationSendView.isVisible = false
            openDirectionsView.isVisible = true
            map.moveCamera(CameraUpdateFactory.zoomTo(15f))
            addMarkerToMap(location.latitude, location.longitude, true)
        } else {
            googleMap.setOnMapClickListener(this)
            removeMarker(map)

            openDirectionsView.isVisible = false

            map.setOnMyLocationChangeListener {
                if (!mapInitialized && mapMarker == null) {
                    val latLng = LatLng(it.latitude, it.longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

                    locationTypeSend.text = resources.getString(R.string.text_send_current_location)
                    locationNameSend.text = getAddress(it.latitude, it.longitude) ?: resources.getString(R.string.text_address_not_found)

                    mapInitialized = true
                }

                locationSendView.isVisible = map.isMyLocationEnabled && mapInitialized
            }
        }
    }

    override fun onMapClick(point: LatLng) {
        if (location == null && mapMarker != null) {
            map.clear()
        }

        addMarkerToMap(point.latitude, point.longitude)
    }

    private fun addMarkerToMap(latitude: Double, longitude: Double, move: Boolean = false) {
        LatLng(latitude, longitude).let { latLng ->
            mapMarker = map.addMarker(MarkerOptions().position(latLng))

            if (move)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, map.cameraPosition.zoom))

            locationSendView.isVisible = location == null
            locationTypeSend.text = resources.getString(R.string.text_send_this_location)
            locationNameSend.text = getAddress(latitude, longitude) ?: resources.getString(R.string.text_address_not_found)
        }
    }

    @Suppress("DEPRECATION")
    private fun removeMarker(map: GoogleMap) {
        map.setOnMarkerClickListener { marker ->
            marker.remove()
            mapMarker = null

            locationSendView.isVisible = map.isMyLocationEnabled

            if (map.isMyLocationEnabled) {
                locationTypeSend.text = resources.getString(R.string.text_send_current_location)
                locationNameSend.text = getAddress(map.myLocation.latitude, map.myLocation.longitude) ?: resources.getString(R.string.text_address_not_found)
            }

            return@setOnMarkerClickListener true
        }
    }

    private fun getAddress(latitude: Double, longitude: Double): String? {
        return try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            geocoder.getFromLocation(
                latitude,
                longitude,
                1
            ).let { address ->
                address!![0].getAddressLine(0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @Suppress("MissingPermission")
    private fun enableMyLocation() {
        if (isPermissionGranted()) {
            try {
                map.isMyLocationEnabled = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else requestPermissions(
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_LOCATION
        )
    }

    private fun isPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                enableMyLocation()
            }
        }
    }

    companion object {
        const val PERMISSION_LOCATION = 1
    }
}
