package mykotlin.cn.com.mytestdemo;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Created by topwise on 17-12-12.
 */

public class BluetoothService extends Service {
    //服务端
    private final String NAME = "MyTestDemo";
    private final String MYUUID = "00001101-0000-1000-8000-00805F9B34FB";
    private BluetoothServerSocket serverSocket;
    private BluetoothSocket socket;
    private InputStream is;//输入流

    private BluetoothBinder bluetoothBinder;

    @Override
    public void onCreate() {
        super.onCreate();
        bluetoothBinder=new BluetoothBinder();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return bluetoothBinder;
    }

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Toast.makeText(getApplicationContext(), String.valueOf(msg.obj),
                    Toast.LENGTH_LONG).show();
        }
    };

    public class BluetoothBinder extends Binder {

        public void init(BluetoothAdapter bluetoothAdapter) {
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, UUID.fromString(MYUUID));
            } catch (Exception e) {
            }
        }
        public void doMyWork(){
            try {
                socket = serverSocket.accept();
                is = socket.getInputStream();
                while(true) {
                    byte[] buffer =new byte[1024];
                    int count = is.read(buffer);
                    Message msg = new Message();
                    msg.obj = new String(buffer, 0, count, "utf-8");
                    handler.sendMessage(msg);
                }
            }
            catch (Exception e) {
                try{
                    socket.close();
                } catch (IOException closeException) { }
            }
        }
    }
}
