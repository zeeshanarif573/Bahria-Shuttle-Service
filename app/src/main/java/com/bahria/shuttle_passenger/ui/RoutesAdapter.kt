package com.bahria.shuttle_passenger.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bahria.shuttle_passenger.listener.RouteClickListener
import com.bahria.shuttle_passenger.databinding.RouteListItemBinding
import com.bahria.shuttle_passenger.model.DriverInfo

class RoutesAdapter(
    private val context: Context,
    private val driverInfoList: List<DriverInfo>,
    private val routeClickListener: RouteClickListener
) :
    RecyclerView.Adapter<RoutesAdapter.ViewHolder>() {

    //this method is returning the view for each item in the list
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RouteListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    //this method is binding the data on the list
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val driverInfo = driverInfoList[position]
        holder.bind(driverInfo)
        holder.itemView.setOnClickListener {
            routeClickListener.onRouteClicked(position)
        }
    }

    //this method is giving the size of the list
    override fun getItemCount(): Int {
        return driverInfoList.size
    }

    //the class is holding the list view
    class ViewHolder(private val binding: RouteListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            driverInfo: DriverInfo
        ) {
            binding.route.text = driverInfo.route
            binding.busNo.text = driverInfo.busNo
        }
    }
}