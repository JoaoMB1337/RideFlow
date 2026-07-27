package com.rideflow.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.rideflow.db.RideFlowDb

class DatabaseDriverFactory(private val context: Context) {
    fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(RideFlowDb.Schema, context, "RideFlowDb.db")
    }
}
