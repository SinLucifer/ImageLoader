package com.sin.imageloaderTest.girls;

import com.sin.imageloaderTest.data.Girls;
import com.sin.imageloaderTest.girls.GirlsContract;
import com.sin.imageloaderTest.data.getter.GirlsGetter;
import com.sin.imageloaderTest.data.getter.UrlCache;
import com.sin.imageloaderTest.util.MyUtils;

import java.util.ArrayList;

public class GirlsPresenter implements GirlsContract.Presenter {

    private GirlsContract.View mView;

    public GirlsPresenter(GirlsContract.View view){
        mView = view;
    }

    @Override
    public void start() {
        getGirls(1,20,false);
    }

    @Override
    public void getGirls(int page, int size, final boolean isRefresh) {

        if (MyUtils.isWifi()){
            GirlsGetter.getInstance().load(page, size, new GirlsGetter.CallBack() {
                @Override
                public void loadComplete(ArrayList<Girls.ResultsBean> girls) {
                    if (isRefresh){
                        mView.refresh(girls);
                    } else {
                        mView.load(girls);
                    }
                }

                @Override
                public void loadError() {

                }
            });
        }else {
            UrlCache.getInstance().load(page, new UrlCache.CallBack() {
                @Override
                public void loadComplete(ArrayList<Girls.ResultsBean> girls) {
                    mView.load(girls);
                }

                @Override
                public void loadError() {

                }
            });
        }
    }
}
