package com.muyoma.thapab.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

data class AudioOutputRoute(
    val id: Int,
    val name: String,
    val type: Int,
    val isActive: Boolean
)

object AudioRouteController {

    fun availableOutputs(context: Context): List<AudioOutputRoute> {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val activeDeviceId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.communicationDevice?.id
        } else {
            null
        }

        val devices = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.availableCommunicationDevices
        } else {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
        }

        return devices
            .groupBy { canonicalKey(it) }
            .values
            .map { group ->
                group.sortedWith(
                    compareByDescending<AudioDeviceInfo> { it.id == activeDeviceId }
                        .thenBy { routePriority(it) }
                ).first()
            }
            .map { device ->
            AudioOutputRoute(
                id = device.id,
                name = displayName(device),
                type = device.type,
                isActive = activeDeviceId == device.id
            )
            }
            .sortedWith(
                compareByDescending<AudioOutputRoute> { it.isActive }
                    .thenBy { routePriority(it.type) }
                    .thenBy { it.name.lowercase() }
            )
    }

    fun routeTo(context: Context, routeId: Int): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val device = audioManager.availableCommunicationDevices.firstOrNull { it.id == routeId }
                ?: return false
            return audioManager.setCommunicationDevice(device)
        }
        return false
    }

    fun clearRoute(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.clearCommunicationDevice()
        }
    }

    private fun typeLabel(device: AudioDeviceInfo): String {
        return when (device.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth headset"
            AudioDeviceInfo.TYPE_BLE_HEADSET -> "BLE headset"
            AudioDeviceInfo.TYPE_BLE_SPEAKER -> "BLE speaker"
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "This device"
            AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> "USB audio"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Headphones"
            else -> "Audio output"
        }
    }

    private fun displayName(device: AudioDeviceInfo): String {
        return device.productName?.toString()?.takeIf {
            it.isNotBlank() && !isBuiltInRoute(device.type)
        } ?: typeLabel(device)
    }

    private fun canonicalKey(device: AudioDeviceInfo): String {
        val normalizedName = device.productName?.toString()?.trim()?.lowercase().orEmpty()
        return when {
            isBuiltInRoute(device.type) -> "builtin-device"
            device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ->
                "wired-headphones"
            isBluetoothRoute(device.type) -> "bluetooth:${normalizedName.ifBlank { "default" }}"
            else -> "${device.type}:${normalizedName.ifBlank { typeLabel(device).lowercase() }}"
        }
    }

    private fun routePriority(device: AudioDeviceInfo): Int = routePriority(device.type)

    private fun routePriority(type: Int): Int {
        return when (type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLE_SPEAKER,
            AudioDeviceInfo.TYPE_BLE_HEADSET -> 0
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE -> 1
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> 2
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> 3
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> 4
            else -> 5
        }
    }

    private fun isBuiltInRoute(type: Int): Boolean {
        return type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER || type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
    }

    private fun isBluetoothRoute(type: Int): Boolean {
        return type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
            type == AudioDeviceInfo.TYPE_BLE_SPEAKER
    }
}
