package com.afollestad.cabinet.ui;

import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import android.widget.ListView;

import com.afollestad.cabinet.R;
import com.afollestad.cabinet.fragments.AboutDialog;
import com.afollestad.cabinet.utils.ThemeUtils;

/**
 * @author Aidan Follestad (afollestad)
 */
public class SettingsActivity extends PreferenceActivity {

    private ThemeUtils mThemeUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mThemeUtils = new ThemeUtils(this);
        setTheme(mThemeUtils.getCurrent());
        super.onCreate(savedInstanceState);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        addPreferencesFromResource(R.xml.settings);
        DrawerActivity.setupTransparentTints(this);

        CheckBoxPreference translucentStatusbar = (CheckBoxPreference) findPreference("translucent_statusbar");
        translucentStatusbar.setChecked(ThemeUtils.isTranslucentStatusbar(this));
        translucentStatusbar.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                recreate();
                return true;
            }
        });
        CheckBoxPreference translucentNavbar = (CheckBoxPreference) findPreference("translucent_navbar");
        translucentNavbar.setChecked(ThemeUtils.isTranslucentNavbar(this));
        translucentNavbar.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                recreate();
                return true;
            }
        });

        findPreference("about").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AboutDialog().show(getFragmentManager(), "ABOUT");
                return true;
            }
        });

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            translucentStatusbar.setEnabled(false);
            translucentStatusbar.setSummary(R.string.translucency_not_supported);
            translucentNavbar.setEnabled(false);
            translucentNavbar.setSummary(R.string.translucency_not_supported);
        }
//        else if (Build.VERSION.SDK_INT >= 20) {
//            translucentStatusbar.setEnabled(false);
//            translucentStatusbar.setSummary(R.string.translucentstatusbar_disabled);
//        }
        // TODO toggle comment for else statement for Material

        ListView list = (ListView)findViewById(android.R.id.list);
        list.setClipToPadding(false);
        DrawerActivity.setupTranslucentPadding(this, list);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mThemeUtils.isChanged()) {
            setTheme(mThemeUtils.getCurrent());
            recreate();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}