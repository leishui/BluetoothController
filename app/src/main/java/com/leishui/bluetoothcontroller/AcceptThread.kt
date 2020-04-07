package com.leishui.bluetoothcontroller

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import java.io.IOException
import java.util.*

private class AcceptThread(bluetoothAdapter: BluetoothAdapter,private val context: Context,uuid: UUID) : Thread() {

    private val NAME = "guo"
    private val TAG = "1008611"
    private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
        bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(NAME, uuid)
    }

    override fun run() {
        // Keep listening until exception occurs or a socket is returned.
        var shouldLoop = true
        while (shouldLoop) {
            val socket: BluetoothSocket? = try {
                mmServerSocket?.accept()
            } catch (e: IOException) {
                Log.e(TAG, "Socket's accept() method failed", e)
                Toast.makeText(context,"Socket's accept() method failed",Toast.LENGTH_SHORT).show()
                shouldLoop = false
                null
            }
            socket?.also {
                Toast.makeText(context,"已连接"+it.remoteDevice.name,Toast.LENGTH_SHORT).show()
                manageMyConnectedSocket(it)
                mmServerSocket?.close()
                shouldLoop = false
            }
        }
    }

    private fun manageMyConnectedSocket(bluetoothSocket: BluetoothSocket) {
        val input = bluetoothSocket.inputStream
        val output = bluetoothSocket.outputStream
        output.write("hello".toByteArray())
        var i = input.read()
        val packageManager = context.packageManager
        while (i!=-1){
            Log.d(TAG,i.toString() )
            if (i==0){
                val intent =packageManager.getLaunchIntentForPackage("com.tencent.mm")
                if(intent==null){
                    Toast.makeText(context, "未安装", Toast.LENGTH_LONG).show()
                }else{
                    context.startActivity(intent)
                }
            }
            if (i==1){
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:18560849224")
                }
                if (intent.resolveActivity(packageManager) != null) {
                    context.startActivity(intent)
                }
            }
            i=input.read()
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
}