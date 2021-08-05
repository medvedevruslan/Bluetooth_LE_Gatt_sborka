package com.example.bluetooth_le_gatt_sborka

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.os.Handler
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.util.*

class BluetoothService(
    private val handler: Handler,
    private val bluetoothAdapter: BluetoothAdapter
) {
    private var _state = STATE_NONE

    @get:Synchronized
    @set:Synchronized
    var state: Int
        get() = _state
        private set(state) {
            Log.i(TAG, "setState($state)")
            _state = state
            handler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget()
        }

    private var connectThread: ConnectThread? = null
    private var acceptThread: AcceptThread? = null
    private var connectedThread: ConnectedThread? = null
    private var stop = false

    @Synchronized
    fun getPairedDeviceByName(targetName: String?): BluetoothDevice? {
        var result: BluetoothDevice?
        val pairedDevices = bluetoothAdapter.bondedDevices
        result = null
        if (pairedDevices.size > 0) {
            for (device in pairedDevices) {
                if (device.name.startsWith(targetName!!)) {
                    result = device
                }
            }
        }
        return result
    }

    @Synchronized
    fun start() {
        Log.i(TAG, "start()")
        stop = false
        if (connectThread != null) {
            connectThread!!.cancel()
            connectThread = null
        }
        if (connectedThread != null) {
            connectedThread!!.cancel()
            connectedThread = null
        }
        state = STATE_LISTEN
        if (acceptThread == null) {
            acceptThread = AcceptThread()
            acceptThread!!.start()
        }
    }

    @Synchronized
    fun stop() {
        Log.i(TAG, "stop() mState=$_state")
        if (_state != STATE_NONE) {
            stop = true
            if (connectThread != null) {
                connectThread!!.cancel()
                connectThread = null
            }
            if (connectedThread != null) {
                connectedThread!!.cancel()
                connectedThread = null
            }
            if (acceptThread != null) {
                acceptThread!!.cancel()
                acceptThread = null
            }
            state = STATE_NONE
        }
    }

    @Synchronized
    fun connect(device: BluetoothDevice) {
        Log.i(TAG, "connect(" + device.name + "/" + device.address + ")")
        if (_state == STATE_CONNECTING && connectThread != null) {
            connectThread!!.cancel()
            connectThread = null
        }
        if (connectedThread != null) {
            connectedThread!!.cancel()
            connectedThread = null
        }
        connectThread = ConnectThread(device)
        connectThread!!.start()
        state = STATE_CONNECTING
    }

    @Synchronized
    fun connected(socket: BluetoothSocket, device: BluetoothDevice) {
        Log.i(TAG, "connected()")
        if (connectThread != null) {
            connectThread!!.cancel()
            connectThread = null
        }
        if (connectedThread != null) {
            connectedThread!!.cancel()
            connectedThread = null
        }
        if (acceptThread != null) {
            acceptThread!!.cancel()
            acceptThread = null
        }
        connectedThread = ConnectedThread(socket)
        connectedThread!!.start()
        val msg = handler.obtainMessage(MESSAGE_DEVICE_NAME)
        val bundle = Bundle()
        bundle.putString(DEVICE_NAME, device.name)
        msg.data = bundle
        handler.sendMessage(msg)
        state = STATE_CONNECTED
    }

    private fun connectionFailed() {
        synchronized(this) {
            Log.i(TAG, "connectionFailed() mStop=$stop")
            if (!stop) {
                handler.obtainMessage(MESSAGE_CONNECTION_FAILED).sendToTarget()
                start()
            }
        }
    }

    private fun connectionLost() {
        synchronized(this) {
            Log.i(TAG, "connectionLost() mStop=$stop")
            if (!stop) {
                handler.obtainMessage(MESSAGE_CONNECTION_LOST).sendToTarget()
            }
        }
    }

    private inner class AcceptThread : Thread() {
        private val serverSocket: BluetoothServerSocket
        override fun run() {
            Log.i(TAG, "AcceptThread.run:" + currentThread().id)
            name = "AcceptThread"
            while (_state != STATE_CONNECTED) {
                try {
                    val socket = serverSocket.accept()
                    if (socket != null) {
                        synchronized(this) {
                            if (_state != STATE_NONE) {
                                if (_state == STATE_LISTEN || _state == STATE_CONNECTING) {
                                    connected(socket, socket.remoteDevice)
                                }
                            }
                            try {
                                socket.close()
                            } catch (e: IOException) {
                                Log.e(
                                    TAG,
                                    "AcceptThread.run: Could not close unwanted socket " + e.message
                                )
                            }
                        }
                    }
                } catch (e2: IOException) {
                    Log.e(TAG, "AcceptThread.run: accept() failed " + e2.message)
                }
            }
            Log.i(TAG, "AcceptThread.end")
        }

        fun cancel() {
            Log.i(TAG, "AcceptThread.cancel")
            try {
                serverSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "AcceptThread.cancel failed " + e.message)
            }
        }

        init {
            Log.i(TAG, "AcceptThread()")
            lateinit var tmp: BluetoothServerSocket
            try {
                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
                    NAME_INSECURE,
                    MY_UUID_INSECURE
                )
            } catch (e: IOException) {
                Log.e(TAG, "AcceptThread() failed " + e.message)
            }
            serverSocket = tmp
        }
    }

    private inner class ConnectThread(device: BluetoothDevice) : Thread() {
        private val device: BluetoothDevice
        private val socket: BluetoothSocket
        override fun run() {
            Log.i(TAG, "ConnectThread.run:" + currentThread().id)
            name = "ConnectThread"
            bluetoothAdapter.cancelDiscovery()
            try {
                socket.connect()
                synchronized(this) { connectThread = null }
                connected(socket, device)
                Log.i(TAG, "ConnectThread.end")
            } catch (e: IOException) {
                try {
                    socket.close()
                } catch (e2: IOException) {
                    Log.e(
                        TAG,
                        "ConnectThread.run: unable to close() socket during connection failure " + e2.message
                    )
                }
                connectionFailed()
            }
        }

        fun cancel() {
            Log.i(TAG, "ConnectThread.cancel")
            try {
                socket.close()
            } catch (e: IOException) {
                Log.e(TAG, "ConnectThread.cancel failed " + e.message)
            }
        }

        init {
            Log.i(TAG, "ConnectThread()")
            this.device = device
            lateinit var tmp: BluetoothSocket
            try {
                tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE)
            } catch (e: IOException) {
                Log.e(
                    TAG,
                    "ConnectThread(): createRfcommSocketToServiceRecord() failed " + e.message
                )
            }
            socket = tmp
        }
    }

    private inner class ConnectedThread(socket: BluetoothSocket) : Thread() {
        private val inStream: InputStream
        private val socket: BluetoothSocket
        override fun run() {
            Log.i(TAG, "ConnectedThread.run:" + currentThread().id)
            val buffer = ByteArray(1024)
            while (true) {
                try {
                    val bytes = inStream.read(buffer, 0, buffer.size)
                    if (bytes > 0) {
                        handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget()
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "ConnectedThread.run: disconnected " + e.message)
                    connectionLost()
                    Log.i(TAG, "ConnectedThread.end")
                    return
                }
            }
        }

        fun cancel() {
            Log.i(TAG, "ConnectedThread.cancel")
            try {
                socket.close()
            } catch (e: IOException) {
                Log.e(TAG, "ConnectedThread.cancel failed " + e.message)
            }
        }

        init {
            Log.i(TAG, "ConnectedThread()")
            this.socket = socket
            lateinit var tmpIn: InputStream
            try {
                tmpIn = socket.inputStream
            } catch (e: IOException) {
                Log.e(TAG, "ConnectThread(): temp sockets not created " + e.message)
            }
            inStream = tmpIn
        }
    }

    companion object {
        const val DEVICE_NAME = "device_name"
        const val MESSAGE_STATE_CHANGE = 1
        const val MESSAGE_READ = 2
        const val MESSAGE_DEVICE_NAME = 4
        const val MESSAGE_CONNECTION_FAILED = 6
        const val MESSAGE_CONNECTION_LOST = 7
        const val STATE_NONE = 0
        const val STATE_LISTEN = 1
        const val STATE_CONNECTING = 2
        const val STATE_CONNECTED = 3
        private val MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val NAME_INSECURE = "SMAC1"
        private const val TAG = "BTService"
    }
}