package smith.melton.locationprovider

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import smith.melton.locationprovider.R
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var startServiceButton: Button
    private lateinit var stopServiceButton: Button
    private lateinit var clearLogButton: Button
    private lateinit var serviceStatusTextView: TextView
    private lateinit var logTextView: TextView
    private lateinit var settingsButton: Button
    private var isServiceRunning = false
    private lateinit var sharedPreferences: SharedPreferences
    private var pollInterval = 5
    private var locationProvider = LocationManager.GPS_PROVIDER
    private var udpIpAddress = "YOUR_SERVER_IP"

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.all { it.value }) {
                startLocationService()
            }
        }

    private val logReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val logMessage = intent?.getStringExtra("logMessage")
            if (logMessage != null) {
                appendLog(logMessage)
            }
        }
    }

    private val settingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                pollInterval = data?.getIntExtra("newInterval", 5) ?: 5
                locationProvider = data?.getStringExtra("newProvider") ?: LocationManager.GPS_PROVIDER
                udpIpAddress = data?.getStringExtra("newIpAddress") ?: "YOUR_SERVER_IP"
                Toast.makeText(this, "New interval: $pollInterval, New provider: $locationProvider, New IP: $udpIpAddress", Toast.LENGTH_SHORT).show()
                updateServiceSettings()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startServiceButton = findViewById(R.id.startServiceButton)
        stopServiceButton = findViewById(R.id.stopServiceButton)
        clearLogButton = findViewById(R.id.clearLogButton)
        serviceStatusTextView = findViewById(R.id.serviceStatusTextView)
        logTextView = findViewById(R.id.logTextView)
        settingsButton = findViewById(R.id.settingsButton)

        sharedPreferences = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        pollInterval = sharedPreferences.getInt("pollInterval", 5)
        locationProvider = sharedPreferences.getString("locationProvider", LocationManager.GPS_PROVIDER) ?: LocationManager.GPS_PROVIDER

        startServiceButton.setOnClickListener {
            checkPermissionsAndStartService()
        }

        stopServiceButton.setOnClickListener {
            stopLocationService()
        }

        clearLogButton.setOnClickListener {
            clearLog()
        }

        settingsButton.setOnClickListener {
            openSettings()
        }
//        registerReceiver(logReceiver, IntentFilter("com.example.yourapp.LOG_UPDATE"))
    }

    override fun onDestroy() {
        super.onDestroy()
//        unregisterReceiver(logReceiver)
    }

    private fun checkPermissionsAndStartService() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permissions.all {
                ContextCompat.checkSelfPermission(
                    this,
                    it
                ) == PackageManager.PERMISSION_GRANTED
            }) {
            startLocationService()
        } else {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationPollService::class.java)
        serviceIntent.putExtra("pollInterval", pollInterval)
        serviceIntent.putExtra("locationProvider", locationProvider)
        serviceIntent.putExtra("udpIpAddress", udpIpAddress)
        ContextCompat.startForegroundService(this, serviceIntent)
        isServiceRunning = true
        updateServiceStatus()
    }

    private fun stopLocationService() {
        val serviceIntent = Intent(this, LocationPollService::class.java)
        stopService(serviceIntent)
        isServiceRunning = false
        updateServiceStatus()
    }

    private fun updateServiceStatus() {
        serviceStatusTextView.text = if (isServiceRunning) {
            "Service Status: Running"
        } else {
            "Service Status: Stopped"
        }
    }

    private fun appendLog(message: String) {
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val formattedDate = dateFormat.format(Date())
        val logMessage = "[$formattedDate] $message\n"
        runOnUiThread {
            logTextView.append(logMessage)
        }
    }

    private fun clearLog() {
        logTextView.text = ""
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        settingsLauncher.launch(intent)
    }

    private fun updateServiceSettings() {
        val intent = Intent("com.example.yourapp.UPDATE_SETTINGS")
        intent.putExtra("newInterval", pollInterval)
        intent.putExtra("newProvider", locationProvider)
        sendBroadcast(intent)
    }
}