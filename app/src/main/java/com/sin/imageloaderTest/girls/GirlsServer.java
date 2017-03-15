package com.sin.imageloaderTest.girls;

import com.sin.imageloaderTest.data.Girls;

import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import rx.Observable;

/**
 * Created by Sin on 2017/3/12.
 */

public interface GirlsServer {
    @Headers("Cache-Control: public, max-age=3600")
    @GET("api/data/{type}/{count}/{page}")
    Observable<Girls> getGirls(
            @Path("type") String type,
            @Path("count") int count,
            @Path("page") int page
    );
}
