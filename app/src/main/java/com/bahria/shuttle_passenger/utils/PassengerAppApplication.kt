package com.bahria.shuttle_passenger.utils

import android.app.Application
import android.content.Context

class PassengerAppApplication : Application() {

//    lateinit var loginRepository: LoginRepository

    companion object {
        private lateinit var appContext: Context
        fun getCtx(): Context {
            return appContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext;
        init()
    }

    private fun init() {
//        val apiService = RetrofitHelper.invoke().create(API::class.java)
//        val database = VisitorDatabase.getDatabase(applicationContext)

//        loginRepository = LoginRepository(apiService, applicationContext)
    }
}