package org.jellyfin.mobile.utils

import timber.log.Timber
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress


class WakeOnLan {

    fun sendAsync(mac: String, broadcastIp: String) {
        Thread {
            send(mac, broadcastIp)
        }.start()
    }

    private fun send(mac: String, broadcastIp: String) {
        val buf = buildMagicPacket(mac)
        val packet = DatagramPacket(buf, buf.size, InetAddress.getByName(broadcastIp), 7)

        try {
            DatagramSocket().send(packet)
        } catch (e: Exception) {
            Timber.e("Could not send WoL-Packet: " + e.message)
        }
    }


    private fun buildMagicPacket(mac: String): ByteArray {

        val packet = arrayListOf<Byte>()

        // add 0xFF header
        for (i in 0..5) {
            packet.add(0xFF.toByte())
        }

        // add mac
        val macBytes = arrayListOf<Byte>()
        mac.split(":").forEach {
            macBytes.add(it.toInt(16).toByte())
        }
        packet.addAll(macBytes)

        // pad message
        while (packet.size < 102) {
            packet.addAll(macBytes)
        }

        return packet.toByteArray()
    }
}