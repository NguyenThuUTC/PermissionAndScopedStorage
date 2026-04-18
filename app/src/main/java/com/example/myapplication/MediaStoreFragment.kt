package com.example.myapplication

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.databinding.FragmentMediaStoreBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaStoreFragment : Fragment() {

    var binding: FragmentMediaStoreBinding? = null

    private var hasRequestedPermission = false
    private var isPermissionDeniedForever = false

    val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->

        val access = when {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] == true -> ImageAccess.FULL
            permissions[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true -> {
                ImageAccess.LIMITED
            }
            else -> ImageAccess.NONE
        }
        val deniedForever = access == ImageAccess.NONE &&
                hasRequestedPermission &&
                isDeniedForever(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                    Manifest.permission.READ_EXTERNAL_STORAGE,

                )

        handlePermissionResult(access, deniedForever)

    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun currentImageAccess(): ImageAccess {
        val imageAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                hasPermission(Manifest.permission.READ_MEDIA_IMAGES) -> ImageAccess.FULL
                hasPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> {
                    ImageAccess.LIMITED
                }

                else -> ImageAccess.NONE
            }
        } else if (hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            ImageAccess.FULL
        } else {
            ImageAccess.NONE
        }

        return imageAccess
    }

    private fun handlePermissionResult(access: ImageAccess, deniedForever: Boolean) {
        isPermissionDeniedForever = deniedForever

        if (access == ImageAccess.NONE) {
            Log.d("Mediastore fragment", "Image access denied")
            return
        }

        loadImages()
    }

    private fun hasAnyImageAccess(): Boolean {
        return currentImageAccess() != ImageAccess.NONE
    }

    private fun loadImages() {
        if (!hasAnyImageAccess()) {
            return
        }

        lifecycleScope.launch {
            val images = withContext(Dispatchers.IO) {
                queryImages()
            }

            Log.d("getImages", "${images.size}")
        }
    }

    private fun queryImages(): List<MediaStoreImageItem> {
        val resolver = requireContext().contentResolver
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val images = mutableListOf<MediaStoreImageItem>()



        resolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "unknown name"
                val sizeBytes = if (cursor.isNull(sizeColumn)) 0L else cursor.getLong(sizeColumn)
                val contentUri = ContentUris.withAppendedId(collection, id)

                images += MediaStoreImageItem(
                    id = id,
                    name = name,
                    sizeBytes = sizeBytes,
                    contentUri = contentUri
                )
            }
        }

        return images
    }

    private fun isDeniedForever(vararg permissions: String): Boolean {
        return permissions.all { permission ->
            !shouldShowRequestPermissionRationale(permission)
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentMediaStoreBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding?.btnRequestPermission?.setOnClickListener {
            handleRequestPermissionClick()
        }

        loadImages()
    }

    private fun handleRequestPermissionClick() {
        val currentAccess = currentImageAccess()
        if (currentAccess != ImageAccess.NONE) {//granted
            isPermissionDeniedForever = false
            loadImages()
        }

        if (isPermissionDeniedForever) {
            openAppSettings()
        } else {
            hasRequestedPermission = true
            requestPermissionsForCurrentVersion()
        }
    }

    private fun openAppSettings() {
        // Open the app settings screen so the user can change permission manually.
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", requireContext().packageName, null)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun requestPermissionsForCurrentVersion() {
        requestPermission.launch(getLegacyReadPermission().toTypedArray())
    }

    private fun getLegacyReadPermission(): List<String> {
        // Android 13 uses READ_MEDIA_IMAGES. Older versions use READ_EXTERNAL_STORAGE.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private enum class ImageAccess {
        NONE,
        LIMITED,
        FULL
    }

}