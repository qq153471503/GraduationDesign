package com.example.graduationdesign.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import com.example.graduationdesign.R;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by KunGe on 2017/9/8.
 */

public class ConnectDevicesActivity extends ActionBarActivity{

    public static ConnectDevicesActivity connectDevicesActivity;
    public DataOutputStream dataOutputStream;
    public DataInputStream dataInputStream;
    public static final int MSG_STATR_ACTIVITY = 0x01;
    private static final int MSG_LED_ON = 0x02;
    private static final int MSG_LED_OFF = 0x03;
    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String TAG = "ConnectDevicesActivity";
    private Button buttonSerch;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice bluetoothDevice;
    private OutputStream outputStream;
    private ListView listViewDevices;
    private ArrayAdapter<String> arrayAdapter;

    private BroadcastReceiver mRceiver;
    private ArrayList<String> arrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_devices);

        /**
         * 获取本地蓝牙适配器
         */
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        /**
         * 初始化布局控件
         */
        initView();

        /**
         * 检查是否支持蓝牙,如果支持蓝牙,则打开蓝牙
         */
        if (bluetoothAdapter == null){
            finish();
        }else {
            if (!bluetoothAdapter.isEnabled()){
                bluetoothAdapter.enable();
                bluetoothAdapter.cancelDiscovery();
            }
        }

        /**
         * 搜索蓝牙设备
         */
        serchBluetoothDevices();

        /**
         * 设置广播接收器
         */
        mRceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)){
                    BluetoothDevice device = intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE);
//                    arrayAdapter.add(device.getName()+ ":" +device.getAddress());
                    String dev = device.getName()+ ":" +device.getAddress();
                    if (!arrayList.contains(dev)){
                        arrayList.add(dev);
                    }
                    //通知ListView适配器,数据已经发生改变
                    arrayAdapter.notifyDataSetChanged();
                }else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                    Log.d(TAG,"搜索结束!");

                    //在主线程中显示搜索结束信息
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ConnectDevicesActivity.this, "搜索结束!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        };//end of 广播接收器

        /**
         * 注册广播
         */
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mRceiver, intentFilter);

        connectDevicesActivity = this;
    }//end of onCreat()

    private void initView() {
        buttonSerch = (Button)findViewById(R.id.id_button_serch);
        buttonSerch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //清空list列表,每搜索一次,重新刷新显示
                arrayList.clear();
                /**
                 * 如果已经连接上数据流,则断开连接
                 */
                if (bluetoothSocket != null){
                    try {
                        if (bluetoothSocket.isConnected())
                            bluetoothSocket.close();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
                /**
                 * 如果正在搜索设备,则取消搜索
                 */
                if (bluetoothAdapter.isDiscovering())
                    bluetoothAdapter.cancelDiscovery();
                Toast.makeText(ConnectDevicesActivity.this, "开始搜索蓝牙设备...", Toast.LENGTH_LONG).show();
                serchBluetoothDevices();
            }
        }); //end of buttonSerch按钮监听

        /**
         * 设置ListView点击事件监听,点击item,连接蓝牙设备
         */
        listViewDevices = (ListView)findViewById(R.id.id_listView_devices);
        arrayList = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,arrayList);
        listViewDevices.setAdapter(arrayAdapter);
        listViewDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String device = arrayAdapter.getItem(i);
                String address = device.substring(device.indexOf(":") + 1).trim();
                Toast.makeText(ConnectDevicesActivity.this, "正在连接,请稍等...", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "address -->  " + address);

                /**
                 * 连接与断开开启新线程原因:  如果不开新线程,程序在close的时候会闪退
                 */
                //连接新的设备时,断开当前连接
                if (bluetoothSocket != null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "in  disconnect Thread ");
                            if (bluetoothSocket.isConnected()) {
                                try {
                                    bluetoothSocket.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }// end of if
                        }
                    }).start();
                }

                //连接新设备
                bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //获得设备
                            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                            bluetoothSocket.connect();
                            outputStream = bluetoothSocket.getOutputStream();
                            dataOutputStream = new DataOutputStream(outputStream);
                            dataInputStream = new DataInputStream(bluetoothSocket.getInputStream());

                            //在主线程中显示连接成功提示信息
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (bluetoothSocket.isConnected())
                                        Toast.makeText(ConnectDevicesActivity.this, "已连接!", Toast.LENGTH_SHORT).show();
                                    Log.d(ConnectDevicesActivity.TAG, "已连接!");
                                    Message msg = new Message();
                                    msg.what = ConnectDevicesActivity.MSG_STATR_ACTIVITY;
                                    mHandler.sendMessage(msg);
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
    }//end of initView()

    @Override
    protected void onDestroy() {
        super.onDestroy();

        /**
         * 关闭流,释放资源
         */
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (outputStream != null)
                        outputStream.close();
                    if (bluetoothSocket.isConnected())
                        bluetoothSocket.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }).start();
        /**
         * 注销广播
         */
        unregisterReceiver(mRceiver);

        /**
         * 关闭蓝牙
         */
        if (bluetoothAdapter.isEnabled())
            bluetoothAdapter.disable();
    }

    /**
     * 函数功能:搜索蓝牙设备
     * startDiscovery : 是一个异步方法
     */
    private void serchBluetoothDevices() {
        while (!bluetoothAdapter.startDiscovery()){
            Log.d(TAG, "尝试失败!");
            try {
                Thread.sleep(100);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case ConnectDevicesActivity.MSG_STATR_ACTIVITY:
                    Intent intent = new Intent("com.example.graduationdesign.HOMEPAGE");
                    intent.addCategory("com.example.graduationdesign.CATEGORY");
//                    Bundle bundle = new Bundle();
//                    bundle.putSerializable("obj", myDataOutputStream);
//                    intent.putExtras(bundle);
                    startActivityForResult(intent,0x01);
                    break;

                default:break;
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case 0x01:

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            bluetoothSocket.close();
                            outputStream.close();
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                }).start();
                Toast.makeText(ConnectDevicesActivity.this, "已断开!", Toast.LENGTH_SHORT).show();
                break;

            default:break;
        }
    }//end of function
}

