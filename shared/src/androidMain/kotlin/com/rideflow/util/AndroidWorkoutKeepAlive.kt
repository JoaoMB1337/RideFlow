package com.rideflow.util

import android.content.Context
import android.content.Intent
import com.rideflow.domain.session.WorkoutKeepAlive
import io.github.aakira.napier.Napier

class AndroidWorkoutKeepAlive(private val context: Context) : WorkoutKeepAlive {

    private val intent get() = Intent(context, WorkoutForegroundService::class.java)

    override fun acquire() {
        try {
            // minSdk 28, portanto startForegroundService está sempre disponível.
            context.startForegroundService(intent)
        } catch (e: Exception) {
            // Arrancar um foreground service pode falhar (ex.: restrições de background start).
            // O treino deve continuar mesmo assim — perde-se a garantia, não a sessão.
            Napier.e(tag = "KeepAlive", throwable = e) { "Failed to start foreground service" }
        }
    }

    override fun release() {
        try {
            context.stopService(intent)
        } catch (e: Exception) {
            Napier.w(tag = "KeepAlive", throwable = e) { "Failed to stop foreground service" }
        }
    }
}
