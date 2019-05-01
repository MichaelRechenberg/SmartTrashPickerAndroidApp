package mrechenberg.smarttrashpickerapp

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast

class BLEDiscoveredDeviceListAdapter(val mContext: Context, val mDiscoveredDevices: MutableList<BLEDiscoveredDevice>) :
    RecyclerView.Adapter<BLEDiscoveredDeviceListAdapter.BLEDiscoveredDeviceViewHolder>() {

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder.
    class BLEDiscoveredDeviceViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
        val uiudTextView = v.findViewById<TextView>(R.id.ble_service_name_textview)
        val bleAddressTextView = v.findViewById<TextView>(R.id.ble_address_textview)
    }


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): BLEDiscoveredDeviceViewHolder {
        var itemView = LayoutInflater.from(parent.context).inflate(R.layout.ble_device_list_item, parent, false)
        return BLEDiscoveredDeviceViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return this.mDiscoveredDevices.size
    }

    override fun onBindViewHolder(holder: BLEDiscoveredDeviceViewHolder, position: Int) {
        // Select the right device from our internal array
        var discoveredDevice = this.mDiscoveredDevices[position]

        var bleDevice = discoveredDevice.bleDevice

        // Populate the Views that comprise one list element with the
        //   data for this discovered device
        var bleAddress = bleDevice.address

        holder.uiudTextView.text = "Smart Trash Picker Device"
        holder.bleAddressTextView.text = bleAddress

        // Create an on-click listener that will pair with this Bluetooth device when
        //   the holder's view is clicked
        holder.v.setOnClickListener { v ->
            // TODO: actually do bluetooth pairing with a background service on click, using mContext
            var underlyingBLEDevice = mDiscoveredDevices[position].bleDevice
            var addressToPairWith = underlyingBLEDevice.address

            var t = Toast.makeText(v.context, "TODO: connect to $addressToPairWith", Toast.LENGTH_SHORT)
            t.show()


        }
    }


}