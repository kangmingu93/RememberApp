package com.mk.remember

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import java.io.IOException


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private val fileList = arrayListOf<MyMarker>()
    private val markerList = arrayListOf<Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        TedPermission.create()
            .setPermissionListener(permissionListener)
            .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
            .setPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)
            .check()

        val fm = supportFragmentManager
        val mapFragment = fm.findFragmentById(R.id.map_fragment) as MapFragment?
            ?: MapFragment.newInstance().also {
                fm.beginTransaction().add(R.id.map_fragment, it).commit()
            }

        mapFragment.getMapAsync(this)
        getFileList()
    }

    @UiThread
    override fun onMapReady(naverMap: NaverMap) {
        fileList.forEach {
            val marker = Marker().apply {
                position = LatLng(it.lat, it.lng)
                icon = OverlayImage.fromBitmap(it.bitmap)
                map = naverMap
            }
            markerList.add(marker)
        }
    }

    private val permissionListener: PermissionListener = object : PermissionListener {
        override fun onPermissionGranted() {
            Toast.makeText(this@MainActivity, "Permission Granted", Toast.LENGTH_SHORT).show()
        }

        override fun onPermissionDenied(deniedPermissions: List<String?>) {
            Toast.makeText(
                this@MainActivity,
                "Permission Denied\n$deniedPermissions",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val options = BitmapFactory.Options().apply {
        this.inSampleSize = 20
    }

    private fun getFileList() {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME)

        val cursor = contentResolver.query(uri, projection, null, null, "${MediaStore.MediaColumns.DATE_ADDED} desc")
        val columnIndex = cursor?.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
        val columnDisplayName = cursor?.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
        var lastIndex = 0

        while (cursor?.moveToNext() == true) {
            val absolutePathOfImage = columnIndex?.let { cursor.getString(it) } ?: ""
            val nameOfFile = columnDisplayName?.let { cursor.getString(it) } ?: ""
            lastIndex = absolutePathOfImage.lastIndexOf(nameOfFile)
            lastIndex = if (lastIndex >= 0) lastIndex else nameOfFile.length.minus(1)

            if (TextUtils.isEmpty(absolutePathOfImage).not()) {
                try {
                    val exif = ExifInterface(absolutePathOfImage)
                    val output: FloatArray = floatArrayOf(0f, 0f)
                    if (exif.getLatLong(output)) {

                        fileList.add(
                            MyMarker(
                                BitmapFactory.decodeFile(absolutePathOfImage, options),
                                absolutePathOfImage,
                                output[0].toDouble(),
                                output[1].toDouble())
                        )
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        cursor?.close()
        Log.d("MainActivity", "totalFileSize = ${fileList.size}")

    }


}