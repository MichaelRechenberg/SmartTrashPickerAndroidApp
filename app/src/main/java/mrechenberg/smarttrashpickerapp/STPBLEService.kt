package mrechenberg.smarttrashpickerapp

import android.app.*
import java.util.UUID
import android.bluetooth.*
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.util.Log

/**
 * Foreground service that creates and maintains a BLE connection
 *  with a device and acts upon indications sent to the app
 *
 * Consumers of this service need to pass in the BluetoothDevice
 *  they want to connect to via a Parcelable Extra within the Intent
 *  (under the key STPBLEService.STP_BLE_DEVICE_INTENT_KEY)
 */
class STPBLEService : Service() {

    // Have a handle to the BluetoothGatt so we can free resources
    //    when this Service is destroyed
    var bluetoothGatt : BluetoothGatt? = null

    // ID for foreground notification
    val ONGOING_NOTIFICATION_ID = 1

    // Identifiers for notification channels (for Oreo and upwards)
    val NOTIFICATION_CHANNEL_STR_ID = "stp-notification-channel"
    val NOTIFICATION_CHANNEL_NAME = "STP Notifications"

    val mBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService() : STPBLEService = this@STPBLEService
    }

    companion object {
        // Intent key to use to store BluetoothDevice within Intent Extras
        val STP_BLE_DEVICE_INTENT_KEY = "STP-BLE-Device"
        // Bluetooth Service UUID for smart trash picker service
        val SMART_TRASH_PICKER_SERVICE_UUID_STR = "00001337-0000-1000-8000-00805f9b34fb"

        // Bluetooth Characterstic UUID for smart trash picker TrashGrabbedChrc
        val SMART_TRASH_PICKER_TRASH_GRABBED_CHRC_UUID_STR = "00001574-0000-1000-8000-00805f9b34fb"

        val BLE_CCCD_UUID_STR = "00002902-0000-1000-8000-00805F9B34FB"
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        var bleDevice = intent?.getParcelableExtra<BluetoothDevice>(STP_BLE_DEVICE_INTENT_KEY)


        var gattCallback = object : BluetoothGattCallback() {

            // Log connectivity status, request to discover services upon GATT connection success
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                when (newState){
                    BluetoothProfile.STATE_CONNECTED -> {
                        // Connected to GATT server
                        Log.d("REE", "Connected to GATT Server :)")
                        Log.d("REE", "Asking to discover BLE services now")

                        var discoverServiceSuccess = gatt?.discoverServices()

                        Log.d("REE", "Discover Service success $discoverServiceSuccess")


                        // Now, any indications sent to the Android app should trigger the
                        //    onCharactersticChanged() callback

                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.d("REE", "Disconnected from GATT Server")
                        Log.d("REE", "Status $status")
                        gatt?.close()
                        bluetoothGatt = null
                    }
                }


            }

            //After we've discovered services, write to CCCD of "Trash grabbed" Characteristic
            //   so we get indications from the Pi for that characteristic
            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                Log.d("REE", "onServicesDiscovered() status $status")

                var stpBleService = gatt?.getService(UUID.fromString(SMART_TRASH_PICKER_SERVICE_UUID_STR))
                var stpTrashPickedChrc = stpBleService?.getCharacteristic(UUID.fromString(SMART_TRASH_PICKER_TRASH_GRABBED_CHRC_UUID_STR))

                // https://developer.android.com/guide/topics/connectivity/bluetooth-le#notification
                // Enable indications by setCharacteristicNotification, then updating the CCCD descriptor
                gatt?.setCharacteristicNotification(stpTrashPickedChrc, true)
                var cccdUUID = UUID.fromString(BLE_CCCD_UUID_STR)
                val cccdDescriptor = stpTrashPickedChrc!!.getDescriptor(cccdUUID).apply {
                    value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                }
                gatt?.writeDescriptor(cccdDescriptor)
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
                // TODO: when this is called, get current location and store it in SQLite DB (Room API?)
                var x = characteristic?.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                Log.d("REE", "onCharacteristicChanged was called, received value $x")
            }
        }

        // Initiate the GATT connection
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            bluetoothGatt = bleDevice?.connectGatt(
                this.applicationContext,
                false,
                gattCallback,
                TRANSPORT_LE
            )
        }
        else {
            bluetoothGatt = bleDevice?.connectGatt(
                this.applicationContext,
                false,
                gattCallback
            )
        }


        // Create notification so this service can run as a Foreground Service

        // Make notification channel if this is a newer phone
        // https://stackoverflow.com/questions/47531742/startforeground-fail-after-upgrade-to-android-8-1/47634345
        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                createNotificationChannel()
                NOTIFICATION_CHANNEL_STR_ID
            }
            else {
                ""
            }

        val pendingIntent : PendingIntent =
            Intent(this@STPBLEService, BLEConnActiveActivity::class.java).let {
                    notificationIntent -> PendingIntent.getActivity(this@STPBLEService, 0, notificationIntent, 0)
            }

        val notification : Notification = NotificationCompat.Builder(this@STPBLEService, channelId)
            .setContentTitle(getText(R.string.stp_ble_notification_content_title))
            .setContentText("${getText(R.string.stp_ble_notification_content_text_prefix)} ${bleDevice?.address})")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(ONGOING_NOTIFICATION_ID, notification)


        return Service.START_STICKY
    }



    // create a notification channel for devices supporting Oreo and higher with channel id NOTIFICATION_CHANNEL_STR_ID
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_STR_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)

        channel.lightColor = Color.CYAN
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        Log.d("REE", "onDestroy() called")
        bluetoothGatt?.close()
        bluetoothGatt?.disconnect()
        bluetoothGatt = null
    }
}

