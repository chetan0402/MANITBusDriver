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
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class TrackActivity : AppCompatActivity() {
    private lateinit var codeScanner: CodeScanner
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
        val scannerView = findViewById<CodeScannerView>(R.id.qr)
        val stopButton = findViewById<Button>(R.id.stopButton)
        codeScanner = CodeScanner(this,scannerView)
        scanQR.setOnClickListener {
            scannerView.visibility = View.VISIBLE
            codeScanner.startPreview()
        }

        codeScanner.decodeCallback = DecodeCallback {
            qrText=it.text
            codeScanner.stopPreview()
            codeScanner.releaseResources()
            runOnUiThread {
                scannerView.visibility = View.GONE
                scanQR.visibility = View.GONE
                stopButton.visibility = View.VISIBLE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(Intent(this,LocationService::class.java))
            }else{
                startService(Intent(this,LocationService::class.java))
            }
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
        if(findViewById<CodeScannerView>(R.id.qr).visibility == View.VISIBLE && ::codeScanner.isInitialized) codeScanner.startPreview()
    }

    override fun onPause() {
        super.onPause()
        if(::codeScanner.isInitialized ) codeScanner.stopPreview()
    }

    companion object {
        var qrText = ""
    }
}