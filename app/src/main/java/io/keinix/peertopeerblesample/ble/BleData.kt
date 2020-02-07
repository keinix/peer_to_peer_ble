package io.keinix.peertopeerblesample.ble

import com.google.gson.annotations.SerializedName

/**
 * Wrapper for data sent and received via BLE
 */
class BleData(@SerializedName("userId") val userId: Int,
              @SerializedName("message") val message: String)