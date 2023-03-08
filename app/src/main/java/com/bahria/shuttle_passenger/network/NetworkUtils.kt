package com.bahria.shuttle_passenger.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.bahria.shuttle_passenger.utils.PassengerAppApplication

class NetworkUtils {

    companion object {
        fun isInternetAvailable(): Boolean {
            (PassengerAppApplication.getCtx()
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).run {
                return this.getNetworkCapabilities(this.activeNetwork)?.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                ) ?: false
            }
        }
    }
}