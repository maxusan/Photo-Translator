package com.batit.phototranslator.core.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.databinding.BindingAdapter
import com.batit.phototranslator.core.CustomView
import com.batit.phototranslator.core.data.Language
import com.bumptech.glide.Glide
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*


fun Context.checkPermissions(vararg permissions: String, granted: (Boolean) -> Unit) {
    Dexter.withContext(this)
        .withPermissions(
            *permissions
        ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                granted(report.areAllPermissionsGranted())
            }

            override fun onPermissionRationaleShouldBeShown(
                permissions: List<PermissionRequest?>?,
                token: PermissionToken?
            ){}
        }).check()
}

@BindingAdapter("setLanguageImage")
fun ImageView.setLanguageImage(language: Language){
    Glide.with(this).load(language.icon).into(this)
}

fun CustomView.saveTranslationToGallery(){
//    val bitmap = drawable.toBitmap(width, height, Bitmap.Config.ARGB_8888)
    isDrawingCacheEnabled = true
    val bitmap: Bitmap = drawingCache
    save(context, UUID.randomUUID().toString(), bitmap)
}

private const val SKINS_DIR = "Translator"
val defaultDirectory = Environment.DIRECTORY_DOWNLOADS + File.separator

fun Bitmap.toFile(stream: OutputStream){
    compress(Bitmap.CompressFormat.PNG, 100, stream)
}

fun save(context: Context, fileName: String, bitmap: Bitmap): Boolean {
    try {
        val stream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver?.let { resolver ->
                val values = getValues(defaultDirectory, fileName)
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)?.let {
                    resolver.openOutputStream(it)
                }
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(defaultDirectory)
            val image = File(downloadsDir, "$fileName.png")
            FileOutputStream(image)
        }
        stream?.let { bitmap.toFile(it) }
        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
        return true
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun getValues(path: String, fileName: String): ContentValues {
    return ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        put(MediaStore.MediaColumns.RELATIVE_PATH, path)
    }
}