package com.afollestad.cabinet.cab.base;

import android.app.Activity;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import com.afollestad.cabinet.file.base.File;
import com.afollestad.cabinet.fragments.DirectoryFragment;


public abstract class BaseCab implements ActionMode.Callback {

    public BaseCab() {
    }

    private ActionMode mActionMode;
    private DirectoryFragment mContext;

    public final BaseCab start() {
        getContext().startActionMode(this);
        return this;
    }

    public BaseCab setFragment(DirectoryFragment fragment) {
        mContext = fragment;
        invalidate();
        return this;
    }

    public final boolean isActive() {
        return mActionMode != null;
    }

    public DirectoryFragment getFragment() {
        return mContext;
    }

    public Activity getContext() {
        return mContext.getActivity();
    }

    public void addAdapter(File file) {
        if (file.isRemote()) {
            mContext.reload();
        } else {
            mContext.getAdapter().add(file);
            mContext.resort();
        }
    }

    public abstract int getMenu();

    public abstract CharSequence getTitle();

    public void invalidate() {
        if (mActionMode != null) mActionMode.invalidate();
    }

    public final void finish() {
        if (mActionMode != null) {
            mActionMode.finish();
            mActionMode = null;
        }
    }

    @Override
    public final boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        mActionMode = actionMode;
        actionMode.getMenuInflater().inflate(getMenu(), menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        actionMode.setTitle(getTitle());
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        finish();
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        mActionMode = null;
    }
}
