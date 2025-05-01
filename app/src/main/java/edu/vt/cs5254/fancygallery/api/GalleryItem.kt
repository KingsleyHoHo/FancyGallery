package edu.vt.cs5254.fancygallery.api

import android.net.Uri
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GalleryItem(
    val title: String,
    val id: String,
    val owner: String,
    @Json(name = "url_s") val url: String,
) {
    val photoPageUri: Uri
        get() = Uri.parse("https://www.prof-oliva.com/vt/cs5254/photos/").buildUpon()
            .appendPath(owner).appendPath(id).build()
}