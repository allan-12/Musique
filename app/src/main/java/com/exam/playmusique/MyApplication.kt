package com.exam.playmusique

import android.app.Application

class MyApplication : Application() {

    val musicPlayerManager: MusicPlayerManager by lazy { MusicPlayerManager() }


    companion object{


        lateinit var instance : MyApplication




    }

    override fun onCreate(){
        super.onCreate()

        instance = this



    }
}