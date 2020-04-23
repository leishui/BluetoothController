package com.leishui.bluetoothcontroller;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * Created by ouyangshen on 2017/12/11.
 */
@SuppressLint("SetTextI18n")
public class BluetoothActivityS extends AppCompatActivity implements
        OnCheckedChangeListener, OnItemClickListener {
    private String l = "";
    private MyBluetoothService bluetoothClient;
    private static final String TAG = "BluetoothActivity";
    private final int mOpenCode = 1;
    private final int mGPSCode = 2;
    private CountDownTimer countDownTimer = new CountDownTimer(120000, 1000) {
        String string;
        String second;

        @Override
        public void onTick(long millisUntilFinished) {
            string = String.valueOf(millisUntilFinished);
            second = string.substring(0, string.length() - 3);
            tv_counter.setText(second.equals("") ? "0" : second + "s");
        }

        @Override
        public void onFinish() {
            tv_counter.setText("未打开本机可被检测");
        }
    };
    private TextView tv_counter;
    private TextView tv_log;
    private CheckBox ck_bluetooth;
    private ScrollView sv_log;
    private TextView tv_discovery;
    private Button bt_discover;
    private EditText et_uuid;
    private EditText et_send;
    private Button bt_send;
    private ListView lv_bluetooth; // 声明一个用于展示蓝牙设备的列表视图对象
    private BluetoothAdapter mBluetooth; // 声明一个蓝牙适配器对象
    private BlueListAdapter mListAdapter; // 声明一个蓝牙设备的列表适配器对象
    private ArrayList<BlueDevice> mDeviceList = new ArrayList<>(); // 蓝牙设备队列
    private Handler mHandler; // 声明一个处理器对象
    //private int mOpenCode = 1; // 是否允许扫描蓝牙设备的选择对话框返回结果代码
    private BluetoothServer bluetoothServer;
    private WorkService mService;
    private boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        initBluetooth(); // 初始化蓝牙适配器
        initViews();
        disableDiscoverButton("未开启蓝牙");
        ck_bluetooth.setOnCheckedChangeListener(this);
        if (BluetoothUtil.getBlueToothStatus(this)) {
            ck_bluetooth.setChecked(true);
            enableDiscoverButton();
        }
        initBlueDevice(); // 初始化蓝牙设备列表
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            requestPermission();
        }
    }

    private void initViews() {
        ck_bluetooth = findViewById(R.id.ck_bluetooth);
        tv_discovery = findViewById(R.id.tv_discovery);
        tv_counter = findViewById(R.id.tv_counter);
        tv_log = findViewById(R.id.tv_log);
        lv_bluetooth = findViewById(R.id.lv_bluetooth);
        bt_discover = findViewById(R.id.bt_discover);
        sv_log = findViewById(R.id.sv_log);
        et_uuid = findViewById(R.id.et_uuid);
        et_send = findViewById(R.id.et_send);
        bt_send = findViewById(R.id.bt_send);
        bt_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothClient != null) {
                    bluetoothClient.write(et_send.getText().toString());
                } else if (bluetoothServer != null) {
                    bluetoothServer.write(et_send.getText().toString());
                }
            }
        });
        bt_discover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.post(mDiscoverable);
                if (Build.VERSION.SDK_INT >= 6.0) {
                    if (isGPSOpen())
                        mHandler.postDelayed(mRefresh, 50);
                    else
                        openGPSSEtting();
                } else
                    mHandler.postDelayed(mRefresh, 50);
            }
        });
        bt_discover.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (bluetoothServer == null) {
                    bluetoothServer = new BluetoothServer(mHandler, mBluetooth, BluetoothActivityS.this, UUID.fromString(et_uuid.getText().toString()));
                    bluetoothServer.start();
                }
                if (bluetoothClient != null) {
                    bluetoothClient.cancel();
                    bluetoothClient = null;
                }
                log("开启作为服务端");
                return true;
            }
        });
    }

    private boolean isGPSOpen() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    // 初始化蓝牙适配器
    private void initBluetooth() {

        //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        //openGPSSEtting();
        // Android从4.3开始增加支持BLE技术（即蓝牙4.0及以上版本）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // 从系统服务中获取蓝牙管理器
            BluetoothManager bm = (BluetoothManager)
                    getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetooth = bm.getAdapter();
        } else {
            // 获取系统默认的蓝牙适配器
            mBluetooth = BluetoothAdapter.getDefaultAdapter();
        }
        if (mBluetooth == null) {
            Toast.makeText(this, "本机未找到蓝牙功能", Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    private void openGPSSEtting() {
        new AlertDialog.Builder(this).setTitle("打开位置信息（GPS）")
                .setMessage("未打开位置信息（GPS）可能会造成无法进行扫描周边蓝牙设备")
                //  取消选项
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(BluetoothActivityS.this, "取消打开位置信息（GPS）", Toast.LENGTH_SHORT).show();
                        // 关闭dialog
                        dialogInterface.dismiss();
                    }
                })
                //  确认选项
                .setPositiveButton("去打开", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //跳转到手机原生设置页面
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent, mGPSCode);
                    }
                })
                .setCancelable(false)
                .show();
    }


    // 初始化蓝牙设备列表
    private void initBlueDevice() {
        mDeviceList.clear();
        // 获取已经配对的蓝牙设备集合
        Set<BluetoothDevice> bondedDevices = mBluetooth.getBondedDevices();
        for (BluetoothDevice device : bondedDevices) {
            mDeviceList.add(new BlueDevice(device.getName(), device.getAddress(), device.getBondState()));
        }

        if (mListAdapter == null) { // 首次打开页面，则创建一个新的蓝牙设备列表
            mListAdapter = new BlueListAdapter(this, mDeviceList);
            lv_bluetooth.setAdapter(mListAdapter);
            lv_bluetooth.setOnItemClickListener(this);
        } else { // 不是首次打开页面，则刷新蓝牙设备列表
            mListAdapter.notifyDataSetChanged();
        }
    }

    private Runnable mDiscoverable = new Runnable() {
        public void run() {
            if (tv_counter.getText().equals("未打开本机可被检测"))
                // Android8.0要在已打开蓝牙功能时才会弹出下面的选择窗
                if (BluetoothUtil.getBlueToothStatus(BluetoothActivityS.this)) {
                    // 弹出是否允许扫描蓝牙设备的选择对话框
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    startActivityForResult(intent, mOpenCode);
                } else {
                    mHandler.postDelayed(this, 1000);
                }
        }
    };

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == R.id.ck_bluetooth) {
            if (isChecked) { // 开启蓝牙功能
                ck_bluetooth.setText("蓝牙开");
                enableDiscoverButton();
                if (!BluetoothUtil.getBlueToothStatus(this)) {
                    BluetoothUtil.setBlueToothStatus(this, true); // 开启蓝牙功能
                }
            } else { // 关闭蓝牙功能
                ck_bluetooth.setText("蓝牙关");
                cancelDiscovery(""); // 取消蓝牙设备的搜索
                disableDiscoverButton("蓝牙已关闭");
                countDownTimer.cancel();
                BluetoothUtil.setBlueToothStatus(this, false); // 关闭蓝牙功能
                initBlueDevice(); // 初始化蓝牙设备列表
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == mOpenCode) { // 来自允许蓝牙扫描的对话框
            // 延迟50毫秒后启动蓝牙设备的刷新任务
            mHandler.postDelayed(mRefresh, 50);
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "不允许蓝牙被附近的其它蓝牙设备发现",
                        Toast.LENGTH_SHORT).show();
            } else
                countDownTimer.start();
        } else if (requestCode == mGPSCode) {
            if (isGPSOpen()) {
                Toast.makeText(this, "成功打开位置信息（GPS）",
                        Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "未打开位置信息（GPS）",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 定义一个刷新任务，每隔两秒刷新扫描到的蓝牙设备
    private Runnable mRefresh = new Runnable() {
        @Override
        public void run() {
            beginDiscovery(); // 开始扫描周围的蓝牙设备
            // 延迟2秒后再次启动蓝牙设备的刷新任务
            mHandler.postDelayed(this, 2000);
        }
    };

    // 开始扫描周围的蓝牙设备
    private void beginDiscovery() {
        // 如果当前不是正在搜索，则开始新的搜索任务
        if (!mBluetooth.isDiscovering()) {
            initBlueDevice(); // 初始化蓝牙设备列表
            tv_discovery.setText("正在搜索蓝牙设备");
            disableDiscoverButton("扫描中");
            mBluetooth.startDiscovery(); // 开始扫描周围的蓝牙设备
        }
    }

    private void cancelDiscovery() {
        cancelDiscovery("取消搜索蓝牙设备");
    }

    // 取消蓝牙设备的搜索
    private void cancelDiscovery(String text) {
        mHandler.removeCallbacks(mRefresh);
        tv_discovery.setText(text);
        log(text);
        //disableDiscoverButton("蓝牙已关闭");
        // 当前正在搜索，则取消搜索任务
        if (mBluetooth.isDiscovering()) {
            mBluetooth.cancelDiscovery(); // 取消扫描周围的蓝牙设备
        }
    }

    private void enableDiscoverButton() {
        bt_discover.setText("扫描周边蓝牙设备");
        bt_discover.setClickable(true);
    }

    private void disableDiscoverButton(String string) {
        bt_discover.setText(string);
        bt_discover.setClickable(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, WorkService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        //mHandler.postDelayed(mRefresh, 50);
        // 需要过滤多个动作，则调用IntentFilter对象的addAction添加新动作
        IntentFilter discoveryFilter = new IntentFilter();
        discoveryFilter.addAction(BluetoothDevice.ACTION_FOUND);
        discoveryFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        discoveryFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        discoveryFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        // 注册蓝牙设备搜索的广播接收器
        registerReceiver(discoveryReceiver, discoveryFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(connection);
        mBound = false;
        cancelDiscovery(""); // 取消蓝牙设备的搜索
        // 注销蓝牙设备搜索的广播接收器
        unregisterReceiver(discoveryReceiver);
    }

    // 蓝牙设备的搜索结果通过广播返回
    private BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            log(action);
            // 获得已经搜索到的蓝牙设备
            if (action != null) {
                switch (action) {
                    case BluetoothDevice.ACTION_FOUND: { // 发现新的蓝牙设备
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        log("name=" + device.getName() + ", state=" + device.getBondState());
                        refreshDevice(device, device.getBondState()); // 将发现的蓝牙设备加入到设备列表

                        break;
                    }
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:  // 搜索完毕
                        mHandler.removeCallbacks(mRefresh); // 需要持续搜索就要注释这行
                        enableDiscoverButton();
                        tv_discovery.setText("蓝牙设备搜索完成");
                        log("蓝牙设备搜索完成");
                        break;
                    case BluetoothDevice.ACTION_BOND_STATE_CHANGED: { // 配对状态变更
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (device != null) {
                            if (device.getBondState() == BluetoothDevice.BOND_BONDING) {
                                tv_discovery.setText("正在配对" + device.getName());
                                log("正在配对\t" + device.getName());
                            } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                                tv_discovery.setText("完成配对" + device.getName());
                                log("已完成配对\t" + device.getName());
                                mHandler.postDelayed(mRefresh, 50);
                            } else if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                                tv_discovery.setText("取消配对" + device.getName());
                                log("取消配对\t" + device.getName());
                                refreshDevice(device, device.getBondState());
                            }
                        }
                        break;
                    }
                    case BluetoothDevice.ACTION_ACL_CONNECTED: {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (device != null) {
                            refreshDevice(device, BlueListAdapter.CONNECTED);
                            log("已连接\t" + device.getName());
                        }
                        break;
                    }
                    case BluetoothDevice.ACTION_ACL_DISCONNECTED: {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (device != null) {
                            refreshDevice(device, 2);
                            log("已断开连接\t" + device.getName());
                        }
                        break;
                    }
                }
            }
        }
    };

    // 刷新蓝牙设备列表
    private void refreshDevice(BluetoothDevice device, int state) {
        int i;
        for (i = 0; i < mDeviceList.size(); i++) {
            BlueDevice item = mDeviceList.get(i);
            if (item.address.equals(device.getAddress())) {
                item.state = state;
                mDeviceList.set(i, item);
                break;
            }
        }
        if (i >= mDeviceList.size()) {
            mDeviceList.add(new BlueDevice(device.getName(), device.getAddress(), device.getBondState()));
        }
        mListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        cancelDiscovery();
        enableDiscoverButton();
        BlueDevice item = mDeviceList.get(position);
        // 根据设备地址获得远端的蓝牙设备对象
        BluetoothDevice device = mBluetooth.getRemoteDevice(item.address);
        Log.d(TAG, "getBondState=" + device.getBondState() + ", item.state=" + item.state);
        if (device.getBondState() == BluetoothDevice.BOND_NONE) { // 尚未配对
            BluetoothUtil.createBond(device); // 创建配对信息
        } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) { // 已经配对
            log("尝试连接");
            if (bluetoothServer != null) {
                bluetoothServer.cancel();
            }
            BluetoothSocket bluetoothSocket = BluetoothUtil.connect(device, UUID.fromString(et_uuid.getText().toString()));
            if (bluetoothSocket != null) {
                try {
                    bluetoothSocket.connect();
                } catch (IOException e) {
                    e.printStackTrace();
                    log("连接失败");
                } finally {
                    if (bluetoothSocket.isConnected()) {
                        log("连接成功");
                        bluetoothClient = new MyBluetoothService(mHandler, bluetoothSocket);
                        bluetoothClient.run();
                    }
                }
            }
//        }else if (device.getBondState() == B){
//
//        }
        }

    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private void log(String s) {
        Date date = new Date();
        String format = DateFormat.getTimeInstance().format(date);
        l += ("\n" + format + "\t" + s);
        tv_log.setText(l);
        //sv_log.scrollToDescendant(v_edge);
        sv_log.fullScroll(ScrollView.FOCUS_DOWN);
    }


    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            WorkService.LocalBinder binder = (WorkService.LocalBinder) service;
            mService = binder.getService();
            mService.startForForeground();
            mService.setMsgListener(new MsgListener());
            mHandler = mService.getHandler();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    protected void onResume() {
        if (mService != null)
            mService.setMsgListener(new MsgListener());
        super.onResume();
    }

    class MsgListener implements WorkService.MsgListener {
        @Override
        public void clientRead(@NotNull String string) {
            log("read:  " + string);
        }

        @Override
        public void clientWrite(@NotNull String string) {
            log("write:  " + string);
        }

        @Override
        public void serverRead(@NotNull String string) {
            log("read:  " + string);
        }

        @Override
        public void serverWrite(@NotNull String string) {
            log("write:  " + string);
        }

        @Override
        public void error(@NotNull String string) {
            log("error:  " + string);
        }

        @Override
        public void toast(@NotNull String string) {
            log("toast:  " + string);
        }

        @Override
        public void failed(@NotNull String string) {
            log("failed: " + string);
        }

    }
    private void requestPermission() {

        Log.i(TAG,"requestPermission");
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG,"checkSelfPermission");
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CALL_PHONE)) {
                Log.i(TAG,"shouldShowRequestPermissionRationale");
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CALL_PHONE},
                        11);

            } else {
                Log.i(TAG,"requestPermissions");
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CALL_PHONE},
                        11);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NotNull String[] permissions, @NotNull int[] grantResults) {
        if (requestCode == 11) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "onRequestPermissionsResult granted");
                // permission was granted, yay! Do the
                // contacts-related task you need to do.

            } else {
                Log.i(TAG, "onRequestPermissionsResult denied");
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                requestPermission();
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}
