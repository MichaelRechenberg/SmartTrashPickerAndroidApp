package mrechenberg.smarttrashpickerapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_export_trash_data.*
import mrechenberg.smarttrashpickerapp.database.TrashDatabase
import mrechenberg.smarttrashpickerapp.database.TrashPickup
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat

class ExportTrashDataActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export_trash_data)

        var username = intent.getStringExtra(HomeActivity.USERNAME_INTENT_KEY)

        var exportButton = start_export_button
        var serverAddressEditText = server_url_edit_text


        // Disable export button until a host address is given
        serverAddressEditText.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun afterTextChanged(s: Editable?) {

                if (s != null){
                    if (s!!.isNotEmpty()){
                        exportButton.isEnabled = true
                    }
                    else {
                        exportButton.isEnabled = false
                    }
                }

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })


        exportButton.setOnClickListener { _ ->


            try {

                var t = Toast.makeText(this@ExportTrashDataActivity, "Exporting your data now.  This may take a while", Toast.LENGTH_SHORT)
                t.show()


                var trashDatabase =
                    TrashDatabase.getDatabase(this@ExportTrashDataActivity)


                var pickupsOfUser = trashDatabase.getTrashPickupDAO().getAllPickupsOfUser(username)

                var pickupsOfUserAsJsonObj = pickupsOfUser.map { tp -> serializeTrashPickupToJson(tp) }
                var jsonArrayPayload = JSONArray(pickupsOfUserAsJsonObj)



                val volleyQueue = Volley.newRequestQueue(this@ExportTrashDataActivity)


                var serverAddress = serverAddressEditText.text.toString()
                var serverURL = "http://$serverAddress/export"

                Log.d("REE", "Sending volley to $serverURL")

                val jsonRequest = JsonArrayRequest(
                    Request.Method.POST,
                    serverURL,
                    jsonArrayPayload,
                    Response.Listener<JSONArray> { response ->
                        Log.d("REE", "Server response -> ${response}")

                        val successToast = Toast.makeText(this@ExportTrashDataActivity, "Successfully exported data", Toast.LENGTH_SHORT)
                        successToast.show()
                    },
                    Response.ErrorListener { error ->
                        Log.d("REE", "Error from server ${error}")
                        val failToast = Toast.makeText(this@ExportTrashDataActivity, "Failed to export data", Toast.LENGTH_SHORT)
                        failToast.show()
                    }
                )

                volleyQueue.add(jsonRequest)

            } catch (exception : Exception){
                Log.e("REE", "Failed to send Volley request due to Exception")
                Log.e("REE", exception.toString())
            }







        }
    }

    // Serialize a TrashPickup object to JSON for demo export
    fun serializeTrashPickupToJson(trashPickup : TrashPickup) : JSONObject {
        val jsonObj = JSONObject()

        jsonObj.put("username", trashPickup.username)
        jsonObj.put("session-id", trashPickup.sessionId)
        jsonObj.put("latitude", trashPickup.latitude)
        jsonObj.put("longitude", trashPickup.longitude)

        var dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        var dateStr = dateFormatter.format(trashPickup.collectedDate)
        jsonObj.put("pickup-date", dateStr)

        return jsonObj
    }
}
