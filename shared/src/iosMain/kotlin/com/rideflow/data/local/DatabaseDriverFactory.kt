package com.rideflow.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.rideflow.db.RideFlowDb

class DatabaseDriverFactory {
    fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = RideFlowDb.Schema,
            name = "RideFlowDb.db",
            // Ver a nota no driver Android: sem isto o ON DELETE CASCADE não é aplicado.
            onConfiguration = { config ->
                config.copy(
                    extendedConfig = config.extendedConfig.copy(foreignKeyConstraints = true)
                )
            }
        )
    }
}
