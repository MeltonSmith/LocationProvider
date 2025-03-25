package smith.melton.locationprovider

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.LocationManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity

const val POLL_INTERVAL = "pollInterval"
const val LOCATION_PROVIDER = "locationProvider"
const val UDP_IP_ADDRESS = "udpIpAddress"
const val UDP_PORT = "udpPort"

class SettingsActivity : AppCompatActivity() {

    private lateinit var intervalEditText: EditText
    private lateinit var providerSpinner: Spinner
    private lateinit var ipAddressEditText: EditText
    private lateinit var portEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        intervalEditText = findViewById(R.id.intervalEditText)
        providerSpinner = findViewById(R.id.providerSpinner)
        ipAddressEditText = findViewById(R.id.ipAddressEditText)
        portEditText = findViewById(R.id.portEditText)
        saveButton = findViewById(R.id.saveButton)
        sharedPreferences = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        val currentInterval = sharedPreferences.getLong(POLL_INTERVAL, 5000)
        intervalEditText.setText(currentInterval.toString())

        val providers = arrayOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, providers)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        providerSpinner.adapter = adapter

        val currentProvider = sharedPreferences.getString(LOCATION_PROVIDER, LocationManager.GPS_PROVIDER)
        val selectedPosition = providers.indexOf(currentProvider)
        providerSpinner.setSelection(selectedPosition)

        val currentIpAddress = sharedPreferences.getString(UDP_IP_ADDRESS, "192.168.100.2")
        ipAddressEditText.setText(currentIpAddress)

        //TODO global variables
        val currentUpdPort = sharedPreferences.getString(UDP_PORT, "2004")
        portEditText.setText(currentUpdPort)

        saveButton.setOnClickListener {
            saveSettings()
        }
    }

    private fun saveSettings() {
        val newInterval = intervalEditText.text.toString().toLongOrNull()
        val newProvider = providerSpinner.selectedItem.toString()
        val newIpAddress = ipAddressEditText.text.toString()
        val newPort = portEditText.text.toString().toLongOrNull()
        if (newInterval != null && newInterval >= 0) {
            sharedPreferences.edit()
                .putLong(POLL_INTERVAL, newInterval)
                .putString(LOCATION_PROVIDER, newProvider)
                .putString(UDP_IP_ADDRESS, newIpAddress)
                .putLong(UDP_PORT, newPort ?: 2004)
                .apply()
            val resultIntent = Intent()
            resultIntent.putExtra("newInterval", newInterval)
            resultIntent.putExtra("newProvider", newProvider)
            resultIntent.putExtra("newIpAddress", newIpAddress)
            resultIntent.putExtra("newPort", newPort)
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}