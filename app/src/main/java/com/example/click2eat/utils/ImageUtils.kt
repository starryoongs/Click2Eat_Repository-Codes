package com.example.click2eat.utils

import android.content.Context

object ImageUtils {

    fun getImageResource(
        context: Context,
        imageName: String?
    ): Int {

        if (imageName.isNullOrBlank()) {
            return android.R.drawable.ic_menu_gallery
        }

        val resourceName = imageName
            .substringAfterLast("/")
            .substringBeforeLast(".")
            .lowercase()

        val resourceId = context.resources.getIdentifier(
            resourceName,
            "drawable",
            context.packageName
        )

        return if (resourceId != 0) {
            resourceId
        } else {
            android.R.drawable.ic_menu_gallery
        }
    }
}
