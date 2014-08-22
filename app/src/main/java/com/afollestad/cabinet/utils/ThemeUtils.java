package com.afollestad.cabinet.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.afollestad.cabinet.R;

public class ThemeUtils {

    public ThemeUtils(Activity context, boolean allowTranslucentNavbar) {
        mContext = context;
        this.allowTranslucentNavbar = allowTranslucentNavbar;
        isChanged(); // invalidate stored booleans
    }

    private Context mContext;
    private boolean darkMode;
    private boolean trueBlack;
    private boolean translucentStatusbar;
    private boolean translucentNavbar;
    private boolean allowTranslucentNavbar;

    public static boolean isDarkMode(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("dark_mode", false);
    }

    public static boolean isTrueBlack(Context context) {
        if (!isDarkMode(context)) return false;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("true_black", false);
    }

    public static boolean isTranslucentStatusbar(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            return false; // always disabled below KitKat
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("translucent_statusbar", true);
    }

    public static boolean isTranslucentNavbar(Context context) {
        if (Build.VERSION.SDK_INT >= 20 || Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            return false; // always disabled on L+ or below KitKat
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("translucent_navbar", Build.VERSION.SDK_INT < 20);
    }

    public boolean isChanged() {
        boolean darkTheme = isDarkMode(mContext);
        boolean blackTheme = isTrueBlack(mContext);
        boolean statusTrans = isTranslucentStatusbar(mContext);
        boolean navTrans = allowTranslucentNavbar && isTranslucentNavbar(mContext);
        boolean changed = darkMode != darkTheme || statusTrans != translucentStatusbar || navTrans != translucentNavbar || trueBlack != blackTheme;
        darkMode = darkTheme;
        trueBlack = blackTheme;
        translucentStatusbar = statusTrans;
        translucentNavbar = navTrans;
        return changed;
    }

    public int getCurrent() {
//        if (translucentStatusbar && translucentNavbar) {
//            if (trueBlack) return R.style.Theme_CabinetTrueBlackNavStatusTranslucent;
//            else if (darkMode) return R.style.Theme_CabinetDarkNavStatusTranslucent;
//            else return R.style.Theme_CabinetNavStatusTranslucent;
//        } else if (translucentStatusbar) {
//            if (trueBlack) return R.style.Theme_CabinetTrueBlackStatusTranslucent;
//            else if (darkMode) return R.style.Theme_CabinetDarkStatusTranslucent;
//            else return R.style.Theme_CabinetStatusTranslucent;
//        } else if (translucentNavbar) {
//            if (trueBlack) return R.style.Theme_CabinetTrueBlackNavTranslucent;
//            else if (darkMode) return R.style.Theme_CabinetDarkNavTranslucent;
//            else return R.style.Theme_CabinetNavTranslucent;
//        } else {
//            if (trueBlack) return R.style.Theme_CabinetTrueBlack;
//            else if (darkMode) return R.style.Theme_CabinetDark;
//            else return R.style.Theme_Cabinet;
//        }
        // TODO toggle commented area for Material
        if (Build.VERSION.SDK_INT >= 20) {
            if (translucentNavbar) {
                if (trueBlack) {
                    Log.v("ThemeUtils", "Using Theme_CabinetTrueBlackNavTranslucent");
                    return R.style.Theme_CabinetTrueBlackNavTranslucent;
                } else if (darkMode) {
                    Log.v("ThemeUtils", "Using Theme_CabinetDarkNavTranslucent");
                    return R.style.Theme_CabinetDarkNavTranslucent;
                } else {
                    Log.v("ThemeUtils", "Using Theme_CabinetNavTranslucent");
                    return R.style.Theme_CabinetNavTranslucent;
                }
            } else {
                if (trueBlack) {
                    Log.v("ThemeUtils", "Using Theme_CabinetTrueBlack");
                    return R.style.Theme_CabinetTrueBlack;
                } else if (darkMode) {
                    Log.v("ThemeUtils", "Using Theme_CabinetDark");
                    return R.style.Theme_CabinetDark;
                } else {
                    Log.v("ThemeUtils", "Using Theme_Cabinet");
                    return R.style.Theme_Cabinet;
                }
            }
        } else {
            if (translucentStatusbar && translucentNavbar) {
                if (trueBlack) return R.style.Theme_CabinetTrueBlackNavStatusTranslucent;
                else if (darkMode) return R.style.Theme_CabinetDarkNavStatusTranslucent;
                else return R.style.Theme_CabinetNavStatusTranslucent;
            } else if (translucentStatusbar) {
                if (trueBlack) return R.style.Theme_CabinetTrueBlackStatusTranslucent;
                else if (darkMode) return R.style.Theme_CabinetDarkStatusTranslucent;
                else return R.style.Theme_CabinetStatusTranslucent;
            } else if (translucentNavbar) {
                if (trueBlack) return R.style.Theme_CabinetTrueBlackNavTranslucent;
                else if (darkMode) return R.style.Theme_CabinetDarkNavTranslucent;
                else return R.style.Theme_CabinetNavTranslucent;
            } else {
                if (trueBlack) return R.style.Theme_CabinetTrueBlack;
                else if (darkMode) return R.style.Theme_CabinetDark;
                else return R.style.Theme_Cabinet;
            }
        }
    }
}
