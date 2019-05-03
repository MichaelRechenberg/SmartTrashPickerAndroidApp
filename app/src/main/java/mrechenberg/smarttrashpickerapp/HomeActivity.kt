package mrechenberg.smarttrashpickerapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import kotlinx.android.synthetic.main.activity_home.*

/**
 * Launcher activity for the app
 */
class HomeActivity : AppCompatActivity() {


    // Used when requesting location permissions
    private val MY_REQUEST_LOCATION_PERMISSIONS = 1

    // Used when requesting location settings
    private val MY_REQUEST_LOCATION_SETTINGS = 2


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        var loginButton = welcome_screen_login_button
        var usernameEditText = welcome_screen_username_edittext

        // Enable the login button only if the user has entered some username
        usernameEditText.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun afterTextChanged(s: Editable?) {

                if (s != null){
                   if (s!!.isNotEmpty()){
                      loginButton.isEnabled = true
                   }
                   else {
                      loginButton.isEnabled = false
                   }
                }

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })


        // When someone clicks the "login" button, start the BLE pairing activity
        loginButton.setOnClickListener { v ->

            var blePairingActivityIntent = Intent(this@HomeActivity, BLEPairingActivity::class.java)
            var typedUsername = usernameEditText.text.toString()
            blePairingActivityIntent.putExtra("username", typedUsername)

            startActivity(blePairingActivityIntent)
        }


        // After we request location permissions, we will prompt the user to change location settings
        //    (in onRequestPermissionsResult()) so we have high accuracy when getting the user's location
        // See https://developer.android.com/training/location
        ActivityCompat.requestPermissions(
            this@HomeActivity,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            MY_REQUEST_LOCATION_PERMISSIONS
        )


    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode){
            MY_REQUEST_LOCATION_PERMISSIONS -> {
                val allLocationPermissionsGranted = grantResults.all { gr -> gr == PackageManager.PERMISSION_GRANTED}

                if (allLocationPermissionsGranted){
                    // Location permissions are granted, request high precision location settings
                    Log.d("REE", "All location permissions are granted, now requesting high precision for location")
                    requestLocationSettings()

                }
                else{
                    Log.e("REE", "Not all location permissions were granted")
                    val toastMessage = "This app requires location permissions to operate.  Please restart " +
                            "the app and enable location access if you want to use this app"
                    var t = Toast.makeText(this@HomeActivity, toastMessage, Toast.LENGTH_LONG)
                    t.show()
                }
            }
            else -> {
                // silently ignore other permission requests
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode) {
            MY_REQUEST_LOCATION_SETTINGS -> {
                if (resultCode == Activity.RESULT_OK){
                    Log.d("REE", "All location settings granted from dialog")
                }
                else {
                    Log.e("REE", "Failed to request location settings.  resultCode = $resultCode")
                }
            }
        }
    }

    private fun requestLocationSettings() {
        val locationRequest = LocationRequest.create()!!.apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        var builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        var settingsClient = LocationServices.getSettingsClient(this@HomeActivity)
        val checkSettingsTask = settingsClient.checkLocationSettings(builder.build())

        checkSettingsTask.addOnSuccessListener {
            Log.d("REE", "All location settings are satisfied :)")
        }

        checkSettingsTask.addOnFailureListener { exception ->
            Log.d("REE", "Not all location settings are satisfied, creating dialog to ask user for settings")
            if(exception is ResolvableApiException) {
                // Prompt user with a dialog for location

                try {
                    exception.startResolutionForResult(
                        this@HomeActivity,
                        MY_REQUEST_LOCATION_SETTINGS
                    )
                } catch (sendEx : IntentSender.SendIntentException){
                    // Ignore error
                }
            }
        }
    }
}
