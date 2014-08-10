package com.afollestad.cabinet.ui;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import com.afollestad.cabinet.R;
import com.afollestad.cabinet.fragments.AboutDialog;
import com.afollestad.cabinet.utils.ThemeUtils;

/**
 * @author Aidan Follestad (afollestad)
 */
public class SettingsActivity extends PreferenceActivity implements AboutDialog.DismissListener {

    private ThemeUtils mThemeUtils;
    private boolean aboutDialogShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mThemeUtils = new ThemeUtils(this);
        setTheme(mThemeUtils.getCurrent());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preference_activity_custom);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        addPreferencesFromResource(R.xml.settings);
        DrawerActivity.setupTransparentTints(this);

        final CheckBoxPreference translucentStatusbar = (CheckBoxPreference) findPreference("translucent_statusbar");
        final CheckBoxPreference translucentNavbar = (CheckBoxPreference) findPreference("translucent_navbar");

        translucentStatusbar.setChecked(ThemeUtils.isTranslucentStatusbar(this));
        translucentStatusbar.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit()
                        .putBoolean("translucent_navbar", false).commit();
                recreate();
                return true;
            }
        });
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
                if (aboutDialogShown) return false;
                aboutDialogShown = true; // double clicking without this causes the dialog to be shown twice
                new AboutDialog().show(getFragmentManager(), "ABOUT");
                return true;
            }
        });
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

    @Override
    public void onDismiss() {
        aboutDialogShown = false;
    }
}