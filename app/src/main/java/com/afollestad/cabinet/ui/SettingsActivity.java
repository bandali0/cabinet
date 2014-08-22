package com.afollestad.cabinet.ui;

import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.ListView;

import com.afollestad.cabinet.R;
import com.afollestad.cabinet.fragments.AboutDialog;
import com.afollestad.cabinet.ui.base.ThemablePreferenceActivity;
import com.afollestad.cabinet.utils.ThemeUtils;
import com.nostra13.universalimageloader.core.ImageLoader;

/**
 * @author Aidan Follestad (afollestad)
 */
public class SettingsActivity extends ThemablePreferenceActivity implements AboutDialog.DismissListener {

    private boolean aboutDialogShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preference_activity_custom);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        DrawerActivity.setupTransparentTints(this);
        ListView list = (ListView) findViewById(android.R.id.list);
        DrawerActivity.setupTranslucentTopPadding(this, list);
        DrawerActivity.setupTranslucentBottomPadding(this, list);

        addPreferencesFromResource(R.xml.settings);
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

        // TODO uncomment if statement for Material
        if (Build.VERSION.SDK_INT >= 20) {
            translucentStatusbar.setEnabled(false);
            translucentStatusbar.setSummary(R.string.translucentstatusbar_disabled);
        }

        translucentNavbar.setChecked(ThemeUtils.isTranslucentNavbar(this));
        translucentNavbar.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                recreate();
                return true;
            }
        });

        findPreference("dark_mode").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                ImageLoader.getInstance().clearMemoryCache();
                recreate();
                return true;
            }
        });
        findPreference("true_black").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
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