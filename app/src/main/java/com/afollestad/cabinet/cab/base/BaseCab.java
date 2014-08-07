package com.afollestad.cabinet.cab.base;

import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import com.afollestad.cabinet.fragments.DirectoryFragment;
import com.afollestad.cabinet.ui.DrawerActivity;

import java.io.Serializable;

public abstract class BaseCab implements ActionMode.Callback, Serializable {

    public BaseCab() {
    }

    private transient ActionMode mActionMode;
    private transient DirectoryFragment mContext;

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

    public DrawerActivity getContext() {
        return (DrawerActivity) mContext.getActivity();
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
        if (getMenu() != -1)
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
