package com.sin.imageloaderTest.data.getter;

import com.sin.imageloaderTest.data.Girls;
import com.sin.imageloaderTest.girls.GirlsServer;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Sin on 2017/3/12.
 */

public class GirlsGetter {
    private static final String TAG = "GirlsGetter";
    public static final String BASE_URL = "http://gank.io/";
    private GirlsServer girlServer;
    private Retrofit retrofit;
    private static final int DEFAULT_TIMEOUT = 5;


    private GirlsGetter(){

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);

        retrofit = new Retrofit.Builder()
                .client(httpClient.build())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .baseUrl(BASE_URL)
                .build();

        girlServer = retrofit.create(GirlsServer.class);
    }

    private static class SingleInstance{
        private static final GirlsGetter INSTANCE = new GirlsGetter();
    }


    public static GirlsGetter getInstance() {
        return SingleInstance.INSTANCE;
    }

    private void getGirls(Subscriber<Girls> subscriber, int page, int size){
        girlServer.getGirls("福利",size,page)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(subscriber);
    }

    public void load(final int page, int count, final CallBack callBack){

        Subscriber<Girls> subscriber = new Subscriber<Girls>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                callBack.loadError();
            }

            @Override
            public void onNext(Girls girls) {
                UrlCache.getInstance().save(girls,page);
                callBack.loadComplete((ArrayList<Girls.ResultsBean>) girls.getResults());
            }
        };

        GirlsGetter.getInstance().getGirls(subscriber,page,count);
    }

    public interface CallBack{
        void loadComplete(ArrayList<Girls.ResultsBean> girls);
        void loadError();
    }
}
