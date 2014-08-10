package com.afollestad.cabinet.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;

import com.afollestad.cabinet.R;
import com.afollestad.cabinet.utils.ThemeUtils;

public class TextEditor extends Activity {

    private ThemeUtils mThemeUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mThemeUtils = new ThemeUtils(this);
        setTheme(mThemeUtils.getCurrent());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_texteditor);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        DrawerActivity.setupTransparentTints(this);
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
