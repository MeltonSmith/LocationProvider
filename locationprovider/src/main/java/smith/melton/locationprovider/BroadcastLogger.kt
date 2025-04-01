package smith.melton.locationprovider

import android.content.Context
import android.content.Intent



object BroadcastLogger {

    private const val SMITH_MELTON_LOCATION_LOG_UPDATE = "smith.melton.location.LOG_UPDATE"

    fun logMessageViaBroadCast(context: Context, message: String) {
        val i = Intent(SMITH_MELTON_LOCATION_LOG_UPDATE)
        i.putExtra("logMessage", message)
        context.sendBroadcast(i)
    }
}