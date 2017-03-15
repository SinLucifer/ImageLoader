package com.sin.imageloaderTest.girls;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.AppCompatImageView;
import android.view.ViewGroup;

import com.jude.easyrecyclerview.adapter.BaseViewHolder;
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter;
import com.sin.imageloader.*;
import com.sin.imageloaderTest.*;
import com.sin.imageloaderTest.R;
import com.sin.imageloaderTest.data.Girls;

public class GirlsAdapter extends RecyclerArrayAdapter<Girls.ResultsBean> {
    private static final String TAG = "GirlsAdapter";

    private Drawable mDefaultDrawable;

    private boolean isGridViewIdle = true;
    private boolean isWifi = false;
    private int mImageWidth = 0;

    public GirlsAdapter(Context context){
        super(context);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            mDefaultDrawable = context.getDrawable(com.sin.imageloaderTest.R.mipmap.ic_launcher);
        }else {
            mDefaultDrawable = context.getResources().getDrawable(R.mipmap.ic_launcher);
        }

        mImageWidth = com.sin.imageloaderTest.util.MyUtils.getScreenWidth() / 3;
    }

    @Override
    public ViewHolder OnCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(parent,R.layout.item);
    }

    @Override
    public void OnBindViewHolder(BaseViewHolder holder, int position) {
        super.OnBindViewHolder(holder, position);

        AppCompatImageView imageView = ((ViewHolder)holder).imageView;
        final String tag = (String)imageView.getTag();
        final String url = getAllData().get(position).getUrl();

        if (!url.equals(tag)){
            imageView.setImageDrawable(mDefaultDrawable);
        }

        if (isGridViewIdle && isWifi){
            imageView.setTag(url);
            ImageLoader.build(getContext().getApplicationContext())
                    .bindBitmap(url,imageView,mImageWidth,mImageWidth);
        }
    }

    public void setWifi(boolean isWifi){
        this.isWifi = isWifi;
    }

    public void setIdle(boolean isIdle){
        this.isGridViewIdle = isIdle;
    }

    class ViewHolder extends BaseViewHolder<Girls.ResultsBean>{
        AppCompatImageView imageView;

        public ViewHolder(ViewGroup parent, @LayoutRes int res) {
            super(parent, res);
            imageView = $(R.id.image);
        }
    }
}
