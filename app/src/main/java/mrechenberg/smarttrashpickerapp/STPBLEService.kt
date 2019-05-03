package mrechenberg.smarttrashpickerapp

import android.app.*
import android.bluetooth.*
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.AsyncTask
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import mrechenberg.smarttrashpickerapp.database.TrashDatabase
import mrechenberg.smarttrashpickerapp.database.TrashPickingSession
import mrechenberg.smarttrashpickerapp.database.TrashPickup
import java.util.*

/**
 * Foreground service that creates and maintains a BLE connection
 *  with a device and acts upon indications sent to the app
 *
 * Consumers of this service need to pass in the BluetoothDevice
 *  they want to connect to via a Parcelable Extra within the Intent
 *  (under the key STPBLEService.STP_BLE_DEVICE_INTENT_KEY)
 */
class STPBLEService : Service() {

    // The username of the user, taken from Intent
    lateinit var username : String

    // Session ID of this TrashPickupSession (once we INSERTed a new TrashPickupSession record)
    var trashPickupSessionId : Long? = null

    // Have a handle to the BluetoothGatt so we can free resources
    //    when this Service is destroyed
    var bluetoothGatt : BluetoothGatt? = null

    // Trash database to record TrashPickups and TrashPickingSessions
    lateinit var trashDatabase : TrashDatabase

    // Location client
    lateinit var fusedLocationClient : FusedLocationProviderClient

    // ID for foreground notification
    val ONGOING_NOTIFICATION_ID = 1

    // Keep track of the amount of trash picked up while this
    //   Service is alive
    var numTrashPickedUp = 0

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

        // Get a handle on the database
        trashDatabase = TrashDatabase.getDatabase(this@STPBLEService.application)

        // Get username from intent
        username = intent!!.getStringExtra(HomeActivity.USERNAME_INTENT_KEY)

        // Get location provider
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this@STPBLEService)


        // Insert new TrashPickupSession record and then init the trashPickupSessionId of this Service,
        //   blocking until it has finished
        var insertAsyncTaskParams = InsertNewTrashPickingSessionAsyncTaskParams(
            username = username,
            trashDatabase = trashDatabase
        )
        var insertTask = InsertNewTrashPickingSessionAsyncTask().execute(insertAsyncTaskParams)
        var newSessionId = insertTask.get()
        trashPickupSessionId = newSessionId




        // Setup Bluetooth code
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
                        // If the Bluetooth connection fails, then we should stop this service
                        //   so the user can reconnect later
                        Log.d("REE", "Disconnected from GATT Server")
                        Log.d("REE", "Status $status")

                        stopSelf()
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

            /**
             *  When the Pi sends the app an indication, that means that the user picked up trash
             *
             *  When that happens, we request the user's current location and update the notification
             *      with the number of trash picked up
             */
            override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
                numTrashPickedUp++
                var x = characteristic?.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                Log.d("REE", "onCharacteristicChanged was called, received value $x")

                // Update foreground service notification with incremented trash count
                var notificationManager = NotificationManagerCompat.from(this@STPBLEService)

                var notificationBuilder = createNotificationBuilder()
                notificationBuilder.setContentText("${getString(R.string.stp_ble_notification_content_text_prefix)} $numTrashPickedUp")
                var updatedNotification = notificationBuilder.build()


                notificationManager.notify(
                    ONGOING_NOTIFICATION_ID,
                    updatedNotification
                )


                try {
                    Log.d("REE", "Requesting user's location")
                    var lastLocationTask = fusedLocationClient.lastLocation
                    lastLocationTask.addOnSuccessListener {location ->
                        val latitude = location.latitude
                        val longitude = location.longitude
                        Log.d("REE", "Determined current location of $username as ($latitude, $longitude)")

                        // Kick off an AsyncTask to insert a new TrashPickup record (fire and forget)
                        var trashPickup = TrashPickup(
                            username = username,
                            latitude = latitude,
                            longitude = longitude,
                            sessionId = trashPickupSessionId as Long,
                            collectedDate = Calendar.getInstance().time
                        )

                        var params = InsertNewTrashPickupAsyncTaskParams(
                            trashDatabase = trashDatabase,
                            trashPickup = trashPickup
                        )

                        InsertNewTrashPickupAsyncTask().execute(params)
                    }
                } catch (securityException : SecurityException) {
                    Log.e("REE", "User has refused permissions for determining location")
                }

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

        var notificationBuilder = createNotificationBuilder()
        var notification = notificationBuilder.build()


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

    private fun createNotificationBuilder() : NotificationCompat.Builder {
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

        val notificationBuilder = NotificationCompat.Builder(this@STPBLEService, channelId)
            .setContentTitle(getText(R.string.stp_ble_notification_content_title))
            .setContentText("${getText(R.string.stp_ble_notification_content_text_prefix)} 0")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)

        return notificationBuilder
    }

    override fun onDestroy() {
        Log.d("REE", "onDestroy() called")
        bluetoothGatt?.close()
        bluetoothGatt?.disconnect()
        bluetoothGatt = null
    }





    data class InsertNewTrashPickingSessionAsyncTaskParams(
        val trashDatabase : TrashDatabase,
        val username : String
    )

    // AsyncTask to insert a new TrashPickingSession record into the database
    // After the record is inserted, the session ID is stored in a field of STPBLEService
    class InsertNewTrashPickingSessionAsyncTask : AsyncTask<InsertNewTrashPickingSessionAsyncTaskParams, Void, Long?>() {
        override fun doInBackground(vararg params: InsertNewTrashPickingSessionAsyncTaskParams?): Long? {
            var param = params[0]

            var trashPickingSessionDAO = param?.trashDatabase?.getTrashPickingSessionDAO()

            var trashPickupSession = TrashPickingSession(
                username = param!!.username,
                startTime = Calendar.getInstance().time,
                endTime = null
            )

            var sessionId = trashPickingSessionDAO?.insert(trashPickupSession)

            return sessionId
        }
    }

    data class InsertNewTrashPickupAsyncTaskParams(
        val trashDatabase: TrashDatabase,
        val trashPickup : TrashPickup
    )

    // AsyncTask to insert a new TrashPickup record into the database
    // Returns the id of the newly added TrashPickup
    class InsertNewTrashPickupAsyncTask : AsyncTask<InsertNewTrashPickupAsyncTaskParams, Void, Long?>() {
        override fun doInBackground(vararg params: InsertNewTrashPickupAsyncTaskParams?): Long? {
            var param = params[0]

            var trashPickupDAO = param?.trashDatabase?.getTrashPickupDAO()

            var trashPickup = param!!.trashPickup

            var trashPickupId =  trashPickupDAO?.insert(trashPickup)

            // var allTrashCount = trashPickupDAO?.getAllPickupsOfSession(trashPickup.sessionId)?.count()
            // Log.d("REE", "We have picked up $allTrashCount trashes for session ${trashPickup.sessionId}")

            return trashPickupId
        }
    }
}

