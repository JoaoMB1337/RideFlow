package com.rideflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.rideflow.data.bluetooth.BlePermissionHandler
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    
    private val blePermissionHandler: BlePermissionHandler by inject()
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle results if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (!blePermissionHandler.hasPermissions()) {
            requestPermissionLauncher.launch(blePermissionHandler.getPermissions())
        }

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}