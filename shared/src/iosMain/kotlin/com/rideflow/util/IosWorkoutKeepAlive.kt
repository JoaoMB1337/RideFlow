package com.rideflow.util

import com.rideflow.domain.session.WorkoutKeepAlive

/**
 * No iOS o equivalente ao foreground service é declarar o background mode
 * `bluetooth-central` no Info.plist — o sistema mantém a app a correr enquanto houver uma
 * ligação BLE activa, sem precisar de nada em runtime. Enquanto esse modo não estiver
 * declarado, isto é um no-op consciente e o treino só grava com a app em primeiro plano.
 */
class IosWorkoutKeepAlive : WorkoutKeepAlive {
    override fun acquire() = Unit
    override fun release() = Unit
}
