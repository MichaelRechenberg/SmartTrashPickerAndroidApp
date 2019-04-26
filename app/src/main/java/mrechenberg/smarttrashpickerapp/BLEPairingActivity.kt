package mrechenberg.smarttrashpickerapp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_blepairing.*

class BLEPairingActivity : AppCompatActivity() {

    var isActivelyScanning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blepairing)

        // Init recycler view
        var discoveredDevicesRecyclerView = ble_discovered_devices_recyclerview
        discoveredDevicesRecyclerView.layoutManager = LinearLayoutManager(this@BLEPairingActivity)

        // TODO: set adapter of recycler view based on the results of BLE scan

        // Allow the user to rescan by clicking near the scan text
        var scanStatusLayout = ble_pairing_scan_control_layout
        var scanStatusTextView = ble_start_scan_textview

        scanStatusLayout.setOnClickListener {v ->
            if (isActivelyScanning){
                isActivelyScanning = !isActivelyScanning
                scanStatusTextView.setText(R.string.ble_start_scan_text_not_scanning)
            }
            else {
                isActivelyScanning = !isActivelyScanning
                scanStatusTextView.setText(R.string.ble_start_scan_text_actively_scanning)


                // TODO: actually do the BLE scan, and when the scan times-out or user connects,
                //   set the scan status text to not_scanning variant


                // TODO: set adapter of recycler view based on the results of BLE scan
            }
        }


    }
}
