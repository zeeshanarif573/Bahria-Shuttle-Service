package com.bahria.shuttle_passenger.ui

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.bahria.shuttle_passenger.R
import com.bahria.shuttle_passenger.databinding.ActivityMapBinding
import com.bahria.shuttle_passenger.model.DriverInfo
import com.bahria.shuttle_passenger.model.DriverLatLng
import com.bahria.shuttle_passenger.network.NetworkUtils
import com.bahria.shuttle_passenger.utils.Util
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    lateinit var binding: ActivityMapBinding
    lateinit var fusedLocationClient: FusedLocationProviderClient
    lateinit var map: GoogleMap
    private var latitude = 0.0
    private var longitude = 0.0
    lateinit var progressDialog: ProgressDialog
    private lateinit var databaseReference: DatabaseReference
    private lateinit var valueEventListener: ValueEventListener
    private lateinit var childEventListener: ChildEventListener
    private var driverInfoList: MutableList<DriverLatLng> = ArrayList()
    lateinit var marker: Marker
    private var isFirebaseLoad = false

    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val locationList = locationResult.locations
            if (locationList.isNotEmpty()) {
                val location = locationList.last()
                val latLng = LatLng(
                    latitude,
                    longitude
                )
                marker.position = latLng
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16.0f))
            }
        }
    }

    private val getLocationResult: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            if (it.resultCode == Activity.RESULT_OK)
                getLatLngOfDriverFromFirebase()
        }

    private val locationRequest: LocationRequest = LocationRequest.create().apply {
        interval = 1000
        fastestInterval = 1000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        maxWaitTime = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_map)
        initialization()
        checkAccessFineLocation()

        binding.topLayout.backBtn.setOnClickListener {
            finish()
        }

        binding.contactNo.setOnClickListener {
            val cellIntent = Intent(Intent.ACTION_DIAL)
            cellIntent.data = Uri.parse("tel:" + intent.getStringExtra("cellNo"))
            startActivity(cellIntent)
        }
    }

    private fun initialization() {
        binding.topLayout.backBtn.visibility = VISIBLE
        progressDialog = Util.initProgressDialog(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getDriverInformation()
    }

    private fun getDriverInformation() {
        binding.name.text = "Driver: " + intent.getStringExtra("name")
        binding.busNo.text = "Bus No: " + intent.getStringExtra("busNo")
        binding.route.text = intent.getStringExtra("route")
    }

    private fun checkAccessFineLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
            checkGPSLocation()
        else
            requestFineLocationPermission()
    }

    private fun checkGPSLocation() {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val result = LocationServices.getSettingsClient(this).checkLocationSettings(builder.build())

        result.addOnCompleteListener { task ->
            try {
                val response = task.getResult(ApiException::class.java)
                if (response!!.locationSettingsStates?.isGpsPresent!! && response.locationSettingsStates?.isGpsUsable!!)
                    getLatLngOfDriverFromFirebase()

            } catch (exception: ApiException) {
                when (exception.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED ->
                        try {
                            if (exception is ResolvableApiException) {
                                val request: IntentSenderRequest = IntentSenderRequest.Builder(
                                    exception.resolution.intentSender
                                ).setFillInIntent(Intent())
                                    .build()
                                getLocationResult.launch(request)
                            }

                        } catch (e: SendIntentException) {
                            e.printStackTrace()
                        } catch (e: ClassCastException) {
                            e.printStackTrace()
                        }

                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                    }
                }
            }
        }
    }

    private fun requestFineLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            100
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            100 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                        == PackageManager.PERMISSION_GRANTED
                    )
                        checkGPSLocation()
                    else
                        getLatLngOfDriverFromFirebase()

                } else
                    requestFineLocationPermission()

                return
            }
        }
    }

    private fun getLatLngOfDriverFromFirebase() {
        if (NetworkUtils.isInternetAvailable()) {
            progressDialog.show()
            if (intent.getStringExtra("from") == "viewAll") {
                databaseReference = FirebaseDatabase.getInstance().reference.child("All-Buses")
                valueEventListener =
                    databaseReference.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            driverInfoList.clear()
                            for (item in snapshot.children) {
                                driverInfoList.add(
                                    DriverLatLng(
                                        item.child("busNo").value.toString(),
                                        item.child("latitude").value.toString().toDouble(),
                                        item.child("longitude").value.toString().toDouble()
                                    )
                                )
                            }
                            getInitialLocationOnMap()
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("error", error.message)
                        }
                    })

            } else {
                Log.e("from", "particular")
                databaseReference = FirebaseDatabase.getInstance().reference.child("Driver-Info")
                childEventListener =
                    databaseReference.addChildEventListener(object : ChildEventListener {
                        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                            if (dataSnapshot.child("busNo").value.toString() == "BTK-125") {
                                val driverInfo: DriverInfo? =
                                    dataSnapshot.getValue(DriverInfo::class.java)
                                latitude = driverInfo?.latitude!!.toDouble()
                                longitude = driverInfo.longitude.toDouble()

                                Log.e("childEventListener", "lat: $latitude ,longitude: $longitude")

                                if (checkIfLocationIsNull())
                                    finish()
                                else {
                                    if (!isFirebaseLoad) {
                                        isFirebaseLoad = true
                                        getInitialLocationOnMap()
                                    }
                                }
                            }
                        }

                        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
                            if (dataSnapshot.child("busNo").value.toString() == "BTK-125") {
                                latitude =
                                    dataSnapshot.getValue(DriverInfo::class.java)?.latitude!!.toDouble()
                                longitude =
                                    dataSnapshot.getValue(DriverInfo::class.java)?.longitude!!.toDouble()
                                if (checkIfLocationIsNull())
                                    finish()
                                else
                                    Log.e(
                                        "childEventListener",
                                        "lat: $latitude ,longitude: $longitude"
                                    )
                            }
                        }

                        override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
                        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
                        override fun onCancelled(databaseError: DatabaseError) {}
                    })
            }
        } else
            Toast.makeText(
                this,
                getString(R.string.no_internet),
                Toast.LENGTH_LONG
            ).show()
    }

    private fun checkIfLocationIsNull(): Boolean {
        if (latitude == 0.0 && longitude == 0.0) {
            if (progressDialog.isShowing)
                progressDialog.dismiss()
            Toast.makeText(
                this@MapActivity,
                getString(R.string.driver_offline),
                Toast.LENGTH_LONG
            ).show()
            return true
        }
        return false
    }

    private fun getInitialLocationOnMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestFineLocationPermission()
            return
        }

        if (NetworkUtils.isInternetAvailable()) {
            fusedLocationClient.lastLocation.addOnCompleteListener {
                progressDialog.dismiss()
                map.clear()
                if (intent.getStringExtra("from") == "viewAll") {
                    for (locations in driverInfoList) {
                        if (locations.latitude != 0.0) {
                            val latLng = LatLng(locations.latitude, locations.longitude)
                            placeMarkers(latLng, locations.busNo)
                            val update = CameraUpdateFactory.newLatLngZoom(latLng, 12.7f)
                            map.animateCamera(update)
                        }
                    }

                    map.isMyLocationEnabled = true

                } else {
                    binding.driverDetailCardView.visibility = VISIBLE
                    val latLng = LatLng(latitude, longitude)
                    placeMarkers(latLng, "")

                    val update = CameraUpdateFactory.newLatLngZoom(latLng, 15.0f)
                    map.animateCamera(update)
                    map.isMyLocationEnabled = false
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                    )
                }
            }
        } else
            Toast.makeText(
                this,
                getString(R.string.no_internet),
                Toast.LENGTH_LONG
            ).show()
    }

    private fun placeMarkers(latLng: LatLng, busNo: String) {
        marker = map.addMarker(
            MarkerOptions().position(latLng)
                .icon(
                    Util.bitmapFromVector(
                        applicationContext,
                        R.drawable.ic_bus_round
                    )
                )
                .title(busNo)
        )!!
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::valueEventListener.isInitialized)
            databaseReference.removeEventListener(valueEventListener)

        if (this::childEventListener.isInitialized)
            databaseReference.removeEventListener(childEventListener)
    }
}