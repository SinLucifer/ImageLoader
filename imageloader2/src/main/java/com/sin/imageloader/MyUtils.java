package com.sin.imageloader;

import android.util.Log;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by Sin on 2017/3/9.
 */

public class MyUtils {
    private static final String TAG = "MyUtils";

    protected static void close(Closeable cl) {
        try{
            if (cl != null)
                cl.close();
        }catch (IOException e){
            Log.e(TAG, "close error ", e);
        }
    }
}
