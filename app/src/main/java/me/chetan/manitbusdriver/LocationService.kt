package me.chetan.manitbusdriver

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.chetan.manitbusdriver.Login.Companion.token
import me.chetan.manitbusdriver.MainActivity.Companion.BASE
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


class LocationService : Service() {
    private lateinit var notificationManager: NotificationManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var lastLocationUpdate : Long = 0
    private lateinit var chassisNo: String

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        doWork()
        chassisNo = TrackActivity.qrText
        return START_STICKY
    }

    private fun doWork() {
        notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "TrackBusChannel"
        createNotificationChannel(channelId)

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Tracking bus")
            .setContentText("Tracking bus")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .build()

        startForeground(1,notification)


        notificationManager.notify(1, notification)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if(System.currentTimeMillis() - lastLocationUpdate < 5000) return
                lastLocationUpdate = System.currentTimeMillis()
                val location = locationResult.lastLocation
                if (location != null ) {
                    GlobalScope.launch(Dispatchers.IO){
                        try{
                            (URL("$BASE/driver").openConnection() as HttpURLConnection).run {
                                setRequestProperty("Accept","application/json")
                                setRequestProperty("Content-Type", "application/json")
                                connectTimeout=2000
                                readTimeout=2000
                                requestMethod = "POST"
                                doOutput = true

                                val body = JSONObject().apply {
                                    put("lat", location.latitude)
                                    put("long", location.longitude)
                                    put("time", location.time / 1000)
                                    put("speed", location.speed)
                                    put("chassisNo", chassisNo)
                                    put("token", token)
                                }

                                outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

                                val responseCode = responseCode
                                if (responseCode == HttpURLConnection.HTTP_OK) {
                                    running = true
                                    val response = JSONObject(inputStream.bufferedReader().use { it.readText() })
                                    if(response.has("forceClose")){
                                        if(response.getBoolean("forceClose")) stopSelf()
                                    }
                                } else {
                                    when(responseCode){
                                        401 -> {
                                            token = ""
                                            getSharedPreferences("token", MODE_PRIVATE).edit().putString("token",
                                                token
                                            ).apply()
                                            stopSelf()
                                        }
                                    }
                                }
                            }
                        } catch (e:Exception){
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            fusedLocationClient.requestLocationUpdates(
                LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY,5000)
                    .setIntervalMillis(5000)
                    .setMinUpdateIntervalMillis(5000)
                    .build(),
                locationCallback,
                Looper.getMainLooper())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Bus Tracking Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for bus tracking notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        running = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    companion object{
        var running = false
    }
}
