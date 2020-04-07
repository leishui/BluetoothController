package com.leishui.bluetoothcontroller

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_test.*

class MainActivity : AppCompatActivity() {

    private var bluetoothAdapter:BluetoothAdapter? = null
//    private var string=""
//    private val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
//    private val btStateListener = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
//    private val discoveryFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
//    private val stateReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            if (intent.getIntExtra(
//                    BluetoothAdapter.EXTRA_STATE,
//                    0
//                ) == BluetoothAdapter.STATE_OFF
//            )
//                enableBluetooth()
//        }
//    }
//    private val discoveryReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            if (intent.action.equals(BluetoothDevice.ACTION_FOUND)){
//                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
//                refreshDevice(device)
//                Toast.makeText(this@MainActivity,device.name+device.address,Toast.LENGTH_LONG).show()
//            }
//        }
//    }
//
//    private fun refreshDevice(device: BluetoothDevice?) {
//        string+=device?.name
//        tv_dev.text = string
//    }
//
//    companion object{
//        const val REQUEST_ENABLE_BT = 1
//    }
//
private val handler = Handler{
    //Toast.makeText(this,it.arg1,Toast.LENGTH_SHORT).show()
    tv_msg.text = it.arg1.toString()
    true
}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        init()
    }
    @SuppressLint("SetTextI18n")
    private fun init(){
        bluetoothAdapter = if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN_MR2){
            val bm = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bm.adapter
        }else
            BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter==null)
            Toast.makeText(this,"设备不支持蓝牙！",Toast.LENGTH_LONG).show()
        else {
            val device = BluetoothUtil.getConnectedDevice(bluetoothAdapter)
            if (device!=null){
                val bluetoothSocket = device.createRfcommSocketToServiceRecord(device.uuids[0].uuid)
                val myBluetoothService = MyBluetoothService(handler,bluetoothSocket)
                myBluetoothService.run()
                tv_msg.text = device.address+device.name+device.uuids[0].uuid
            }

        }
    }
//
//    private fun init() {
//        bluetoothAdapter = if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN_MR2){
//            val bm = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//            bm.adapter
//        }else
//            BluetoothAdapter.getDefaultAdapter()
//        if (bluetoothAdapter==null)
//            Toast.makeText(this,"设备不支持蓝牙！",Toast.LENGTH_LONG).show()
//        else if (!bluetoothAdapter!!.isEnabled)
//            enableBluetooth()
//        else
//            bluetooth()
//    }
//
//    private fun bluetooth() {
//        val a = bluetoothAdapter?.bondedDevices
//        a?.forEach {
//            println("---------------------"+it.name+" "+it.address+it.bondState)
//        }
//        //beginDiscovering()
//    }
//
//    private fun beginDiscovering() {
//        if (!bluetoothAdapter!!.isDiscovering){
//            bluetoothAdapter!!.startDiscovery()
//        }
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == REQUEST_ENABLE_BT){
//            if (resultCode== Activity.RESULT_CANCELED)
//                enableBluetooth()
//        }
//    }
//
//    fun enableBluetooth(){
//        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
//    }
//
//    override fun onStart() {
//        super.onStart()
//        registerReceiver(discoveryReceiver,discoveryFilter)
//        registerReceiver(stateReceiver,btStateListener)
//    }
}
