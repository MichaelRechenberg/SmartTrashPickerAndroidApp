package mrechenberg.smarttrashpickerapp

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_blepairing.*

class BLEPairingActivity : AppCompatActivity() {

    // Flag for Bluetooth intent
    val REQUEST_ENABLE_BT = 1

    // Number of ms to use for a scanning period
    val SCAN_PERIOD : Long = 10000

    // Flag indicating if we are currently scanning for Bluetooth devices
    var isActivelyScanning = false

    var bleScanner : BluetoothLeScanner? = null


    var bleScanCallback : ScanCallback? = null


    /**
     * Taken from Android bluetooth tutorial https://developer.android.com/guide/topics/connectivity/bluetooth-le.html
     */
    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blepairing)

        bleScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)


                // I don't think we need any more information from the ScanResult besides the
                //    BluetoothDevice
                var foundBLEDevice = result?.device

                var toastMessage = "Found BLE device with address $foundBLEDevice.address"

                Log.d("REE", toastMessage)

                // TODO: use this BLE device to feed into the Adapter for RecyclerView of
                //    BLE devices


            }
        }

        // Request that the user enable Bluetooth, if it isn't turned on already
        bluetoothAdapter?.takeIf { !it.isEnabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        // Initialize BLE scanner
        bleScanner = bluetoothAdapter?.bluetoothLeScanner

        scanForBLEDevices(SCAN_PERIOD)




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

                stopScanForBLEDevices()
            }
            else {
                isActivelyScanning = !isActivelyScanning
                scanStatusTextView.setText(R.string.ble_start_scan_text_actively_scanning)
                scanForBLEDevices(SCAN_PERIOD)


                // TODO: set adapter of recycler view based on the results of BLE scan
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED){
            // The user refused to enable Bluetooth, or there was an error
            // Either way, punt the user back to the home screen and show a Toast message
            //    explaining what happened

            var toastStr = "To connect to a trash picker, Bluetooth must be enabled"
            var t = Toast.makeText(this@BLEPairingActivity, toastStr, Toast.LENGTH_LONG)
            t.show()

            var homeActivityIntent = Intent(this@BLEPairingActivity, HomeActivity::class.java)
            startActivity(homeActivityIntent)

            // finish() so the back button works as the user expects
            finish()
        }
    }

    // TODO: Use ScanFilters to filter BLE results to just those for smart trash picker
    // TODO: I'll have to deal with duplicates
    private fun scanForBLEDevices(scanDuration : Long){


        Log.d("REE", "Starting BLE scan")
        bleScanner?.startScan(bleScanCallback)

        // Stop the scan after scanDuration ms have elapsed
        var bleScanHandler = Handler()
        bleScanHandler.postDelayed(Runnable {
            this@BLEPairingActivity.stopScanForBLEDevices()

            var scanStatusTextView = ble_start_scan_textview
            scanStatusTextView.setText(R.string.ble_start_scan_text_not_scanning)

        }, scanDuration)
    }

    private fun stopScanForBLEDevices(){
        Log.d("REE", "Stopping BLE scan")
        // Note that to stop the scan, you need to use the SAME ScanCallback
        // See https://github.com/innoveit/react-native-ble-manager/issues/113
        bleScanner?.flushPendingScanResults(bleScanCallback)
        bleScanner?.stopScan(bleScanCallback)
    }
}
