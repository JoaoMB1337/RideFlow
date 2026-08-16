package com.rideflow.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.rideflow.db.RideFlowDb

class DatabaseDriverFactory(private val context: Context) {
    fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = RideFlowDb.Schema,
            context = context,
            name = "RideFlowDb.db",
            // O SQLite tem as foreign keys desligadas por omissão, pelo que sem isto o
            // ON DELETE CASCADE de TrainingData nunca dispara e apagar um Train deixa
            // milhares de samples órfãos na base de dados.
            callback = object : AndroidSqliteDriver.Callback(RideFlowDb.Schema) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.setForeignKeyConstraintsEnabled(true)
                }
            }
        )
    }
}
