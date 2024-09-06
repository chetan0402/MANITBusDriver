package me.chetan.manitbusdriver

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class TrackActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_track)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED){
            val locationPermissionRequest = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                when{
                    permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {

                    }
                    permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                        MaterialAlertDialogBuilder(this).run {
                            setTitle("Need fine location")
                            setPositiveButton("Retry") { _ , _ ->
                                startActivity(Intent(this@TrackActivity,TrackActivity::class.java))
                                finish()
                            }
                            setCancelable(false)
                            show()
                        }
                    }
                    permissions.getOrDefault(Manifest.permission.CAMERA, false) -> {

                    } else -> {
                        finish()
                    }
                }
            }
            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.CAMERA))
        }

        val scanQR = findViewById<Button>(R.id.scanQR)
        val stopButton = findViewById<Button>(R.id.stopButton)

        val qrLauncher = registerForActivityResult(ScanQRCode()) {
            if(it !is QRResult.QRSuccess) return@registerForActivityResult
            qrText = it.content.rawValue.toString()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(Intent(this,LocationService::class.java))
            }else{
                startService(Intent(this,LocationService::class.java))
            }
            this.lifecycleScope.launch(Dispatchers.IO){
                runOnUiThread {
                    scanQR.visibility = View.GONE
                    stopButton.visibility = View.VISIBLE
                }
            }
        }

        scanQR.setOnClickListener {
            qrLauncher.launch(null)
        }

        stopButton.setOnClickListener {
            runOnUiThread {
                MaterialAlertDialogBuilder(this).run {
                    setTitle("Confirm")
                    setMessage("End duty?")
                    setNegativeButton("No") { _ ,_ -> }
                    setPositiveButton("Yes") { _ , _ ->
                        stopService(Intent(this@TrackActivity,LocationService::class.java))
                        finish()
                    }
                    setCancelable(false)
                    show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if(LocationService.running){
            runOnUiThread {
                findViewById<Button>(R.id.scanQR).visibility = View.GONE
                findViewById<Button>(R.id.stopButton).visibility = View.VISIBLE
            }
        }else{
            runOnUiThread {
                findViewById<Button>(R.id.scanQR).visibility = View.VISIBLE
                findViewById<Button>(R.id.stopButton).visibility = View.GONE
            }
        }
    }

    override fun onPause() {
        super.onPause()
    }

    companion object {
        var qrText = ""
    }
}