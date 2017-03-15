package com.sin.imageloader;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageLoader {
    public static final String TAG = "ImageLoader";

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAX_POOL_SIZE = CPU_COUNT + 2;
    private static final long KEEP_ALIVE = 10L;

    private static final int DISK_CACHE_INDEX = 0;
    private static final long DISK_CACHE_SIZE = 1024*1024*50;
    private static final int IO_BUFFER_SIZE = 8 * 1024;
    private static final int TAG_KEY_URI = R.id.tag_key;
    private static boolean isDiskLruCacheCreated = false;

    private Context mContext;

    private static final int MESSAGE_OBATIN = 1;

    private String dir;

    private LruCache<String,Bitmap> mMemoryCache;
    private DiskLruCache mDiskLruCache;
    private static ImageLoader mImageLoader;
    private ImageResizer imageResizer = new ImageResizer();

    private static MyHandler mHandler = new MyHandler(Looper.getMainLooper());

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(@NonNull Runnable r) {
            return new Thread(r,"ImageLoader#" + mCount.getAndIncrement());
        }
    };

    private static final Executor THREAD_POOL_EXECUTOR =
            new ThreadPoolExecutor(CORE_POOL_SIZE,MAX_POOL_SIZE
                    ,KEEP_ALIVE, TimeUnit.SECONDS
                    ,new LinkedBlockingQueue<Runnable>(),sThreadFactory);

    public static ImageLoader build(Context c){
        if (mImageLoader == null)
            mImageLoader = new ImageLoader(c);
        return mImageLoader;
    }

    private ImageLoader(Context c){
        this.mContext = c.getApplicationContext();
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheMemory = maxMemory / 8;
        mMemoryCache = new LruCache<String,Bitmap>(cacheMemory){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getHeight() * value.getRowBytes() / 1024;
            }
        };

        File diskCacheDir = getDiskCacheDir(mContext,"bitmap");
        
        if (!diskCacheDir.exists()){
            diskCacheDir.mkdirs();
        }

        if (getUsableSpace(diskCacheDir) > DISK_CACHE_SIZE){
            try{
                mDiskLruCache = mDiskLruCache.open(diskCacheDir,1,1,DISK_CACHE_SIZE);
                isDiskLruCacheCreated = true;
            }catch (IOException e){
                Log.e(TAG, "DiskLruCache open error:  ",e);
            }
        }
    }

    public Bitmap loadBitmap(String url,int reqWidth,int reqHeight){
        Bitmap bitmap = loadBitmapFromMemoryCache(url);
        if (bitmap != null){
            Log.i(TAG, "loadBitmapFromMemory, url= " + url);
            return bitmap;
        }

        try {
            bitmap = loadBitmapFromDiskCache(url,reqWidth,reqHeight);
            if (bitmap != null){
                Log.i(TAG, "loadBitmapFromDiskCache, url= " + url);
                return bitmap;
            }
            bitmap = loadBitmapFromHttp(url,reqWidth,reqHeight);
        }catch (Exception e){
            Log.e(TAG, "loadBitmapFromDiskCache error ", e);
        }

        if (bitmap == null && !isDiskLruCacheCreated){
            Log.i(TAG, "DiskLruCache is not Created!");
            bitmap = downloadBitmapFromUrl(url);
        }

        return bitmap;
    }

    public void bindBitmap(final String url, final ImageView imageView
            ,final int reqWidth,final int reqHeight){
        imageView.setTag(TAG_KEY_URI,url);
        Bitmap bitmap = loadBitmapFromMemoryCache(url);
        if (bitmap != null){
            Log.i(TAG, "bindBitmap from memoryCache complete!");
            imageView.setImageBitmap(bitmap);
            return;
        }

        Runnable loadBitmap = new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = loadBitmap(url, reqWidth, reqHeight);
                if (bitmap != null){
                    LoaderResult loader = new LoaderResult(imageView,url,bitmap);
                    mHandler.obtainMessage(MESSAGE_OBATIN,loader).sendToTarget();
                }
            }
        };

        THREAD_POOL_EXECUTOR.execute(loadBitmap);
    }

    private Bitmap loadBitmapFromHttp(String url,int reqWidth,int reqHeight) throws IOException{
        if (Looper.getMainLooper() == Looper.myLooper()){
            throw new RuntimeException("can not visit network from UI Thread.");
        }
        if (mDiskLruCache == null){
            Log.i(TAG, "there are no DiskLruCache");
            return null;
        }

        String key = hashKeyFromUrl(url);
        DiskLruCache.Editor editor = mDiskLruCache.edit(key);


        if (editor != null){
            OutputStream os = editor.newOutputStream(DISK_CACHE_INDEX);
            if(downloadUrlToSteam(url,os)){
                editor.commit();
            }else {
                editor.abort();
            }
        }
        mDiskLruCache.flush();
        return loadBitmapFromDiskCache(url,reqWidth,reqHeight);
    }

    private Bitmap loadBitmapFromDiskCache(String url
            ,int reqWidth,int reqHeight) throws IOException {
        if (Looper.getMainLooper() == Looper.myLooper()){
            throw new RuntimeException("can not visit network from UI Thread.");
        }

        if (mDiskLruCache == null)
            return null;

        Bitmap bitmap = null;
        String key = hashKeyFromUrl(url);
        DiskLruCache.Snapshot snapShot = mDiskLruCache.get(key);
        if (snapShot != null){
            FileInputStream fis = (FileInputStream)snapShot.getInputStream(DISK_CACHE_INDEX);
            FileDescriptor fds = fis.getFD();
            bitmap = imageResizer.decodeSampledBitmapFromFileDescriptor(fds,reqWidth,reqHeight);
            if (bitmap != null)
                addBitmapToMemoryCache(key,bitmap);
        }

        return bitmap;
    }

    private Bitmap loadBitmapFromMemoryCache(String url){
        final String key = hashKeyFromUrl(url);
        return getBitmapFromMemoryCache(key);
    }

    private void addBitmapToMemoryCache(String key,Bitmap bitmap){
        if (getBitmapFromMemoryCache(key) == null){
            mMemoryCache.put(key,bitmap);
        }
    }

    private Bitmap getBitmapFromMemoryCache(String key){
        return mMemoryCache.get(key);
    }

    private Bitmap downloadBitmapFromUrl(String urlString){
        Bitmap bitmap = null;
        HttpURLConnection urlConnection = null;
        BufferedInputStream is = null;

        try{
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            is = new BufferedInputStream(urlConnection.getInputStream());
            bitmap = BitmapFactory.decodeStream(is);
        }catch (IOException e){
            Log.e(TAG, "downloadBitmap error,url: " + urlString, e);
        }finally {
            if (urlConnection != null)
                urlConnection.disconnect();
            MyUtils.close(is);
        }

        return bitmap;
    }

    private boolean downloadUrlToSteam(String urlString,OutputStream os){

        HttpURLConnection connection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;

        try{
            final URL url = new URL(urlString);
            connection = (HttpURLConnection)url.openConnection();
            in = new BufferedInputStream(connection.getInputStream(),IO_BUFFER_SIZE);
            out = new BufferedOutputStream(os,IO_BUFFER_SIZE);
            int b;
            while((b = in.read()) != -1){
                out.write(b);
            }
            return true;
        }catch (IOException e){
            Log.e(TAG, "downloadUrlToSteam error",e );
        }finally {
            if (connection != null)
                connection.disconnect();
            MyUtils.close(in);
            MyUtils.close(out);
        }

        return false;
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private long getUsableSpace(File file){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD){
            return file.getUsableSpace();
        }

        StatFs statFs = new StatFs(file.getPath());
        return statFs.getAvailableBlocksLong()* statFs.getBlockSizeLong();
    }


    private File getDiskCacheDir(Context c, String uniqueName){
        boolean isExternalStorageAvailable = Environment
                .getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        final String cachePath;
        if (isExternalStorageAvailable){
            cachePath = c.getExternalCacheDir().getPath();
        }else{
            cachePath = c.getCacheDir().getPath();
        }

        return new File(cachePath + File.separator + uniqueName);
    }

    private String hashKeyFromUrl(String url){
        String cacheKey;
        try{
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(url.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        }catch (NoSuchAlgorithmException e){
            cacheKey = String.valueOf(url.hashCode());
        }
        return cacheKey;
    }

    private String bytesToHexString(byte[] bytes){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1){
                sb.append("0");
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    private static class LoaderResult {
        ImageView imageView;
        String url;
        Bitmap bitmap;

        LoaderResult(ImageView imageView, String url, Bitmap bitmap) {
            this.imageView = imageView;
            this.url = url;
            this.bitmap = bitmap;
        }
    }

    private static class MyHandler extends Handler{

        MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_OBATIN){
                LoaderResult result = (LoaderResult)msg.obj;
                ImageView imageView = result.imageView;
                String url = (String) imageView.getTag(TAG_KEY_URI);
                if (url.equals(result.url)){
                    imageView.setImageBitmap(result.bitmap);
                } else {
                    Log.i(TAG, "set image bitmap,but uri has been changed, ignored!");
                }
            }
        }
    }
}
