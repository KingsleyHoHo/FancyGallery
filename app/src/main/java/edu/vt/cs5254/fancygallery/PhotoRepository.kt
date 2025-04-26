package edu.vt.cs5254.fancygallery

import edu.vt.cs5254.fancygallery.api.FlickrApi
import edu.vt.cs5254.fancygallery.api.GalleryItem
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create

class PhotoRepository {
    private val flickrApi: FlickrApi

    init {
        val retrofit: Retrofit =
            Retrofit.Builder().baseUrl("https://www.prof-oliva.com/vt/cs5254/")
                .addConverterFactory(MoshiConverterFactory.create()).build()

        flickrApi = retrofit.create<FlickrApi>()
    }

    suspend fun fetchPhotos(perPage: Int = 50): List<GalleryItem> =
        flickrApi.fetchPhotos(perPage).photos.galleryItem
}