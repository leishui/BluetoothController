package com.leishui.bluetoothcontroller

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.Color
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat


class WorkService : Service() {
    // Binder given to clients
    private val binder = LocalBinder()
    private var msgListener: MsgListener? = null
    private var notification: Notification? = null

    private val handler =
        Handler(Handler.Callback { msg ->
            when (msg.what) {
                MyBluetoothService.MESSAGE_READ -> {
                    val s = msg.obj as String
                    msgListener?.clientRead(s)
                    parse(s)
                }
                MyBluetoothService.MESSAGE_WRITE -> {
                    msgListener?.clientWrite(msg.obj as String)
                }
                MyBluetoothService.MESSAGE_TOAST -> {
                    msgListener?.toast(msg.data.getString("toast")!!)
                }
                MyBluetoothService.MESSAGE_ERROR -> {
                    msgListener?.error(msg.obj as String)
                }
                BluetoothServer.MESSAGE_SERVER_READ -> {
                    val s = msg.obj as String
                    msgListener?.serverRead(s)
                    parse(s)
                }
                BluetoothServer.MESSAGE_SERVER_WRITE -> {
                    msgListener?.serverWrite(msg.obj as String)
                }
            }
            false
        })

    interface MsgListener {
        fun clientRead(string: String)
        fun clientWrite(string: String)
        fun serverRead(string: String)
        fun serverWrite(string: String)
        fun error(string: String)
        fun toast(string: String)
        fun failed(string: String)
    }

    fun setMsgListener(msgListener: MsgListener) {
        this.msgListener = msgListener
    }

    fun getHandler():Handler{
        return handler
    }
    fun startForForeground(){
        if (Build.VERSION.SDK_INT >= 26) {
            startMyOwnForeground()
        } else {
            startMyOwnForeground2()
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun startMyOwnForeground() {
        val channelId = "com.leishui.bluetoothcontroller"
        val channelName = "BluetoothController"
        val chan = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(chan)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
        notification = notificationBuilder.setOngoing(true)
            .setContentTitle("BluetoothController")
            .setContentText("App is running in background")
            .setSmallIcon(R.drawable.ic_bluetooth_black_24dp)
            .setPriority(NotificationManager.IMPORTANCE_HIGH)
            .setCategory(Notification.EXTRA_MEDIA_SESSION)
            .build()
        startForeground(1, notification)
    }


    private fun startMyOwnForeground2() {
        notification = NotificationCompat.Builder(this, "1")
            .setContentTitle("BluetoothController")
            .setContentText("App is running in background")
            .setSmallIcon(R.drawable.ic_bluetooth_black_24dp)
            .build()
        startForeground(1, notification)
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): WorkService = this@WorkService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private fun openApp(string: String) {
        val packageManager = this.packageManager
        val intent = packageManager.getLaunchIntentForPackage(string)
        if (intent != null)
            startActivity(intent)
        else
            msgListener?.failed("不存在该应用")
    }

    @SuppressLint("MissingPermission")
    private fun call(string: String){
        val uri: Uri = Uri.parse("tel:$string")
        val intent = Intent(Intent.ACTION_CALL, uri).apply { flags=FLAG_ACTIVITY_NEW_TASK }
        if (intent != null)
            startActivity(intent)
        else
            msgListener?.failed("拨打失败")
    }

    private fun parse(string: String){
        val length = string.length
        when {
            string.startsWith("打开应用") -> {
                openApp(string.substring(4,length))
                msgListener?.toast("openApp:  "+string.substring(4,length))
            }
            string.startsWith("拨打") -> {
                call(string.substring(2,length))
                msgListener?.toast("call:  "+string.substring(2,length))
            }
            string.startsWith("open") -> {
                openApp(string.substring(4,length))
                msgListener?.toast("openApp:  "+string.substring(4,length))
            }
            string.startsWith("call") -> {
                call(string.substring(4,length))
                msgListener?.toast("call:  "+string.substring(2,length))
            }
        }
    }

}