package mykotlin.cn.com.mytestdemo.utils;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by topwise on 17-12-19.
 */

public class FileUtils {
    /**
     * 根据byte数组生成文件
     *
     * @param bytes
     *            生成文件用到的byte数组
     */
    public void createFileWithByte(byte[] bytes){
        /**
         * 创建File对象，其中包含文件所在的目录以及文件的命名
         */
        File file = new File("sdcard"+File.separator+"DCIM"+File.separator+"Camera"+File.separator+"gg.jpg");
        // 创建FileOutputStream对象
        FileOutputStream outputStream = null;
        // 创建BufferedOutputStream对象
        BufferedOutputStream bufferedOutputStream = null;
        try {
            // 如果文件存在则删除
            if (file.exists()){
                Log.d("zhangrui","文件已存在");
                file.delete();
            }
            // 在文件系统中根据路径创建一个新的空文件
            file.createNewFile();
            // 获取FileOutputStream对象
            outputStream = new FileOutputStream(file);
            Log.d("zhangrui","文件已创建");
            // 获取BufferedOutputStream对象
            bufferedOutputStream = new BufferedOutputStream(outputStream);
            // 往文件所在的缓冲输出流中写byte数据
            bufferedOutputStream.write(bytes);
            // 刷出缓冲输出流，该步很关键，要是不执行flush()方法，那么文件的内容是空的。
            bufferedOutputStream.flush();
        } catch (Exception e){
            // 打印异常信息
            e.printStackTrace();
        } finally {
            // 关闭创建的流对象
            if (outputStream != null){
                try {
                    outputStream.close();
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
            if (bufferedOutputStream != null){
                try {
                    bufferedOutputStream.close();
                } catch (Exception e2){
                    e2.printStackTrace();
                }
            }
        }
    }
}
