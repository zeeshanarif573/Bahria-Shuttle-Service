package com.bahria.shuttle_passenger.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bahria.shuttle_passenger.R
import com.bahria.shuttle_passenger.databinding.ActivityMainBinding
import com.bahria.shuttle_passenger.listener.RouteClickListener
import com.bahria.shuttle_passenger.model.DriverInfo
import com.bahria.shuttle_passenger.utils.Util
import com.google.firebase.database.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity(), RouteClickListener {

    lateinit var binding: ActivityMainBinding
    private lateinit var routesAdapter: RoutesAdapter
    private lateinit var driverInfoList: MutableList<DriverInfo>
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        databaseReference = FirebaseDatabase.getInstance().reference.child("All-Buses")
        setRoutesAdapter()

        binding.viewAll.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra("from", "viewAll")
            startActivity(intent)
        }
    }

    private fun setRoutesAdapter() {
        val gson = Gson()
        val driverInfoData = object : TypeToken<List<DriverInfo>>() {}.type
        driverInfoList = gson.fromJson(
            Util.getJsonDataFromAsset(this, "driver_info.json"),
            driverInfoData
        )

        routesAdapter = RoutesAdapter(this, driverInfoList, this)
        val layoutManager = LinearLayoutManager(this)
        binding.routesView.layoutManager = layoutManager
        binding.routesView.addItemDecoration(
            DividerItemDecoration(
                this,
                layoutManager.orientation
            )
        )
        binding.routesView.adapter = routesAdapter
    }

    override fun onRouteClicked(position: Int) {
        val driverInfo = driverInfoList[position]
        switchToMapActivity(driverInfo)
    }

    private fun switchToMapActivity(driverInfo: DriverInfo) {
        val intent = Intent(this, MapActivity::class.java)
        intent.putExtra("from", "")
        intent.putExtra("busNo", driverInfo.busNo)
        intent.putExtra("name", driverInfo.name)
        intent.putExtra("route", driverInfo.route)
        intent.putExtra("cellNo", driverInfo.cellNo)
        startActivity(intent)
    }
}