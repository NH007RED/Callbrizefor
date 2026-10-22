package com.nh.callbrize.net

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

const val WIFI_PORT = 52525
val CALLBRIZE_UUID: UUID = UUID.fromString("7f3c1a52-5b1e-4c2a-9a63-0c5a11b2d001")

/** A raw byte connection (a TCP socket or a Bluetooth RFCOMM socket). */
class RawConn(val input: InputStream, val output: OutputStream, val closer: () -> Unit)

/** Line-based messaging over a [RawConn]. Reading and writing never happen on the main thread. */
class StreamLink(
    private val raw: RawConn,
    private val onLine: (StreamLink, String) -> Unit,
    private val onClosed: (StreamLink) -> Unit
) {
    private val sender: ExecutorService = Executors.newSingleThreadExecutor()
    private val closed = AtomicBoolean(false)

    fun start() {
        val t = Thread {
            try {
                val reader = BufferedReader(InputStreamReader(raw.input, Charsets.UTF_8))
                while (true) {
                    val line = reader.readLine() ?: break
                    onLine(this, line)
                }
            } catch (e: Exception) {
                // connection ended
            } finally {
                close()
            }
        }
        t.isDaemon = true
        t.start()
    }

    fun send(line: String) {
        if (closed.get()) return
        try {
            sender.execute {
                try {
                    raw.output.write((line + "\n").toByteArray(Charsets.UTF_8))
                    raw.output.flush()
                } catch (e: Exception) {
                    close()
                }
            }
        } catch (e: Exception) {
            // executor already shut down
        }
    }

    fun close() {
        if (!closed.compareAndSet(false, true)) return
        try { raw.closer() } catch (e: Exception) { }
        try { sender.shutdownNow() } catch (e: Exception) { }
        onClosed(this)
    }
}

interface HostTransport {
    fun start(onConn: (RawConn) -> Unit, onError: (String) -> Unit)
    fun stop()
}

/** Wi-Fi (same network or phone hotspot) host using a plain TCP server socket. */
class TcpHost(private val port: Int) : HostTransport {
    @Volatile private var server: ServerSocket? = null
    @Volatile private var running = false

    override fun start(onConn: (RawConn) -> Unit, onError: (String) -> Unit) {
        running = true
        val t = Thread {
            try {
                val ss = ServerSocket()
                ss.reuseAddress = true
                ss.bind(InetSocketAddress(port))
                server = ss
                while (running) {
                    val s = ss.accept()
                    s.tcpNoDelay = true
                    onConn(RawConn(s.getInputStream(), s.getOutputStream()) {
                        try { s.close() } catch (e: Exception) { }
                    })
                }
            } catch (e: Exception) {
                if (running) onError("Wi-Fi hosting failed: ${e.message}")
            }
        }
        t.isDaemon = true
        t.start()
    }

    override fun stop() {
        running = false
        try { server?.close() } catch (e: Exception) { }
    }
}

class TcpClient(private val host: String, private val port: Int) {
    fun connect(onConn: (RawConn) -> Unit, onError: (String) -> Unit) {
        val t = Thread {
            try {
                val s = Socket()
                s.connect(InetSocketAddress(host, port), 6000)
                s.tcpNoDelay = true
                onConn(RawConn(s.getInputStream(), s.getOutputStream()) {
                    try { s.close() } catch (e: Exception) { }
                })
            } catch (e: Exception) {
                onError("Could not connect: ${e.message ?: "check the address and the Wi-Fi"}")
            }
        }
        t.isDaemon = true
        t.start()
    }
}

/** Bluetooth host (classic RFCOMM). Phones must be paired first. */
@SuppressLint("MissingPermission")
class BtHost(private val adapter: BluetoothAdapter) : HostTransport {
    @Volatile private var server: BluetoothServerSocket? = null
    @Volatile private var running = false

    override fun start(onConn: (RawConn) -> Unit, onError: (String) -> Unit) {
        running = true
        val t = Thread {
            try {
                val ss = adapter.listenUsingRfcommWithServiceRecord("Callbrize", CALLBRIZE_UUID)
                server = ss
                while (running) {
                    val s = ss.accept()
                    onConn(RawConn(s.inputStream, s.outputStream) {
                        try { s.close() } catch (e: Exception) { }
                    })
                }
            } catch (e: Exception) {
                if (running) onError("Bluetooth hosting failed: ${e.message}")
            }
        }
        t.isDaemon = true
        t.start()
    }

    override fun stop() {
        running = false
        try { server?.close() } catch (e: Exception) { }
    }
}

@SuppressLint("MissingPermission")
class BtClient(private val adapter: BluetoothAdapter, private val address: String) {
    fun connect(onConn: (RawConn) -> Unit, onError: (String) -> Unit) {
        val t = Thread {
            try {
                val device = adapter.getRemoteDevice(address)
                val s = device.createRfcommSocketToServiceRecord(CALLBRIZE_UUID)
                s.connect()
                onConn(RawConn(s.inputStream, s.outputStream) {
                    try { s.close() } catch (e: Exception) { }
                })
            } catch (e: Exception) {
                onError("Could not connect: ${e.message ?: "is the host waiting in its lobby?"}")
            }
        }
        t.isDaemon = true
        t.start()
    }
}
