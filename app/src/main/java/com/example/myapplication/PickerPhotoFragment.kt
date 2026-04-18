package com.example.myapplication

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.myapplication.databinding.FragmentPickerPhotoBinding
import java.io.File

class PickerPhotoFragment : Fragment() {

    var binding: FragmentPickerPhotoBinding? = null

    val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            // xử lý ảnh
            Log.d("PickerPhotoFragment", "Picked image Uri: $uri")

            binding?.run {
                Glide.with(this@PickerPhotoFragment)
                    .load(uri)
                    .into(imgPhoto)

                showFileInfo(uri)
            }

            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                val bytes = input.readBytes()
            }


        }
    }

    val pickMultiplePhoto = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            // xử lý nhiều ảnh
            Log.d("PickerPhotoFragment", "Picked multiple image Uris: $uris")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentPickerPhotoBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding?.btnPickPhoto?.setOnClickListener {
            pickImage.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
//            pickMultiplePhoto.launch(
//                PickVisualMediaRequest(
//                    ActivityResultContracts.PickVisualMedia.VideoOnly
//                )
//            )
        }

        binding?.btnOpenMediaStore?.setOnClickListener {
            findNavController().navigate(R.id.mediaStoreFragment)
        }
    }

    private fun showFileInfo(uri: Uri?) = binding?.run {
        if (uri == null) {
            txtPhotoInfo.text = ""
            return@run
        }

        val resolver = requireContext().contentResolver
        var displayName = "Unknown"
        var sizeBytes: Long? = null

        resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                if (nameIndex != -1) {
                    displayName = cursor.getString(nameIndex)
                }
                if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                    sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        }

        if (sizeBytes == null) {
            sizeBytes = resolver.openInputStream(uri)?.use { inputStream ->
                var total = 0L
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

                while (true) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break
                    total += bytesRead
                }
                total
            }
        }

        txtPhotoInfo.text = getString(R.string.file_name_format, displayName, sizeBytes ?: 0L)
    }




}