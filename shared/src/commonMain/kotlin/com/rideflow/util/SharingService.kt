package com.rideflow.util

interface SharingService {
    fun shareText(text: String)
    fun shareImage(bytes: ByteArray, fileName: String)
}
