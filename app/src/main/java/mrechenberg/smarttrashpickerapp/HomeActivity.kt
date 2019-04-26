package mrechenberg.smarttrashpickerapp

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : AppCompatActivity() {


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


    }
}
