package mykotlin.cn.com.mytestdemo;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import mykotlin.cn.com.mytestdemo.utils.FileUtils;


public class MainActivity extends AppCompatActivity {
    @BindView(R.id.isSupportBlueTooth)
    Button isSupportBlueTooth;

    @BindView(R.id.isOpenBlueTooth)
    Button isOpenBlueTooth;

    @BindView(R.id.OpenBlueTooth)
    Button OpenBlueTooth;

    @BindView(R.id.CloseBlueTooth)
    Button CloseBlueTooth;

    @BindView(R.id.SearchBlueToothDevices)
    Button SearchBlueToothDevices;

    @BindView(R.id.SearchAvaliableBlueToothDevices)
    Button SearchAvaliableBlueToothDevices;

    @BindView(R.id.DeviceVisiable)
    Button DeviceVisiable;

    @BindView(R.id.SetDeviceDisVisible)
    Button SetDeviceDisVisible;

    private BlueToothController blueToothController=new BlueToothController();
    private Set<BluetoothDevice> devices=new LinkedHashSet<BluetoothDevice>();//已经配对的设备
    private List<String> mNoMatchdevices=new ArrayList<String>();//可以配对的设备
    private DrawerLayout mDrawerLayout;
    private ListView mlistview;
    private ArrayAdapter<String> myadapter;
    private FileUtils fileutils=new FileUtils();
    private BluetoothDevice device;
    private AcceptThread acceptThread;
    //客户端
    private OutputStream os;//输出流
    private BluetoothSocket clientSocket;
    //服务端
    private final String NAME = "MyTestDemo";
    private final String MYUUID = "00001101-0000-1000-8000-00805F9B34FB";
    private BluetoothServerSocket serverSocket;
    private BluetoothSocket socket;
    private InputStream is;//输入流

    //蓝牙广播接受
    private final BroadcastReceiver mReceiver = new BroadcastReceiver(){
        public void onReceive(Context context, Intent intent){
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 1);
            String action = intent.getAction();
            switch (state){
                case BluetoothAdapter.STATE_OFF:
                    Toast.makeText(MainActivity.this, "蓝牙已关闭", Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothAdapter.STATE_ON:
                    Toast.makeText(MainActivity.this, "蓝牙已打开", Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    Toast.makeText(MainActivity.this, "正在打开蓝牙", Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    Toast.makeText(MainActivity.this, "正在关闭蓝牙", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(MainActivity.this, "未知状态", Toast.LENGTH_SHORT).show();
            }

            //发现了设备
            switch (action){
                case BluetoothDevice.ACTION_FOUND:
                    Toast.makeText(MainActivity.this, "发现设备", Toast.LENGTH_SHORT).show();
                    //从Intent中获取设备的BluetoothDevice对象
                    BluetoothDevice device =  intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                    Log.d("zhangrui",device.getName()+"\n"+ device.getAddress());
                    if(device.getName() != null)
                    mNoMatchdevices.add(device.getName()+"\n"+device.getAddress());
                    myadapter.notifyDataSetChanged();
                    break;
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mDrawerLayout=(DrawerLayout)findViewById(R.id.id_drawerlayout);
        mlistview=(ListView)findViewById(R.id.myList);
        myadapter=new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_expandable_list_item_1,mNoMatchdevices);
        mlistview.setAdapter(myadapter);
        //客户端
        mlistview.setOnItemClickListener(mylisten);

        //服务器端
        AcceptThread acceptThread = new AcceptThread();
        if(!blueToothController.isOpenBlueTooth()){
            mOpenBlueTooth();
        }else {
            acceptThread.start();
        }
    }

    private AdapterView.OnItemClickListener mylisten=new AdapterView.OnItemClickListener(){
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id){
            String s = myadapter.getItem(position);
            String address = s.substring(s.indexOf("\n") + 1).trim();//把地址解析出来
            // 判断当前是否还是正在搜索周边设备，如果是则暂停搜索
            if(blueToothController.getMyadapter().isDiscovering()){
                blueToothController.getMyadapter().cancelDiscovery();
            }
            //获得远程设备
            device = blueToothController.getMyadapter().getRemoteDevice(address);
            if(device != null)
            new ClientThread().start();
        }
    };


    @Override
    protected void onResume(){
        super.onResume();
        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode){
            case RESULT_OK:
                Toast.makeText(MainActivity.this,"蓝牙已经开启",Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(MainActivity.this,"未知错误",Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public static boolean isNeedAdapt(){
        //24以上版本
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    @OnClick({R.id.isSupportBlueTooth,R.id.isOpenBlueTooth,R.id.OpenBlueTooth,R.id.CloseBlueTooth,R.id.SearchBlueToothDevices,R.id.SearchAvaliableBlueToothDevices,R.id.DeviceVisiable,R.id.SetDeviceDisVisible})
    public void MyOnclick(View view){
        switch (view.getId()){
            case R.id.isSupportBlueTooth:
                Toast.makeText(MainActivity.this, "该设备是否支持蓝牙?=" + blueToothController.isSupportBlueTooth(), Toast.LENGTH_SHORT).show();
                break;
            case R.id.isOpenBlueTooth:
                Toast.makeText(MainActivity.this, "该蓝牙设备是否已经打开?=" + blueToothController.isOpenBlueTooth(), Toast.LENGTH_SHORT).show();
                break;
            case R.id.OpenBlueTooth:
                mOpenBlueTooth();
                break;
            case R.id.CloseBlueTooth:
                blueToothController.CloseBlueTooth();
                break;
            case R.id.SearchBlueToothDevices:
                if(blueToothController.isOpenBlueTooth()){
                    //搜索已经配对的设备
                    devices = blueToothController.SearchBlueToothDevices();
                    for (BluetoothDevice bluetoothDevice : devices){
                        Log.d("zhangrui", bluetoothDevice.getName() + "\n" + bluetoothDevice.getAddress());
                    }
                }else{
                    mOpenBlueTooth();
                }
                break;
            case R.id.SearchAvaliableBlueToothDevices:
                if(blueToothController.isOpenBlueTooth()){
                    Toast.makeText(MainActivity.this, "开始搜索可发现的设备?=" + blueToothController.SearchAvaliableBlueToothDevices(), Toast.LENGTH_SHORT).show();
                }else {
                    mOpenBlueTooth();
                }
                break;
            case R.id.DeviceVisiable:
                 blueToothController.SetDeviceVisiable(MainActivity.this);
                break;
            case R.id.SetDeviceDisVisible:
                 blueToothController.SetDeviceDisVisible();
                break;
        }
    }


    @Override
    protected void onPause(){
        super.onPause();
        unregisterReceiver(mReceiver);
    }


    public void mOpenBlueTooth(){
        if (isNeedAdapt()){
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                // 申请一个（或多个）权限，并提供用于回调返回的获取码（用户定义）
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            }
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                // 申请一个（或多个）权限，并提供用于回调返回的获取码（用户定义）
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
            }
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                // 申请一个（或多个）权限，并提供用于回调返回的获取码（用户定义）
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 102);
            }
            if (checkSelfPermission(Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS) != PackageManager.PERMISSION_GRANTED){
                // 申请一个（或多个）权限，并提供用于回调返回的获取码（用户定义）
                requestPermissions(new String[]{Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS}, 103);
            }
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                // 申请一个（或多个）权限，并提供用于回调返回的获取码（用户定义）
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 104);
            }
        }
        blueToothController.OpenBlueTooth(MainActivity.this,1);
    }


    private Handler handler = new Handler(){
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            Toast.makeText(getApplicationContext(),String.valueOf(msg.obj),Toast.LENGTH_LONG).show();
        }
    };

    //服务端监听客户端的线程类
    private class AcceptThread extends Thread{
        public AcceptThread(){
            try{
                serverSocket = blueToothController.getMyadapter().listenUsingRfcommWithServiceRecord(NAME, UUID.fromString(MYUUID));
            }catch (Exception e){
            }
        }
        public void run(){
            while (true){
                try {
                    socket = serverSocket.accept();
                    is = socket.getInputStream();
                    //文件操作
                    byte[] buffer =new byte[1024];
                    int count = 0;
                    while((count=is.read(buffer))!=-1){

                        Message msg = new Message();
                        msg.obj = new String(buffer, 0, count, "utf-8");
                        handler.sendMessage(msg);
                    }
//                    fileutils.createFileWithByte(buffer);
                }catch(Exception e){
                    try{
                        socket.close();
                    }catch(IOException closeException){ }
                }
            }
        }
    }

    //客户端监听线程
    private class ClientThread extends Thread{
        @Override
        public void run() {
            super.run();
            blueToothController.send(MYUUID,device,MainActivity.this);
        }
    }

}
