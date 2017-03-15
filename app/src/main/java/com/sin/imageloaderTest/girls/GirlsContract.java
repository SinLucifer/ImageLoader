package com.sin.imageloaderTest.girls;

import com.sin.imageloaderTest.data.Girls;

import java.util.ArrayList;

/**
 * Created by Sin on 2017/3/14.
 */

public interface GirlsContract {

    interface View {
        void refresh(ArrayList<Girls.ResultsBean> resultsBeen);
        void load(ArrayList<Girls.ResultsBean> resultsBeen);
    }

    interface Presenter{
        void start();
        void getGirls(int page,int size,boolean isRefresh);
    }
}
