package mrechenberg.smarttrashpickerapp

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.ParcelUuid
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_blepairing.*
import mrechenberg.smarttrashpickerapp.STPBLEService.Companion.SMART_TRASH_PICKER_SERVICE_UUID_STR

/**
 * Activity for choosing a trash picker to pair over bluetooth
 *
 * Scans and filters for bluetooth devices that advertise the UUID of
 *    the "smart trash picker" service defined here https://github.com/MichaelRechenberg/Smart-Trash-Picker
 */
class BLEPairingActivity : AppCompatActivity() {

    // Flag for Bluetooth intent
    val REQUEST_ENABLE_BT = 1

    // Number of ms to use for a scanning period
    val SCAN_PERIOD : Long = 10 * 1000

    // Flag indicating if we are currently scanning for Bluetooth devices
    var isActivelyScanning = false

    // BLE scanner
    var bleScanner : BluetoothLeScanner? = null

    // The ScanCallback to use once we connect to the GATT server
    var bleScanCallback : ScanCallback? = null

    // Used to filter advertising packets from the same device and
    //    keep track of discovered devices
    val setOfDiscoveredDeviceAddresses : HashSet<String> = hashSetOf()
    val listOfDiscoveredBLEDevices : MutableList<BLEDiscoveredDevice> = mutableListOf()


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

        // Request that the user enable Bluetooth, if it isn't turned on already
        bluetoothAdapter?.takeIf { !it.isEnabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }


        // Init recycler view and its adapter
        var discoveredDevicesRecyclerView = ble_discovered_devices_recyclerview
        discoveredDevicesRecyclerView.layoutManager = LinearLayoutManager(this@BLEPairingActivity)

        var discoveredDevicesAdapter = BLEDiscoveredDeviceListAdapter(
            this@BLEPairingActivity,
            listOfDiscoveredBLEDevices
        )
        discoveredDevicesRecyclerView.adapter = discoveredDevicesAdapter


        // Initialize the BLE ScanCallback we will use on each BLE Scan
        // This callback updates the list of discovered bluetooth devices and has a
        //    closure on the recycler view's adapter (so it can notify when the
        //    dataset has changed)
        bleScanCallback = object : ScanCallback() {


            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)


                // I don't think we need any more information from the ScanResult besides the
                //    BluetoothDevice
                var foundBLEDevice = result!!.device

                var foundBLEAddress = foundBLEDevice?.address

                if (foundBLEAddress != null && !setOfDiscoveredDeviceAddresses.contains(foundBLEAddress)){
                    // We haven't seen this device before, so add it to our list of discovered
                    //   devices and use the RecyclerView adapter to notify that we've added
                    //   a new device to the list

                    setOfDiscoveredDeviceAddresses.add(foundBLEAddress)
                    var logMessage = "Found BLE device with address $foundBLEDevice.address"
                    Log.d("REE", logMessage)


                    val bleDeviceForView = BLEDiscoveredDevice(foundBLEDevice)
                    listOfDiscoveredBLEDevices.add(bleDeviceForView)

                    discoveredDevicesAdapter.notifyDataSetChanged()

                }

            }
        }


        // Initialize BLE scanner
        bleScanner = bluetoothAdapter?.bluetoothLeScanner


        // Scan filter for Bluetooth devices that advertise the
        //    Smart Trash Picker UUID
        var filterForTrashPickerServiceUIUD = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid.fromString(SMART_TRASH_PICKER_SERVICE_UUID_STR))
            .build()

        var scanFilters = listOf(
            filterForTrashPickerServiceUIUD
        )

        // Start scanning for BLE devices (since we've initialized this.bleScanCallback
        //   using the filters we've specified
        scanForBLEDevices(SCAN_PERIOD, scanFilters)





        // Allow the user to cancel a scan and start a new scan by clicking the scan text
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

                // update Recycler view by clearing dataset, then scan for devices again
                listOfDiscoveredBLEDevices.clear()
                setOfDiscoveredDeviceAddresses.clear()
                discoveredDevicesAdapter.notifyDataSetChanged()
                scanForBLEDevices(SCAN_PERIOD, scanFilters)


            }
        }
    }

    // Ensure Bluetooth is enabled.  If the user denies bluetooth access,
    //    punt them back to the home activity and show a Toast message
    //    explaining that Bluetooth permissions are required
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

    /**
     * Scan for bluetooth devices for a specified amount of time, scanning
     *  for devices that satisfy a list of scan filters
     *
     *  this.bleScanCallback is invoked for every valid BLE advertisement
     */
    private fun scanForBLEDevices(scanDuration : Long, scanFilters : List<ScanFilter>){


        Log.d("REE", "Starting BLE scan")
        // Clear any found BLE addresses and devices
        setOfDiscoveredDeviceAddresses.clear()
        listOfDiscoveredBLEDevices.clear()

        var scanSettings = ScanSettings.Builder()
            .build()

        bleScanner?.startScan(
            scanFilters,
            scanSettings,
            bleScanCallback
        )

        // Stop the scan after scanDuration ms have elapsed
        var bleScanHandler = Handler()
        bleScanHandler.postDelayed(Runnable {
            this@BLEPairingActivity.stopScanForBLEDevices()

            var scanStatusTextView = ble_start_scan_textview
            scanStatusTextView.setText(R.string.ble_start_scan_text_not_scanning)

        }, scanDuration)
    }

    // Stop scanning for BLE devices
    private fun stopScanForBLEDevices(){
        Log.d("REE", "Stopping BLE scan")
        // Note that to stop the scan, you need to use the SAME ScanCallback for stopScan()
        // See https://github.com/innoveit/react-native-ble-manager/issues/113
        bleScanner?.flushPendingScanResults(bleScanCallback)
        bleScanner?.stopScan(bleScanCallback)
    }
}
