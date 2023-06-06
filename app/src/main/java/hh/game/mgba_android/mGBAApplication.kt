package hh.game.mgba_android

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.google.android.material.color.DynamicColors

class mGBAApplication : Application() {
    companion object{
        lateinit var context : Context
    }
    override fun onCreate() {
        super.onCreate()
        context = this
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}