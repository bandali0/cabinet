package com.afollestad.cabinet.ui;

import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import com.afollestad.cabinet.R;
import com.afollestad.cabinet.fragments.AboutDialog;
import com.afollestad.cabinet.utils.ThemeUtils;

/**
 * @author Aidan Follestad (afollestad)
 */
public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        addPreferencesFromResource(R.xml.settings);

        CheckBoxPreference translucentStatusbar = (CheckBoxPreference) findPreference("translucent_statusbar");
        CheckBoxPreference translucentNavbar = (CheckBoxPreference) findPreference("translucent_navbar");
        translucentStatusbar.setChecked(ThemeUtils.isTranslucentStatusbar(this));
        translucentNavbar.setChecked(ThemeUtils.isTranslucentNavbar(this));

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            translucentStatusbar.setEnabled(false);
            translucentStatusbar.setSummary(R.string.translucency_not_supported);
            translucentNavbar.setEnabled(false);
            translucentNavbar.setSummary(R.string.translucency_not_supported);
        } else if (Build.VERSION.SDK_INT >= 20) {
            translucentStatusbar.setEnabled(false);
            translucentStatusbar.setSummary(R.string.translucentstatusbar_disabled);
        }
        // TODO toggle comment for else statement for Material

        findPreference("about").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AboutDialog().show(getFragmentManager(), "ABOUT");
                return true;
            }
        });
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