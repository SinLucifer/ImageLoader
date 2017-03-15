package com.sin.imageloaderTest.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public class MyUtils {
    private static final String TAG = "MyUtils";

    public static void close(Closeable cl) {
        try{
            if (cl != null)
                cl.close();
        }catch (IOException e){
            Log.e(TAG, "close error ", e);
        }
    }

    public static boolean isWifi(){
        ConnectivityManager connectivityManager = (ConnectivityManager) MyApplication.getInstance()
                .getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.getType() == ConnectivityManager.TYPE_WIFI){
            return true;
        } else {
            return false;
        }
    }

    public static int getScreenWidth(){
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) MyApplication.getInstance()
                .getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        return metrics.widthPixels;
    }

    public static File getDiskCacheDir( String uniqueName){
        boolean isExternalStorageAvailable = Environment
                .getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        final String cachePath;
        if (isExternalStorageAvailable){
            cachePath = MyApplication.getInstance().getExternalCacheDir().getPath();
        }else{
            cachePath = MyApplication.getInstance().getCacheDir().getPath();
        }

        return new File(cachePath + File.separator + uniqueName);
    }
}
