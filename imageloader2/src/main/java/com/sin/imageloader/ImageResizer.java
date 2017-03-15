package com.sin.imageloader;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.FileDescriptor;

public class ImageResizer {
    public static final String TAG = "ImageResizer";

    public ImageResizer(){}

    public Bitmap decodeSampledBitmapFromResource(Resources res
            , int redId,int reqWidth,int reqHeight){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res,redId,options);

        options.inSampleSize = shapeSize(options,reqWidth,reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res,redId,options);
    }

    public Bitmap decodeSampledBitmapFromFileDescriptor(FileDescriptor fd
            ,int reqWidth,int reqHeight){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fd,null,options);

        options.inSampleSize = shapeSize(options,reqWidth,reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFileDescriptor(fd,null,options);
    }

    private int shapeSize(BitmapFactory.Options options,int reqWidth,int reqHeight){
        int width = options.outWidth;
        int height = options.outHeight;
        int sampleSize = 1;

        while(width > reqWidth || height > reqHeight){
            sampleSize *= 2;
            width /= 2;
            height /= 2;
        }

        return sampleSize;
    }
}
