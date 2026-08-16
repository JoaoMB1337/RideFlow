package com.rideflow.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/**
 * Segura o processo vivo enquanto há um treino a decorrer.
 *
 * Sem isto o Android pode suspender ou matar o processo assim que a app vai para background —
 * o utilizador põe o telemóvel de lado a meio do treino e a ligação BLE cai com a gravação.
 *
 * O serviço não faz trabalho nenhum: quem grava é o `WorkoutSessionManager`. Isto é apenas o
 * mecanismo de plataforma que garante que ele continua a poder correr.
 */
class WorkoutForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("RideFlow")
            .setContentText("Treino em curso")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // START_STICKY: se o sistema nos matar por pressão de memória, volta a arrancar
        // o serviço e o treino sobrevive.
        return START_STICKY
    }

    private fun createChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Treino em curso",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantém a gravação do treino activa com o ecrã desligado"
                setShowBadge(false)
            }
        )
    }

    companion object {
        private const val CHANNEL_ID = "rideflow_workout"
        private const val NOTIFICATION_ID = 1001
    }
}
