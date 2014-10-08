package com.afollestad.cabinet;

import android.app.Application;
import android.content.Context;

import com.afollestad.cabinet.utils.APKIconDownloader;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

/**
 * @author Aidan Follestad (afollestad)
 */
public class App extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
                .defaultDisplayImageOptions(getDisplayOptions(0))
                .imageDownloader(new APKIconDownloader(this))
                .build();
        ImageLoader.getInstance().init(config);
        context = getApplicationContext();
    }

    public static DisplayImageOptions getDisplayOptions(int fallback) {
        if (fallback == 0) fallback = R.drawable.ic_file_image;
        return new DisplayImageOptions.Builder()
                .resetViewBeforeLoading(true)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .showImageForEmptyUri(fallback)
                .showImageOnFail(fallback)
                .cacheInMemory(true)
                .cacheOnDisk(false)
                .build();
    }

    public static Context getAppContext() {
        return context;
    }
}
