package com.afollestad.cabinet.cab.base;

import android.util.Log;
import android.view.ActionMode;

import com.afollestad.cabinet.file.base.File;
import com.afollestad.cabinet.fragments.DirectoryFragment;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseFileCab extends BaseCab {

    public BaseFileCab() {
        super();
        mFiles = new ArrayList<File>();
    }

    private File mDirectory;
    private final List<File> mFiles;
    public boolean overrideDestroy;

    public abstract void paste();

    public abstract boolean canPaste();

    public void invalidateFab() {
        Log.v("Fab", "invalidateFab()");
        boolean hide = false;
        if (!canPaste()) {
            Log.v("Fab", "Can't paste");
            hide = true;
        } else {
            if (getFiles().size() == 0) Log.v("Fab", "No files are in the CAB");
            for (File fi : getFiles()) {
                Log.v("Fab", "Checking if " + fi.getParent().getPath() + " == " + getDirectory().getPath());
                if (fi.getParent().equals(getDirectory())) {
                    Log.v("Fab", "They are equal");
                    hide = true;
                    break;
                }
            }
        }
        if (hide) Log.v("Fab", "Fab is disabled");
        else Log.v("Fab", "Fab is not disabled");
        getContext().disableFab(hide);
    }

    @Override
    public BaseFileCab setFragment(DirectoryFragment fragment) {
        mDirectory = fragment.getDirectory();
        super.setFragment(fragment);
        getContext().setPasteMode(canPaste());
        return this;
    }

    public final BaseFileCab addFile(File file) {
        getFragment().mAdapter.setItemChecked(file, true);
        mFiles.add(file);
        invalidate();
        return this;
    }

    public final BaseFileCab addFiles(List<File> files) {
        getFragment().mAdapter.setItemsChecked(files, true);
        mFiles.addAll(files);
        invalidate();
        return this;
    }

    public final BaseFileCab removeFile(File file) {
        getFragment().mAdapter.setItemChecked(file, false);
        for (int i = 0; i < mFiles.size(); i++) {
            if (file.getPath().equals(mFiles.get(i).getPath())) {
                mFiles.remove(i);
                invalidate();
                break;
            }
        }
        return this;
    }

    public final BaseFileCab setFile(File file) {
        getFragment().mAdapter.resetChecked();
        getFragment().mAdapter.setItemChecked(file, true);
        clearFiles();
        mFiles.add(file);
        invalidate();
        return this;
    }

    public final BaseFileCab setFiles(List<File> files) {
        getFragment().mAdapter.resetChecked();
        getFragment().mAdapter.setItemsChecked(files, true);
        clearFiles();
        mFiles.addAll(files);
        invalidate();
        return this;
    }

    public final void clearFiles() {
        mFiles.clear();
    }

    public final boolean containsFile(File file) {
        for (File fi : mFiles) {
            if (fi.equals(file)) return true;
        }
        return false;
    }

    @Override
    public final void invalidate() {
        if (getFiles().size() == 0) finish();
        else {
            invalidateFab();
            super.invalidate();
        }
    }

    public final File getDirectory() {
        return mDirectory;
    }

    public final List<File> getFiles() {
        return mFiles;
    }

    @Override
    public int getMenu() {
        return -1;
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        if (!overrideDestroy) {
            clearFiles();
            getFragment().mAdapter.resetChecked();
            if (canPaste()) getContext().setPasteMode(false);
        }
        super.onDestroyActionMode(actionMode);
    }
}
