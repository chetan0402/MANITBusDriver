package me.chetan.manitbusdriver

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class Login : AppCompatActivity() {
    private var phone_number: Long? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        token = getSharedPreferences("token", MODE_PRIVATE).getString("token","").toString()
        if(token!=""){
            startActivity(Intent(this,TrackActivity::class.java))
            finish()
        }

        val phoneNumber = findViewById<TextInputLayout>(R.id.loginPhoneInput)
        val getOTPButton = findViewById<Button>(R.id.getOTP)
        getOTPButton.setOnClickListener {
            phone_number = phoneNumber.editText?.text.toString().toLong()
            sendOTPManage()
        }
        val loginButton = findViewById<Button>(R.id.login)
        loginButton.setOnClickListener {
            login()
        }
    }

    private fun sendOTPManage(){
        this.lifecycleScope.launch(Dispatchers.IO){
            try{
                (URL("${MainActivity.BASE}/otp").openConnection() as HttpURLConnection).run {
                    connectTimeout = 2000
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Accept","application/json")
                    setRequestProperty("Content-Type", "application/json")

                    val body = JSONObject().run {
                        put("phone",phone_number)
                    }

                    outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

                    val responseCode = responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        // Handle successful response
                        val response = inputStream.bufferedReader().use { it.readText() }
                        // Process the response as needed
                    } else {
                        runOnUiThread {
                            MaterialAlertDialogBuilder(this@Login).run {
                                setTitle("OTP rejected")
                                show()
                            }
                        }
                    }
                }
            }catch (e:Exception){
                e.printStackTrace()
                runOnUiThread {
                    MaterialAlertDialogBuilder(this@Login).run {
                        setTitle("No internet")
                        show()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if(token!=""){
            startActivity(Intent(this,TrackActivity::class.java))
            finish()
        }
    }

    private fun login(){
        this.lifecycleScope.launch(Dispatchers.IO) {
            try{
                (URL("${MainActivity.BASE}/verify").openConnection() as HttpURLConnection).run {
                    connectTimeout = 2000
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Accept","application/json")
                    setRequestProperty("Content-Type", "application/json")

                    val body = JSONObject().run {
                        put("phone",phone_number)
                        val otpLayout = findViewById<TextInputLayout>(R.id.OTP)
                        put("otp",otpLayout.editText?.text.toString().toInt())
                    }

                    outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

                    val responseCode = responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val response = inputStream.bufferedReader().use { it.readText() }
                        token = JSONObject(response).getString("token")
                        getSharedPreferences("token", MODE_PRIVATE).edit().putString("token",token).apply()
                        startActivity(Intent(this@Login,TrackActivity::class.java))
                        finish()
                    } else {
                        runOnUiThread {
                            MaterialAlertDialogBuilder(this@Login).run {
                                setTitle("Login rejected")
                                show()
                            }
                        }
                    }
                }
            }catch (e:Exception){
                e.printStackTrace()
                runOnUiThread {
                    MaterialAlertDialogBuilder(this@Login).run {
                        setTitle("No internet")
                        show()
                    }
                }
            }
        }
    }

    companion object{
        var token = ""
    }
}