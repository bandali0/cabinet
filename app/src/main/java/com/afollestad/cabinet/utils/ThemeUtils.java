package com.afollestad.cabinet.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.afollestad.cabinet.R;

public class ThemeUtils {

    public ThemeUtils(Activity context) {
        context.getActionBar().setIcon(R.drawable.ic_file_ab);
        mContext = context;
        isChanged(); // invalidate stored booleans
    }

    private Context mContext;
    private boolean translucentStatusbar;
    private boolean translucentNavbar;

    public static boolean isTranslucentStatusbar(Context context) {
        if (Build.VERSION.SDK_INT >= 20 || Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            return false; // always disabled on L+ or below KitKat
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
        boolean statusTrans = isTranslucentStatusbar(mContext);
        boolean navTrans = isTranslucentNavbar(mContext);
        boolean changed = statusTrans != translucentStatusbar || navTrans != translucentNavbar;
        translucentStatusbar = statusTrans;
        translucentNavbar = navTrans;
        return changed;
    }

    public int getCurrent() {
        if (translucentStatusbar && translucentNavbar) {
            return R.style.Theme_CabinetNavStatusTranslucent;
        } else if (translucentStatusbar) {
            return R.style.Theme_CabinetStatusTranslucent;
        } else if (translucentNavbar) {
            return R.style.Theme_CabinetNavTranslucent;
        } else {
            return R.style.Theme_Cabinet;
        }
        // TODO toggle commented area for Material
//        if (Build.VERSION.SDK_INT >= 20) {
//            if (translucentNavbar) {
//                return R.style.Theme_CabinetNavTranslucent;
//            } else {
//                return R.style.Theme_Cabinet;
//            }
//        } else {
//            if (translucentStatusbar && translucentNavbar) {
//                return R.style.Theme_CabinetNavStatusTranslucent;
//            } else if (translucentStatusbar) {
//                return R.style.Theme_CabinetStatusTranslucent;
//            } else if (translucentNavbar) {
//                return R.style.Theme_CabinetNavTranslucent;
//            } else {
//                return R.style.Theme_Cabinet;
//            }
//        }
    }
}
