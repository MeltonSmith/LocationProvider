package smith.melton.locationprovider

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationPollService : Service() {

    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var sharedPreferences: SharedPreferences
    private var udpSocket: DatagramSocket? = null
    private val serverAddress = "YOUR_SERVER_IP" // Replace with your server IP
    private val serverPort = 12345 // Replace with your server port
    private val channelId = "LocationServiceChannel"
    private val notificationId = 1
    private val minDistanceBetweenUpdates = 0f // 0 meters



    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        sharedPreferences = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        createNotificationChannel()
        locationListener = createLocationListener()
        startForeground(notificationId, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startLocationUpdates()
        setServiceRunning(true)
        return START_STICKY
    }

    private fun setServiceRunning(isRunning: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("isServiceRunning", isRunning)
        editor.apply()
    }

    override fun onDestroy() {
        stopLocationUpdates()
        udpSocket?.close()
        super.onDestroy()
    }


    private fun createLocationListener(): LocationListener {
        return object : LocationListener {
            override fun onLocationChanged(location: Location) {
                sendLocationOverUDP(location)
            }

            override fun onProviderDisabled(provider: String) {
                Log.d("LocationService", "Provider disabled: $provider")
            }

            override fun onProviderEnabled(provider: String) {
                Log.d("LocationService", "Provider enabled: $provider")
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                Log.d("LocationService", "Provider status changed: $provider, status: $status")
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val locationProvider = sharedPreferences.getString("locationProvider", LocationManager.GPS_PROVIDER) ?: LocationManager.GPS_PROVIDER

        locationManager.getLastKnownLocation(locationProvider)?.let { processLocation(it) }

        locationManager.requestLocationUpdates(
            locationProvider,
            sharedPreferences.getLong("pollInterval", 5L),
            minDistanceBetweenUpdates,
            locationListener
        )
    }

    private fun stopLocationUpdates() {
        locationManager.removeUpdates(locationListener)
        setServiceRunning(false)
    }

    private fun sendLocationOverUDP(location: Location) {
        Thread {
            processLocation(location)
        }.start()
    }

    private fun processLocation(location: Location) {
        try {
            if (udpSocket == null) {
                udpSocket = DatagramSocket()
            }
            val message = formatLocationMessage(location)
            val i = Intent("smith.melton.location.LOG_UPDATE")
            i.putExtra("logMessage", message)
            sendBroadcast(i)
    //                val address = InetAddress.getByName(serverAddress)
    //                val data = message.toByteArray()
    //                val packet = DatagramPacket(data, data.size, address, serverPort)

    //                udpSocket?.send(packet)
            Log.d("LocationService", "Sent: $message")
        } catch (e: Exception) {
            Log.e("LocationService", "Error sending UDP packet", e)
        }
    }

    private fun formatLocationMessage(location: Location): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
            Locale.getDefault())
        val formattedDate = dateFormat.format(Date())
        return "${location.latitude},${location.longitude},$formattedDate"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId,
                "Location Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Location Service")
            .setContentText("Collecting location data...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}