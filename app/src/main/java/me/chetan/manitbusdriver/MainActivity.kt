package me.chetan.manitbusdriver

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        this.lifecycleScope.launch(Dispatchers.IO){
            try{
                (URL("$BASE/version/driver").openConnection() as HttpURLConnection).run {
                    connectTimeout = 2000

                    if(applicationContext.packageManager.getPackageInfo(applicationContext.packageName,0).versionCode<JSONObject(inputStream.bufferedReader().use { it.readText() }).getInt("version")){
                        runOnUiThread {
                            MaterialAlertDialogBuilder(this@MainActivity).run {
                                setTitle("Please update")
                                setMessage("New version available")
                                setPositiveButton("Update") { _ , _ ->
                                    startActivity(Intent(ACTION_VIEW, Uri.parse("$BASE/driver/download")))
                                    finish()
                                }
                                setCancelable(false)
                                show()
                            }
                        }
                    }else{
                        runOnUiThread{
                            startActivity(Intent(this@MainActivity,Login::class.java))
                            finish()
                        }
                    }
                }
            } catch (e:Exception){
                runOnUiThread {
                    MaterialAlertDialogBuilder(this@MainActivity).run {
                        setTitle("No internet")
                        setMessage("Please turn on your internet")
                        setCancelable(false)
                        setPositiveButton("Retry") { _ , _ ->
                            startActivity(Intent(this@MainActivity,MainActivity::class.java))
                            finish()
                        }
                        show()
                    }
                }
            }
        }
    }

    companion object{
        val BASE = "http://ebus.manit.ac.in"
    }
}