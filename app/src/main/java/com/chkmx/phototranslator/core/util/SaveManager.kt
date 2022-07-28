package com.batit.phototranslator.core.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*

object SaveManager {

    private val defaultDirectory = Environment.DIRECTORY_DOWNLOADS + File.separator


    fun saveImage(context: Context, bitmap: Bitmap): String {
        var path: String = ""
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(defaultDirectory)
            val fileName = "Image_${UUID.randomUUID().toString()}.jpg"
            val image = File(downloadsDir, fileName)
            path = image.path
            val stream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver?.let { resolver ->
                    val values = getValues(defaultDirectory, fileName)
                    resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)?.let {
                        resolver.openOutputStream(it)
                    }
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(defaultDirectory)
                val image = File(downloadsDir, "Image_${UUID.randomUUID().toString()}.jpg")
                path = image.path
                FileOutputStream(image)
            }
            stream?.let { bitmap.toFile(it) }
//            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return path
    }

    private fun Bitmap.toFile(stream: OutputStream) {
        compress(Bitmap.CompressFormat.JPEG, 70, stream)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getValues(path: String, fileName: String): ContentValues {
        return ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, path)
        }
    }
}