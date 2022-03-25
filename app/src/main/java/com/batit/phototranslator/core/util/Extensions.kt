package com.batit.phototranslator.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.batit.phototranslator.core.data.Language
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener


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

fun Context.getImageFromUri(uri: Uri, callback: (Bitmap) -> Unit){
    Glide.with(this)
        .asBitmap()
        .load(uri)
        .into(object : CustomTarget<Bitmap>(){
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                callback(resource)
            }
            override fun onLoadCleared(placeholder: Drawable?) {

            }
        })
}

@BindingAdapter("setLanguageImage")
fun ImageView.setLanguageImage(language: Language){
    Glide.with(this).load(language.icon).into(this)
}