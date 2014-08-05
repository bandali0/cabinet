package com.afollestad.cabinet.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.afollestad.cabinet.R;

public class ThemeUtils {

    public ThemeUtils(Context context) {
        mContext = context;
        isChanged(); // invalidate stored booleans
    }

    private Context mContext;
    private boolean translucentStatusbar;
    private boolean translucentNavbar;

    private void log(String message) {
        Log.d("ThemeUtils", message);
    }

    public static boolean isTranslucentStatusbar(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("translucent_statusbar", true);
    }

    public static boolean isTranslucentNavbar(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("translucent_navbar", Build.VERSION.SDK_INT < 20);
    }

    public boolean isChanged() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean statusTrans = prefs.getBoolean("translucent_statusbar", true);
        boolean navTrans = prefs.getBoolean("translucent_navbar", Build.VERSION.SDK_INT < 20);
        boolean changed = statusTrans != translucentStatusbar || navTrans != translucentNavbar;
        translucentStatusbar = statusTrans;
        translucentNavbar = navTrans;
        log("Changed? " + changed);
        return changed;
    }

    public int getCurrent() {
        log("Translucent status bar: " + translucentStatusbar + ", nav bar: " + translucentNavbar);
        if (Build.VERSION.SDK_INT >= 20) {
            if (translucentNavbar) {
                log("Material nav translucent");
                return R.style.Theme_CabinetNavTranslucent;
            } else {
                log("Material regular");
                return R.style.Theme_Cabinet;
            }
        } else {
            if (translucentStatusbar && translucentNavbar) {
                return R.style.Theme_CabinetNavStatusTranslucent;
            } else if (translucentStatusbar) {
                return R.style.Theme_CabinetStatusTranslucent;
            } else if (translucentNavbar) {
                return R.style.Theme_CabinetNavTranslucent;
            } else {
                return R.style.Theme_Cabinet;
            }
        }
    }
}
