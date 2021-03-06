package org.tasks

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.android.utils.FlipperUtils
import com.facebook.flipper.plugins.databases.DatabasesFlipperPlugin
import com.facebook.flipper.plugins.inspector.DescriptorMapping
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin
import com.facebook.flipper.plugins.sharedpreferences.SharedPreferencesFlipperPlugin
import com.facebook.soloader.SoLoader
import leakcanary.AppWatcher
import org.tasks.data.CRASH_ON_MAIN_THREAD
import org.tasks.preferences.Preferences
import timber.log.Timber
import timber.log.Timber.DebugTree
import javax.inject.Inject

class BuildSetup @Inject constructor(
        private val context: Application,
        private val preferences: Preferences) {

    fun setup() {
        Timber.plant(DebugTree())
        SoLoader.init(context, false)
        CRASH_ON_MAIN_THREAD = preferences.getBoolean(R.string.p_crash_main_queries, false)
        if (preferences.getBoolean(R.string.p_leakcanary, false)) {
            AppWatcher.manualInstall(context)
        }
        if (preferences.getBoolean(R.string.p_flipper, false) && FlipperUtils.shouldEnableFlipper(context)) {
            val client = AndroidFlipperClient.getInstance(context)
            client.addPlugin(InspectorFlipperPlugin(context, DescriptorMapping.withDefaults()))
            client.addPlugin(DatabasesFlipperPlugin(context))
            client.addPlugin(NetworkFlipperPlugin())
            client.addPlugin(SharedPreferencesFlipperPlugin(context))
            client.start()
        }
        if (preferences.getBoolean(R.string.p_strict_mode_thread, false)) {
            StrictMode.setThreadPolicy(
                    StrictMode.ThreadPolicy.Builder()
                            .detectDiskReads()
                            .detectDiskWrites()
                            .detectNetwork()
                            .penaltyLog()
                            .build())
        }
        if (preferences.getBoolean(R.string.p_strict_mode_vm, false)) {
            StrictMode.setVmPolicy(
                    VmPolicy.Builder()
                            .detectLeakedSqlLiteObjects()
                            .detectLeakedClosableObjects()
                            .penaltyLog()
                            .build())
        }
    }
}