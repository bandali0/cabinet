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

    private void log(String message) {
        Log.d("File-Cab", message);
    }

    public abstract void paste();

    public abstract boolean canPaste();

    public void invalidateFab() {
        boolean hide = false;
        for (File fi : getFiles()) {
            if (fi.getParent().equals(getDirectory())) {
                hide = true;
                break;
            }
        }
        getFragment().toggleFab(hide);
    }

    @Override
    public BaseFileCab setFragment(DirectoryFragment fragment) {
        log("setFragment: " + fragment);
        mDirectory = fragment.getDirectory();
        super.setFragment(fragment);
        invalidateFab();
        fragment.setPasteMode(canPaste());
        return this;
    }

    public final BaseFileCab addFile(File file) {
        log("Add file: " + file.getPath());
        getFragment().getAdapter().setItemChecked(file, true);
        mFiles.add(file);
        invalidate();
        return this;
    }

    public final BaseFileCab addFiles(List<File> files) {
        log("Add " + files.size() + " files");
        getFragment().getAdapter().setItemsChecked(files, true);
        mFiles.addAll(files);
        invalidate();
        return this;
    }

    public final BaseFileCab removeFile(File file) {
        log("Remove file: " + file.getPath());
        getFragment().getAdapter().setItemChecked(file, false);
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
        log("Set file: " + file.getPath());
        getFragment().getAdapter().resetChecked();
        getFragment().getAdapter().setItemChecked(file, true);
        clearFiles();
        mFiles.add(file);
        invalidate();
        return this;
    }

    public final BaseFileCab setFiles(List<File> files) {
        log("Set " + files.size() + " files");
        getFragment().getAdapter().resetChecked();
        getFragment().getAdapter().setItemsChecked(files, true);
        clearFiles();
        mFiles.addAll(files);
        invalidate();
        return this;
    }

    public final void clearFiles() {
        log("Clear files");
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
        else super.invalidate();
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
            getFragment().getAdapter().resetChecked();
            if (canPaste()) getFragment().setPasteMode(false);
        } else log("Override destroy");
        super.onDestroyActionMode(actionMode);
    }
}
