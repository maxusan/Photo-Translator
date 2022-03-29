package com.batit.phototranslator.core.util

import android.content.*
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.Toast
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import com.batit.phototranslator.core.data.Language
import com.batit.phototranslator.core.db.PhotoItem
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.imageview.ShapeableImageView
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File


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
            ) {
            }
        }).check()
}

fun Fragment.shareText(text: String) {
    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "text/plain"
    intent.putExtra(
        Intent.EXTRA_SUBJECT,
        text
    )
    intent.putExtra(Intent.EXTRA_TEXT, text)
    startActivity(Intent.createChooser(intent, "Share"))
}

fun Context.getRealPathFromURI( contentUri: Uri): String? {
    val column = arrayOf(MediaStore.Images.ImageColumns.DATA)

    val cursor = contentResolver.query(
        contentUri,
        column, null, null, null
    )

    var filePath: String? = ""

    val columnIndex = cursor!!.getColumnIndex(column[0])

    if (cursor!!.moveToFirst()) {
        filePath = cursor!!.getString(columnIndex)
    }
    cursor!!.close()
    return filePath
}

@BindingAdapter("setPhoto")
fun ShapeableImageView.setPhoto(photoItem: PhotoItem){
    Glide.with(this).load(photoItem.photoUri).into(this)
}

fun Uri.getMimeType(context: Context): String? {
    val extension: String? = if (this.scheme == ContentResolver.SCHEME_CONTENT) {
        val mime = MimeTypeMap.getSingleton()
        mime.getExtensionFromMimeType(context.contentResolver.getType(this))
    } else {
         MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(File(this.path)).toString())
    }
    return extension
}

fun Fragment.copyTextToClipboard(text: String) {
    val clipboard: ClipboardManager? =
        requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
    val clip = ClipData.newPlainText("Text", text)
    clipboard?.setPrimaryClip(clip)
    Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
}

fun Context.getImageFromUri(uri: Uri, callback: (Bitmap) -> Unit) {
    Glide.with(this)
        .asBitmap()
        .load(uri)
        .into(object : CustomTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                callback(resource)
            }

            override fun onLoadCleared(placeholder: Drawable?) {

            }
        })
}

@BindingAdapter("setLanguageImage")
fun ImageView.setLanguageImage(language: Language) {
    Glide.with(this).load(language.icon).into(this)
}