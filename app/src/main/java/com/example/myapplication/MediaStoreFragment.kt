package com.example.myapplication

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.example.myapplication.databinding.FragmentMediaStoreBinding

class MediaStoreFragment : Fragment() {

    var binding: FragmentMediaStoreBinding? = null

    val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->

        val granted = permissions.entries.all { it.value }
        if (granted) {
            // quyền đã được cấp, thực hiện hành động cần thiết
            Log.d("permission status", "Permissions granted: ${permissions.keys}")
        } else {
            // quyền bị từ chối, hiển thị thông báo hoặc xử lý theo cách khác
            Log.d("permission status", "Permissions denied: ${permissions.keys}")
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

            requestPermissionsForCurrentVersion()

        }
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

}