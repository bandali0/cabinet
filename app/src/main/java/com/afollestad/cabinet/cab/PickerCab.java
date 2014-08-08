package com.afollestad.cabinet.cab;

import android.view.ActionMode;

import com.afollestad.cabinet.R;
import com.afollestad.cabinet.cab.base.BaseCab;

public class PickerCab extends BaseCab {

    @Override
    public int getMenu() {
        return -1;
    }

    @Override
    public CharSequence getTitle() {
        return getContext().getString(R.string.pick_a_file);
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        getContext().pickMode = false;
        super.onDestroyActionMode(actionMode);
    }
}