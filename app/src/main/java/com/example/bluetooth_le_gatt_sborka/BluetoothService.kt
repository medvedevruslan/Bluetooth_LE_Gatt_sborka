package com.example.bluetooth_le_gatt_sborka;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothService {
    public static final String
            DEVICE_NAME = "device_name";
    public static final int
            MESSAGE_STATE_CHANGE = 1,
            MESSAGE_READ = 2,
            MESSAGE_DEVICE_NAME = 4,
            MESSAGE_CONNECTION_FAILED = 6,
            MESSAGE_CONNECTION_LOST = 7,
            STATE_NONE = 0,
            STATE_LISTEN = 1,
            STATE_CONNECTING = 2,
            STATE_CONNECTED = 3;
    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String
            NAME_INSECURE = "SMAC1",
            TAG = "BTService";
    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private final Handler mHandler;
    private ConnectThread mConnectThread;
    private int mState = STATE_NONE;
    private AcceptThread mAcceptThread;
    private ConnectedThread mConnectedThread;
    private boolean mStop;

    public BluetoothService(Handler handler) {
        mHandler = handler;
        mStop = false;
    }

    public synchronized int getState() {
        return mState;
    }

    private synchronized void setState(int state) {
        Log.i(TAG, "setState(" + state + ")");
        mState = state;
        mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public synchronized BluetoothDevice getPairedDeviceByName(String targetName) {
        BluetoothDevice result;
        Set<BluetoothDevice> pairedDevices = mAdapter.getBondedDevices();
        result = null;
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().startsWith(targetName)) {
                    result = device;
                }
            }
        }
        return result;
    }

    public synchronized void start() {
        Log.i(TAG, "start()");
        mStop = false;
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        setState(STATE_LISTEN);
        if (mAcceptThread == null) {
            AcceptThread acceptThread = new AcceptThread();
            mAcceptThread = acceptThread;
            acceptThread.start();
        }
    }

    public synchronized void stop() {
        Log.i(TAG, "stop() mState=" + mState);
        if (mState != STATE_NONE) {
            mStop = true;
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
            if (mConnectedThread != null) {
                mConnectedThread.cancel();
                mConnectedThread = null;
            }
            if (mAcceptThread != null) {
                mAcceptThread.cancel();
                mAcceptThread = null;
            }
            setState(STATE_NONE);
        }
    }

    public synchronized void connect(BluetoothDevice device) {
        Log.i(TAG, "connect(" + device.getName() + "/" + device.getAddress() + ")");
        if (mState == STATE_CONNECTING && mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        ConnectThread connectThread = new ConnectThread(device);
        mConnectThread = connectThread;
        connectThread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        Log.i(TAG, "connected()");
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        ConnectedThread connectedThread = new ConnectedThread(socket);
        mConnectedThread = connectedThread;
        connectedThread.start();
        Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setState(STATE_CONNECTED);
    }

    private void connectionFailed() {
        synchronized (this) {
            Log.i(TAG, "connectionFailed() mStop=" + this.mStop);
            if (!mStop) {
                mHandler.obtainMessage(MESSAGE_CONNECTION_FAILED).sendToTarget();
                start();
            }
        }
    }

    private void connectionLost() {
        synchronized (this) {
            Log.i(TAG, "connectionLost() mStop=" + mStop);
            if (!mStop) {
                mHandler.obtainMessage(MESSAGE_CONNECTION_LOST).sendToTarget();
            }
        }
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            Log.i(TAG, "AcceptThread()");
            BluetoothServerSocket tmp = null;
            try {
                tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME_INSECURE, MY_UUID_INSECURE);
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread() failed " + e.getMessage());
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "AcceptThread.run:" + Thread.currentThread().getId());
            setName("AcceptThread");
            while (mState != STATE_CONNECTED) {
                try {
                    BluetoothSocket socket = mmServerSocket.accept();
                    if (socket != null) {
                        synchronized (this) {
                            if (mState != STATE_NONE) {
                                if (mState == STATE_LISTEN || mState == STATE_CONNECTING) {
                                    connected(socket, socket.getRemoteDevice());
                                }
                            }
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e(TAG, "AcceptThread.run: Could not close unwanted socket " + e.getMessage());
                            }
                        }
                    }
                } catch (IOException e2) {
                    Log.e(TAG, "AcceptThread.run: accept() failed " + e2.getMessage());
                }
            }
            Log.i(TAG, "AcceptThread.end");
        }

        public void cancel() {
            Log.i(TAG, "AcceptThread.cancel");
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread.cancel failed " + e.getMessage());
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothDevice mmDevice;
        private final BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device) {
            Log.i(TAG, "ConnectThread()");
            mmDevice = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread(): createRfcommSocketToServiceRecord() failed " + e.getMessage());
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "ConnectThread.run:" + Thread.currentThread().getId());
            setName("ConnectThread");
            mAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
                synchronized (this) {
                    mConnectThread = null;
                }
                connected(mmSocket, mmDevice);
                Log.i(TAG, "ConnectThread.end");
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "ConnectThread.run: unable to close() socket during connection failure " + e2.getMessage());
                }
                connectionFailed();
            }
        }

        public void cancel() {
            Log.i(TAG, "ConnectThread.cancel");
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread.cancel failed " + e.getMessage());
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final BluetoothSocket mmSocket;

        public ConnectedThread(BluetoothSocket socket) {
            Log.i(TAG, "ConnectedThread()");
            mmSocket = socket;
            InputStream tmpIn = null;
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread(): temp sockets not created " + e.getMessage());
            }
            mmInStream = tmpIn;
        }

        public void run() {
            Log.i(TAG, "ConnectedThread.run:" + Thread.currentThread().getId());
            byte[] buffer = new byte[1024];
            while (true) {
                try {
                    int bytes = mmInStream.read(buffer, 0, buffer.length);
                    if (bytes > 0) {
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "ConnectedThread.run: disconnected " + e.getMessage());
                    connectionLost();
                    Log.i(TAG, "ConnectedThread.end");
                    return;
                }
            }
        }

        public void cancel() {
            Log.i(TAG, "ConnectedThread.cancel");
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "ConnectedThread.cancel failed " + e.getMessage());
            }
        }
    }
}
