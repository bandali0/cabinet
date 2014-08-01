package com.afollestad.cabinet.utils;

import android.content.Context;
import android.net.Uri;

import com.nostra13.universalimageloader.core.download.BaseImageDownloader;

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

    public APKIconDownloader(Context context, int connectTimeout, int readTimeout) {
        super(context, connectTimeout, readTimeout);
        mContext = context;
    }

    @Override
    protected InputStream getStreamFromOtherSource(String imageUri, Object extra) throws IOException {
        return mContext.getContentResolver().openInputStream(Uri.parse(imageUri));
    }
}
