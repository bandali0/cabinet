package com.afollestad.cabinet.ui.base;

import android.app.Activity;
import android.os.Bundle;

import com.afollestad.cabinet.utils.ThemeUtils;

/**
 * @author Aidan Follestad (afollestad)
 */
public class ThemableActivity extends Activity {

    private ThemeUtils mThemeUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mThemeUtils = new ThemeUtils(this);
        setTheme(mThemeUtils.getCurrent());
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mThemeUtils.isChanged()) {
            setTheme(mThemeUtils.getCurrent());
            recreate();
        }
    }
}
