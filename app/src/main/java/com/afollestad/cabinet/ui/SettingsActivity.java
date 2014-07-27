package com.afollestad.cabinet.ui;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import android.widget.ListView;

import com.afollestad.cabinet.R;

/**
 * @author Aidan Follestad (afollestad)
 */
public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DrawerActivity.setupTransparentTints(this);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        addPreferencesFromResource(R.xml.settings);

        ListView list = (ListView) findViewById(android.R.id.list);
        DrawerActivity.setupTranslucentPadding(this, list);
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
