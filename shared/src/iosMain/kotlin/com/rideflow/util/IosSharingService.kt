package com.rideflow.util

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage

class IosSharingService : SharingService {
    override fun shareText(text: String) {
        val window = UIApplication.sharedApplication.keyWindow
        val rootViewController = window?.rootViewController
        
        val activityViewController = UIActivityViewController(listOf(text), null)
        rootViewController?.presentViewController(activityViewController, true, null)
    }

    override fun shareImage(bytes: ByteArray, fileName: String) {
        // Simple implementation for iOS
        val window = UIApplication.sharedApplication.keyWindow
        val rootViewController = window?.rootViewController

        // val nsData = NSData.create(bytes = bytes.toCValues(), length = bytes.size.toULong())
        // val image = UIImage.imageWithData(nsData)
        // val activityViewController = UIActivityViewController(listOf(image), null)
        // rootViewController?.presentViewController(activityViewController, true, null)
    }
}
