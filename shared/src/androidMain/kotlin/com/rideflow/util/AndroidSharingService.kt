package com.rideflow.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

class AndroidSharingService(private val context: Context) : SharingService {
    override fun shareText(text: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(shareIntent)
    }

    override fun shareImage(bytes: ByteArray, fileName: String) {
        val uri = saveImageToGallery(bytes, fileName)
        if (uri != null) {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/png"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val shareIntent = Intent.createChooser(sendIntent, "Partilhar Treino").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(shareIntent)
        }
    }

    private fun saveImageToGallery(bytes: ByteArray, fileName: String): Uri? {
        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/RideFlow")
            }
        }

        val uri = context.contentResolver.insert(imageCollection, contentValues)
        return uri?.also {
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                outputStream.write(bytes)
            }
        }
    }
}
