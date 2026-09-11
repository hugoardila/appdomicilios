package com.appdomicilios

import android.content.Context

object AppContextHolder {
    lateinit var applicationContext: Context
        private set

    val isReady: Boolean
        get() = ::applicationContext.isInitialized

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }
}
