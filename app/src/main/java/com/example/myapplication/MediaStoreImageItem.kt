package com.example.myapplication

data class MediaStoreImageItem(
    val id: Long,
    val name: String,
    val sizeBytes: Long,
    val contentUri: android.net.Uri
)