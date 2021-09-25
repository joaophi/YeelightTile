package me.pedro.yeelighttile

import android.service.quicksettings.TileService
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

open class LifecycleAwareTileService : TileService(), LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    @CallSuper
    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    @CallSuper
    override fun onStartListening() {
        super.onStartListening()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    @CallSuper
    override fun onStopListening() {
        super.onStopListening()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}