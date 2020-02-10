package io.keinix.peertopeerblesample

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.keinix.peertopeerblesample.ble.BleData
import io.keinix.peertopeerblesample.ble.BleManager
import io.keinix.peertopeerblesample.ble.isBleOn
import io.keinix.peertopeerblesample.util.RequestCodes
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var bleManager: BleManager
    private val userId = (0 until 10000).random()
    private var totalBleMessageCount = 0
    private var bleStarted = false

    private val bleDataExchangeManager = object : BleManager.BleDataExchangeManager {
        override fun onDataReceived(data: BleData) {
            totalBleMessageCount++
            bleDataTotalCountTextView.text = totalBleMessageCount.toString()
            bleDataTextView.text = data.message
            Toast.makeText(this@MainActivity, "Message Received", Toast.LENGTH_SHORT).show()
        }

        override fun getBleData(): BleData {
            return BleData(userId, messageEditText.text.toString())
        }
    }

    private val hasLocationPermission
        get() = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bleManager = BleManager(this, bleDataExchangeManager)
        bindView()
    }

    override fun onDestroy() {
        super.onDestroy()
        bleManager.stop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == RequestCodes.ACCESS_COARSE_LOCATION) {
            if (grantResults.isNotEmpty() &&
                grantResults.first() == PackageManager.PERMISSION_GRANTED) {
                startBle()
            } else {
                Toast.makeText(this, "Location Permission required to scan", Toast.LENGTH_SHORT).show()
                startButton.text = "Start Ble"
            }
        }
    }

    private fun bindView() {
        startButton.setOnClickListener {
            if (!BluetoothAdapter.getDefaultAdapter().isBleOn) {
                Toast.makeText(this, "Please turn BLE on", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (bleStarted) {
                bleManager.stop()
                Toast.makeText(this, "Ble Stopped", Toast.LENGTH_SHORT).show()
                startButton.text = "Start Ble"
            } else {
                startBle()
                startButton.text = "Stop Ble"
            }
            bleStarted = !bleStarted
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBle() {
        if (hasLocationPermission) {
            bleManager.start()
            Toast.makeText(this, "Ble Started", Toast.LENGTH_SHORT).show()
        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
            RequestCodes.ACCESS_COARSE_LOCATION
        )
    }
}
