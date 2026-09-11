package com.appdomicilios

import android.app.Application
import com.appdomicilios.data.DemoRepository
import com.appdomicilios.data.SessionStore
import com.appdomicilios.notifications.ensurePushNotificationChannels

class XpertGoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContextHolder.init(this)
        ensurePushNotificationChannels(this)
        SessionStore.loadUser(this)?.let(DemoRepository::applyAuthenticatedUser)
    }
}
