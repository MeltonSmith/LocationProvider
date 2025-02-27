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

class SettingsActivity : AppCompatActivity() {

    private lateinit var intervalEditText: EditText
    private lateinit var providerSpinner: Spinner
    private lateinit var ipAddressEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        intervalEditText = findViewById(R.id.intervalEditText)
        providerSpinner = findViewById(R.id.providerSpinner)
        ipAddressEditText = findViewById(R.id.ipAddressEditText)
        saveButton = findViewById(R.id.saveButton)
        sharedPreferences = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        val currentInterval = sharedPreferences.getInt("pollInterval", 5)
        intervalEditText.setText(currentInterval.toString())

        val providers = arrayOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, providers)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        providerSpinner.adapter = adapter

        val currentProvider = sharedPreferences.getString("locationProvider", LocationManager.GPS_PROVIDER)
        val selectedPosition = providers.indexOf(currentProvider)
        providerSpinner.setSelection(selectedPosition)

        val currentIpAddress = sharedPreferences.getString("udpIpAddress", "YOUR_SERVER_IP")
        ipAddressEditText.setText(currentIpAddress)

        saveButton.setOnClickListener {
            saveSettings()
        }
    }

    private fun saveSettings() {
        val newInterval = intervalEditText.text.toString().toIntOrNull()
        val newProvider = providerSpinner.selectedItem.toString()
        val newIpAddress = ipAddressEditText.text.toString()
        if (newInterval != null && newInterval > 0) {
            sharedPreferences.edit()
                .putInt("pollInterval", newInterval)
                .putString("locationProvider", newProvider)
                .putString("udpIpAddress", newIpAddress)
                .apply()
            val resultIntent = Intent()
            resultIntent.putExtra("newInterval", newInterval)
            resultIntent.putExtra("newProvider", newProvider)
            resultIntent.putExtra("newIpAddress", newIpAddress)
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}