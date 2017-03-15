package com.sin.imageloaderTest.util;

import android.app.Application;

/**
 * Created by Sin on 2017/3/14.
 */

public class MyApplication extends Application{

    private static MyApplication instance;

    public static MyApplication getInstance(){
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}
