package mykotlin.cn.com.mytestdemo;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresPermission;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import static android.os.Environment.getExternalStorageDirectory;

/**
 * Created by topwise on 17-12-11.
 */

public class BlueToothController {
    //蓝牙适配器
    private BluetoothAdapter myadapter;
    // 获取到选中设备的客户端串口，全局变量，否则连接在方法执行完就结束了
    private BluetoothSocket clientSocket;
    // 获取到向设备写的输出流，全局变量，否则连接在方法执行完就结束了
    private OutputStream os;

    public BlueToothController(){
        myadapter=BluetoothAdapter.getDefaultAdapter();
    }
    public BluetoothAdapter getMyadapter(){
        return myadapter;
    }
    //是否支持蓝牙设备
    public boolean isSupportBlueTooth(){
        boolean isSupport=false;
        if(myadapter != null){
            isSupport=true;
        }
        return isSupport;
    }

    public boolean isOpenBlueTooth(){
        //判断蓝牙是否开启
        boolean isOpen=false;
        if(myadapter.isEnabled()){
           isOpen=true;
        }
        return isOpen;
    }

    //打开蓝牙设备
    public void OpenBlueTooth(Activity activity,int requestCode){
        Intent myintent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(myintent,requestCode);//返回值只要大于0即可
    }

    //关闭蓝牙设备
    public void CloseBlueTooth(){
        if(myadapter != null && myadapter.isEnabled()){
            myadapter.disable();
        }
    }

    /**
     * 获取已经配对的设备
     */
    public Set<BluetoothDevice> SearchBlueToothDevices() {
        if (myadapter != null && myadapter.isEnabled()) {
            return myadapter.getBondedDevices();
        }
        return null;
    }

    /**
     * 获取没有配对的设备
     */
    public boolean SearchAvaliableBlueToothDevices(){
        return myadapter.startDiscovery();
    }

    /**
     * 设置设备永久可见
     */
    public void SetDeviceVisiable(Activity activity){
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        //定义持续时间
        //EXTRA_DISCOVERABLE_DURATION附加字段，可以定义不同持续时间。应用程序能够设置的最大持续时间是3600秒，0意味着设备始终是可发现的。任何小于0或大于3600秒的值都会自动的被设为120秒
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
        activity.startActivity(discoverableIntent);
    }

    /**
     * 设置设备不可见
     */
    public void SetDeviceDisVisible(){
            BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();
            try {
                Method setDiscoverableTimeout = BluetoothAdapter.class.getMethod("setDiscoverableTimeout", int.class);
                setDiscoverableTimeout.setAccessible(true);
                Method setScanMode =BluetoothAdapter.class.getMethod("setScanMode", int.class,int.class);
                setScanMode.setAccessible(true);
                setDiscoverableTimeout.invoke(adapter, 1);
                setScanMode.invoke(adapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE,1);
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    /**
     * 设备间通信(请求端)
     */
    public void send(String uuid,BluetoothDevice selectDevice,Activity activity) {
        // 这里需要try catch一下，以防异常抛出
        try {
            // 判断客户端接口是否为空
            if (clientSocket == null) {
                // 获取到客户端接口
//                clientSocket = selectDevice.createRfcommSocketToServiceRecord(UUID.fromString(uuid));
//                clientSocket =(BluetoothSocket) selectDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(selectDevice,1);
                int sdk = Integer.parseInt(Build.VERSION.SDK);
                if (sdk >= 10) {
                    clientSocket = selectDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(uuid));
                } else {
                    clientSocket = selectDevice.createRfcommSocketToServiceRecord(UUID.fromString(uuid));
                }

            }
            // 向服务端发送连接
            if(!clientSocket.isConnected())
            clientSocket.connect();
            // 获取到输出流，向外写数据
            os = clientSocket.getOutputStream();
            Log.d("zhangrui","获取输出流");
            // 判断是否拿到输出流
            if (os != null) {
                Log.d("zhangrui","输出流不为空");
                // 需要发送的信息
                String text = "成功发送信息";
                 //以utf-8的格式发送出去
                os.write(text.getBytes("UTF-8"));
//                File file = new File("sdcard"+File.separator+"DCIM"+File.separator+"Camera"+File.separator+"gg.jpg");
//                if(!file.exists()){
//                    Log.d("zhangrui","要读取的文件不存在");
//                }
//                FileInputStream fileInputStream = new FileInputStream(file);
//                int len = 0;
//                byte[] buf = new byte[1024];
//                while((len=fileInputStream.read(buf))!=-1){
//                    Log.d("zhangrui",new String(buf,0,len));
//                    os.write(buf);
//                }

            }
            Log.d("zhangrui","发送信息成功，请查收");
        } catch (Exception e) {
            Log.d("zhangrui", "发送信息失败="+e.getMessage());
        }

    }

    //取消配对
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void unPairAllDevices() {
        Log.i("TAG", "unPairAllDevices");
        for (BluetoothDevice device : myadapter.getBondedDevices()) {
            try {
                Method removeBond = device.getClass().getDeclaredMethod("removeBond");
                removeBond.invoke(device);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    //断开蓝牙连接
    public void interrupConnect(){
        if(myadapter != null)
        myadapter.disable();
    }

}
