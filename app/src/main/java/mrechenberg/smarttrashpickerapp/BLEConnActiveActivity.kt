package mrechenberg.smarttrashpickerapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_bleconn_active.*

/**
 * Activity to display while there is active BLE connection
 *  with a trash picker.  The user can disconnect from
 *  the trash picker from this activity
 */
class BLEConnActiveActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bleconn_active)

        val disconnectButton = stop_ble_service_button

        disconnectButton.setOnClickListener {
            Log.d("REE", "Stopping STPBLEService")
            var stpBleIntent = Intent(this@BLEConnActiveActivity, STPBLEService::class.java)
            stopService(stpBleIntent)

            var t = Toast.makeText(this@BLEConnActiveActivity, "Succesfully disconnected", Toast.LENGTH_SHORT)
            t.show()

            finish()
        }

    }
}
