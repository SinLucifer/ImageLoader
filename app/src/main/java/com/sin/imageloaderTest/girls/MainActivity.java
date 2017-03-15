package com.sin.imageloaderTest.girls;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.jude.easyrecyclerview.EasyRecyclerView;
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter;
import com.sin.imageloaderTest.R;
import com.sin.imageloaderTest.data.Girls;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements GirlsContract.View,
        RecyclerArrayAdapter.OnMoreListener, SwipeRefreshLayout.OnRefreshListener{
    private static final String TAG = "MainActivity";

    private ArrayList<Girls.ResultsBean> girlList;
    private GirlsAdapter imageAdapter;
    private GirlsPresenter presenter;

    private int page = 1;
    private int count = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        girlList = new ArrayList<>();
        imageAdapter = new GirlsAdapter(this);
        imageAdapter.setWifi(true);

        final EasyRecyclerView loadMoreRecycleView = (EasyRecyclerView)findViewById(R.id.load_more_recycleView);
        GridLayoutManager linearLayoutManager =
                new GridLayoutManager(this,2);
        loadMoreRecycleView.setLayoutManager(linearLayoutManager);
        loadMoreRecycleView.setAdapterWithProgress(imageAdapter);
        loadMoreRecycleView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE){
                    imageAdapter.setIdle(true);
                    imageAdapter.notifyDataSetChanged();
                }else {
                    imageAdapter.setIdle(false);
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        loadMoreRecycleView.setRefreshListener(this);
        imageAdapter.setMore(R.layout.load_more_layout,this);
        imageAdapter.setError(R.layout.error_layout);

        presenter = new GirlsPresenter(this);
        presenter.start();
    }

    @Override
    public void refresh(ArrayList<Girls.ResultsBean> resultsBeen) {
        girlList.clear();
        girlList.addAll(resultsBeen);
        imageAdapter.clear();
        imageAdapter.addAll(resultsBeen);
    }

    @Override
    public void load(ArrayList<Girls.ResultsBean> resultsBeen) {
        girlList.addAll(resultsBeen);
        imageAdapter.addAll(resultsBeen);
    }

    @Override
    public void onRefresh() {
        presenter.getGirls(1,count,true);
    }

    @Override
    public void onMoreShow() {
        page++;
        presenter.getGirls(page,count,false);
    }

    @Override
    public void onMoreClick() {

    }
}
