package com.sin.imageloaderTest.data.getter;

import android.util.Log;

import com.google.gson.Gson;
import com.sin.imageloaderTest.data.Girls;
import com.sin.imageloaderTest.util.MyUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by Sin on 2017/3/15.
 */

public class UrlCache {
    private final static String TAG = "UrlCache";

    private File cache;

    private static class SingleInstance {
        private static UrlCache instance = new UrlCache();
    }

    public static UrlCache getInstance() {
        return SingleInstance.instance;
    }

    public interface CallBack{
        void loadComplete(ArrayList<Girls.ResultsBean> girls);
        void loadError();
    }

    private UrlCache(){
        cache = MyUtils.getDiskCacheDir("Cache");

        if (!cache.exists()){
            cache.mkdir();
        }
    }

    public void save(Girls girls,int page){
        Gson gson = new Gson();
        final String value = gson.toJson(girls);
        File file = new File(cache,"page" + page +".json");
        Observable.just(file)
                .flatMap(new Func1<File, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(File file) {
                        return Observable.just(save(file,value));
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }

    private Boolean save(File file,String value){
        FileOutputStream fos = null;

        try {
            if(!file.exists())
                file.createNewFile();
            fos = new FileOutputStream(file,false);
            fos.write(value.getBytes());
        }catch (IOException e){
            Log.e(TAG, "save error",e);
            return false;
        }finally {
            if (fos != null)
                MyUtils.close(fos);
        }

        return true;
    }

    public void load(int page, final CallBack callBack){
        File file = new File(cache,"page" + page +".json");
        Observable.just(file)
                .flatMap(new Func1<File, Observable<Girls>>() {
                    @Override
                    public Observable<Girls> call(File file) {
                        return Observable.just(load(file));
                    }})
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Girls>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        callBack.loadError();
                    }

                    @Override
                    public void onNext(Girls girls) {
                        callBack.loadComplete((ArrayList<Girls.ResultsBean>) girls.getResults());
                    }
                });
    }

    private Girls load(File file){

        FileInputStream fis = null;
        Girls girls = null;
        byte[] value;
        try {
            if (file.exists()) {
                fis = new FileInputStream(file);
                value = new byte[fis.available()];
                fis.read(value);
                Gson gson = new Gson();
                girls = gson.fromJson(new String(value), Girls.class);
            }
        }catch (IOException e){
            Log.e(TAG, "load error",e);
        }finally {
            if (fis != null){
                MyUtils.close(fis);
            }
        }

        return girls;
    }
}
