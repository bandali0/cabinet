package com.afollestad.cabinet.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.nostra13.universalimageloader.core.download.BaseImageDownloader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Extends Universal Image Loader's image loader in order to support displaying an APK file's icon
 *
 * @author Aidan Follestad
 */
public class APKIconDownloader extends BaseImageDownloader {

    private Context mContext;

    public APKIconDownloader(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    protected InputStream getStreamFromOtherSource(String path, Object extra) throws IOException {
        PackageManager pm = mContext.getPackageManager();
        PackageInfo pi = pm.getPackageArchiveInfo(path, 0);
        pi.applicationInfo.sourceDir = path;
        pi.applicationInfo.publicSourceDir = path;
        Drawable apkIcon = pi.applicationInfo.loadIcon(pm);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ((BitmapDrawable) apkIcon).getBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] imageInByte = stream.toByteArray();
        stream.close();
        return new ByteArrayInputStream(imageInByte);
    }
}
