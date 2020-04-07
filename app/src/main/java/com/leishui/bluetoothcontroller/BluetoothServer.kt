package com.leishui.bluetoothcontroller

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.util.Log
import java.io.IOException
import java.io.OutputStream
import java.util.*

class BluetoothServer(
    private val handler: Handler,
    bluetoothAdapter: BluetoothAdapter,
    private val context: Context,
    uuid: UUID
) : Thread() {

    companion object {
        const val MESSAGE_SERVER_READ = 4
        const val MESSAGE_SERVER_WRITE = 5
    }

    private val NAME = "guo"
    private val TAG = "1008611"
    private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream
    private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
        bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, uuid)
    }
    private lateinit var output:OutputStream

    override fun run() {
        // Keep listening until exception occurs or a socket is returned.
        var shouldLoop = true
        while (shouldLoop) {
            val socket: BluetoothSocket? = try {
                mmServerSocket?.accept()
            } catch (e: IOException) {
                Log.e(TAG, "Socket's accept() method failed", e)
                //Toast.makeText(context,"Socket's accept() method failed",Toast.LENGTH_SHORT).show()
                val msg = handler.obtainMessage(MESSAGE_SERVER_READ, "Socket's accept() method failed")
                msg.sendToTarget()
                shouldLoop = false
                null
            }
            socket?.also {
                //Toast.makeText(context,"已连接"+it.remoteDevice.name,Toast.LENGTH_SHORT).show()
                val msg = handler.obtainMessage(MESSAGE_SERVER_READ, "已连接" + it.remoteDevice.name)
                msg.sendToTarget()
                manageMyConnectedSocket(it)
                mmServerSocket?.close()
                shouldLoop = false
            }
        }
    }

    private fun manageMyConnectedSocket(bluetoothSocket: BluetoothSocket) {
        val input = bluetoothSocket.inputStream
        output = bluetoothSocket.outputStream
        output.write("hello,we have connected!".toByteArray())
        while (true) {
            // Read from the InputStream.
            val numBytes = try {
                input.read(mmBuffer)
            } catch (e: IOException) {
                Log.e(TAG, "Input stream was disconnected", e)
                val msg = handler.obtainMessage(MESSAGE_SERVER_READ, "Input stream was disconnected")
                msg.sendToTarget()
                break
            }
            Log.d(TAG, numBytes.toString())
            // Send the obtained bytes to the UI activity.
            val readMsg = handler.obtainMessage(
                MESSAGE_SERVER_READ, numBytes, -1,
                String(mmBuffer.copyOf(numBytes)))
            readMsg.sendToTarget()
        }
    }

    // Closes the connect socket and causes the thread to finish.
    fun cancel() {
        try {
            mmServerSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Could not close the connect socket", e)
        }
    }

    fun write(string: String) {
        output?.write(string.toByteArray())
    }
}