package com.afollestad.cabinet;

import android.app.Application;

import com.afollestad.cabinet.utils.APKIconDownloader;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

/**
 * @author Aidan Follestad (afollestad)
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .resetViewBeforeLoading(true)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .showImageForEmptyUri(R.drawable.ic_image)
                .showImageOnFail(R.drawable.ic_image)
                .build();
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
                .defaultDisplayImageOptions(options)
                .imageDownloader(new APKIconDownloader(this))
                .build();
        ImageLoader.getInstance().init(config);
    }
}
