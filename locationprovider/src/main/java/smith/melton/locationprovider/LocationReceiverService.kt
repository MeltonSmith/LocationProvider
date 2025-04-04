package smith.melton.locationprovider

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import smith.melton.locationprovider.BroadcastLogger.logMessageViaBroadCast
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean


class LocationReceiverService : Service(){
    private var udpSocket: DatagramSocket? = null
    private var udpPort = 2004
    private val channelId = "UdpReceiverServiceChannel"
    private val notificationId = 2
    private var isRunning = AtomicBoolean(false)
    private lateinit var locationManager: LocationManager
    private val mockProviderName = "LocationProviderMock"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(notificationId, createNotification())
        logMessageViaBroadCast(applicationContext,"UDP Receiver Service created")

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        setupMockLocationProvider()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        udpPort = intent?.getIntExtra(UDP_PORT, 2004) ?: 2004
        startListening()
        return START_STICKY
    }

    override fun onDestroy() {
        stopListening()
        logMessageViaBroadCast(applicationContext,"UDP Receiver Service destroyed")
        removeMockLocationProvider()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startListening() {
        if (isRunning.get()) return
        isRunning.set(true)
        Thread {
            receiverWorker()
        }.start()
    }

    private fun receiverWorker(): Unit {
        try {
            udpSocket = DatagramSocket(udpPort)
            val buffer = ByteArray(1024)
            logMessageViaBroadCast(applicationContext,"UDP Receiver started on port: $udpPort")
            while (isRunning.get()) {
                val packet = DatagramPacket(buffer, buffer.size)
                udpSocket?.receive(packet)
                val message = String(packet.data, 0, packet.length)
                Log.d("UdpReceiverService", "Received: $message")
                logMessageViaBroadCast(applicationContext,"Received: $message")
                val location = parseLocationMessage(message)
                if (location != null) {
                    sendLocationToUI(location)
                    updateMockLocation(location)
                }
            }
        } catch (e: Exception) {
            Log.e("UdpReceiverService", "Error receiving UDP packet", e)
            logMessageViaBroadCast(applicationContext,"Error receiving UDP packet: ${e.message}")
        } finally {
            udpSocket?.close()
            udpSocket = null
        }
    }

    private fun stopListening() {
        isRunning.set(false)
        udpSocket?.close()
        udpSocket = null
        logMessageViaBroadCast(applicationContext,"UDP Receiver stopped")
    }

    private fun parseLocationMessage(message: String): LocationData? {
        val parts = message.split(",")
        if (parts.size == 3) {
            try {
                val latitude = parts[0].toDouble()
                val longitude = parts[1].toDouble()
                val time = parts[2]
                return LocationData(latitude, longitude, time)
            } catch (e: NumberFormatException) {
                Log.e("UdpReceiverService", "Error parsing location message", e)
                logMessageViaBroadCast(applicationContext,"Error parsing location message: ${e.message}")
            }
        }
        return null
    }

    private fun sendLocationToUI(location: LocationData) {
        val locationMessage = "Received Location: Lat=${location.latitude}, Lon=${location.longitude}, Time=${location.time}"
        logMessageViaBroadCast(applicationContext, locationMessage)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId,
                "UDP Receiver Service Channel",
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
            .setContentTitle("UDP Receiver Service")
            .setContentText("Listening for UDP packets...")
            .setSmallIcon(R.mipmap.ic_launcher_adaptive_fore_circle)
            .setBadgeIconType(R.mipmap.ic_launcher_circle)
            .setContentIntent(pendingIntent)
            .build()
    }


    private fun setupMockLocationProvider() {
        try {
            locationManager.addTestProvider(
                mockProviderName,
                false,
                false,
                false,
                false,
                true,
                true,
                true,
                ProviderProperties.POWER_USAGE_LOW,
                ProviderProperties.ACCURACY_FINE
            )
            locationManager.setTestProviderEnabled(mockProviderName, true)
        } catch (e: SecurityException) {
            Log.e("UdpReceiverService", "Error setting up mock location provider", e)
            logMessageViaBroadCast(applicationContext, "Error setting up mock location provider: ${e.message}")
        }
    }

    private fun removeMockLocationProvider() {
        try {
            locationManager.removeTestProvider(mockProviderName)
        } catch (e: Exception) {
            Log.e("UdpReceiverService", "Error removing mock location provider", e)
            logMessageViaBroadCast(applicationContext, "Error removing mock location provider: ${e.message}")
        }
    }

    private fun updateMockLocation(locationData: LocationData) {
        try {
            val location = Location(mockProviderName)
            location.latitude = locationData.latitude
            location.longitude = locationData.longitude
            location.time = System.currentTimeMillis()
            location.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            locationManager.setTestProviderLocation(mockProviderName, location)
        } catch (e: SecurityException) {
            Log.e("UdpReceiverService", "Error updating mock location", e)
            logMessageViaBroadCast(applicationContext, "Error updating mock location: ${e.message}")
        }
    }
}

data class LocationData(val latitude: Double, val longitude: Double, val time: String)