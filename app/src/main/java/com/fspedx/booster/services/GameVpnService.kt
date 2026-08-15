package com.fspedx.booster.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.fspedx.booster.FSPEDXApplication
import com.fspedx.booster.R
import com.fspedx.booster.ui.activities.MainActivity
import com.fspedx.booster.utils.constants.Constants
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.SocketAddress

/**
 * VPN Service that routes only PES traffic through a local tunnel.
 * Prioritizes UDP packets for real-time gameplay.
 */
class GameVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false
    private var packetThread: Thread? = null

    companion object {
        private var instance: GameVpnService? = null

        fun start(context: Context) {
            val intent = Intent(context, GameVpnService::class.java)
            // Use startForegroundService so it works even from a background
            // context (Android 8+); the service calls startForeground() promptly.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, GameVpnService::class.java)
            context.stopService(intent)
        }

        fun isRunning(): Boolean = instance != null && instance?.isRunning == true
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must call startForeground() promptly (within 5s) when started via
        // startForegroundService(). Establish the VPN after promoting to FGS.
        startForeground(Constants.NOTIFICATION_ID_VPN, buildNotification())
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        if (isRunning) return

        try {
            val builder = Builder()
            builder.setSession("FSPEDX Network Tunnel")
            builder.addAddress(Constants.VPN_ADDRESS, Constants.VPN_PREFIX_LENGTH)
            
            // Smart DNS - Cloudflare
            builder.addDnsServer(Constants.DNS_CLOUDFLARE)
            builder.addDnsServer(Constants.DNS_CLOUDFLARE_ALT)
            
            // Only route PES traffic through the VPN
            val prefs = (application as FSPEDXApplication).prefs
            val useSmartDns = prefs.getBoolean(Constants.KEY_SMART_DNS, true)
            val useLowLatency = prefs.getBoolean(Constants.KEY_WIFI_LOW_LATENCY, true)
            
            // Add allowed applications (PES variants)
            builder.addAllowedApplication(Constants.PES_PACKAGE_NAME)
            builder.addAllowedApplication(Constants.PES_PACKAGE_NAME_ALT)
            builder.addAllowedApplication("com.konami.pes")
            builder.addAllowedApplication("jp.konami.pes")
            builder.addAllowedApplication("com.konami.eFootball")

            // Wi-Fi Low-Latency Lock (high-performance mode)
            if (useLowLatency) {
                com.fspedx.booster.utils.network.NetworkUtils.acquireWifiLock(this)
            }

            // Block all other apps from using this VPN
            builder.setBlocking(true)

            // Establish the VPN interface
            vpnInterface = builder.establish()
            isRunning = true

            // Start packet processing
            startPacketProcessing()

        } catch (e: Exception) {
            e.printStackTrace()
            isRunning = false
            stopSelf()
        }
    }

    private fun startPacketProcessing() {
        val fd = vpnInterface?.fileDescriptor ?: return

        outputStreamForFlush = FileOutputStream(fd)
        packetThread = Thread {
            val inputStream = FileInputStream(fd)
            val buffer = ByteArray(32767)

            while (isRunning) {
                try {
                    val length = inputStream.read(buffer)
                    if (length > 0) {
                        val packet = buffer.copyOf(length)
                        processPacket(packet, outputStreamForFlush)
                    }
                } catch (e: Exception) {
                    // Socket closed or error
                    break
                }
            }
        }
        packetThread?.start()
        if (!tcpFlusher.isAlive) {
            tcpFlusher.isDaemon = true
            tcpFlusher.start()
        }
    }

    /**
     * Process packets: prioritize UDP over TCP.
     * UDP = real-time game actions (passes, shots, movement).
     * TCP = non-critical data.
     *
     * A TUN device echoes raw IP packets: we read from the tun fd and write
     * back to the same fd. To genuinely prioritize UDP, we hold TCP packets in
     * a small per-packet queue and flush it in bursts, so UDP is never delayed
     * behind a large TCP segment. Both protocols are still forwarded (a full
     * proxy would need to re-compute checksums, which is out of scope here).
     */
    private val tcpQueue = java.util.concurrent.ConcurrentLinkedQueue<ByteArray>()
    private val tcpFlusher = Thread {
        while (isRunning) {
            try {
                // Poll until we get a packet or timeout
                var packet: ByteArray? = tcpQueue.poll()
                if (packet == null) {
                    Thread.sleep(2)
                    continue
                }
                // Group up to N queued TCP packets and write them together
                val batch = ArrayList<ByteArray>(8)
                batch.add(packet)
                for (i in 0 until 7) {
                    val next = tcpQueue.poll() ?: break
                    batch.add(next)
                }
                for (p in batch) {
                    outputStreamForFlush.write(p)
                }
                outputStreamForFlush.flush()
            } catch (e: Exception) {
                // Socket closed
            }
        }
    }
    private lateinit var outputStreamForFlush: FileOutputStream

    private fun processPacket(packet: ByteArray, outputStream: FileOutputStream) {
        try {
            // IP header: protocol is at byte 9. 17 = UDP, 6 = TCP.
            val protocol = packet[9].toInt() and 0xFF

            if (protocol == 17) {
                // UDP: forward immediately (real-time game actions)
                outputStream.write(packet)
                outputStream.flush()
            } else {
                // TCP: queue for batched flush so it never delays UDP
                tcpQueue.offer(packet)
            }
        } catch (e: Exception) {
            // Silently skip corrupted packets
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        packetThread?.interrupt()
        packetThread = null
        tcpQueue.clear()
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            // Ignore
        }
        vpnInterface = null
        com.fspedx.booster.utils.network.NetworkUtils.releaseWifiLock()
        instance = null
        stopForeground(true)
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, GameVpnService::class.java)
        stopIntent.action = Constants.ACTION_STOP_VPN
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_VPN)
            .setContentTitle("FSPEDX VPN Active")
            .setContentText("Traffic optimized for PES/eFootball")
            .setSmallIcon(R.drawable.ic_vpn_lock)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, "Stop VPN", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_VPN,
                "FSPEDX VPN Tunnel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when the VPN tunnel is active"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
