package io.keinix.peertopeerblesample

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.keinix.peertopeerblesample.ble.isBleOn
import io.keinix.peertopeerblesample.util.RequestCode
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var bleUpdateLiveData: LiveData<MainViewModel.BleUpdateResult>? = null
    private lateinit var viewModel: MainViewModel

    private val bleUpdateObserver = Observer<MainViewModel.BleUpdateResult> {
            bleDataTotalCountTextView.text = it.totalMessageCount.toString()
            bleDataTextView.text = it.newMessage
            Toast.makeText(this@MainActivity, "Message Received", Toast.LENGTH_SHORT).show()
    }

    private val hasLocationPermission
        get() = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        bindView()
        if (viewModel.isBleStarted && !viewModel.bleUpdateResultLiveData.hasObservers()) {
            bleUpdateLiveData = viewModel.bleUpdateResultLiveData
            bleUpdateLiveData?.observe(this, bleUpdateObserver)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        if (requestCode == RequestCode.ACCESS_COARSE_LOCATION) {
            if (grantResults.isNotEmpty() &&
                grantResults.first() == PackageManager.PERMISSION_GRANTED) {
                startBle()
            } else {
                Toast.makeText(this, "Location Permission required to scan", Toast.LENGTH_SHORT).show()
                startButton.text = getString(R.string.start_ble)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun bindView() {
        val buttonTextRes = if (viewModel.isBleStarted) R.string.stop_ble else R.string.start_ble
        startButton.text = getString(buttonTextRes)
        messageEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null && s.isNotBlank()) viewModel.lastBleMessage = s.toString()
            }

        })

        startButton.setOnClickListener {
            if (!BluetoothAdapter.getDefaultAdapter().isBleOn) {
                Toast.makeText(this, "Please turn BLE on", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (viewModel.isBleStarted) {
                stopBle()
                Toast.makeText(this, "Ble Stopped", Toast.LENGTH_SHORT).show()
                startButton.text = getString(R.string.start_ble)
            } else {
                startBle()
                startButton.text = getString(R.string.stop_ble)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBle() {
        if (hasLocationPermission) {
            bleUpdateLiveData = viewModel.startBle()
            bleUpdateLiveData?.observe(this, bleUpdateObserver)
            Toast.makeText(this, "Ble Started", Toast.LENGTH_SHORT).show()
        } else {
            requestLocationPermission()
        }
    }

    private fun stopBle() {
        bleUpdateLiveData?.removeObserver(bleUpdateObserver)
        viewModel.stopBle()
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
            RequestCode.ACCESS_COARSE_LOCATION
        )
    }
}
