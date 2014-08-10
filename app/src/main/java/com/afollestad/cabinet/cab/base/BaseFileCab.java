package com.afollestad.cabinet.cab.base;

import android.util.Log;
import android.view.ActionMode;

import com.afollestad.cabinet.file.base.File;
import com.afollestad.cabinet.fragments.DirectoryFragment;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseFileCab extends BaseCab {

    public enum PasteMode {
        ENABLED,
        NOT_AVAILABLE,
        DISABLED
    }

    public BaseFileCab() {
        super();
        mFiles = new ArrayList<File>();
    }

    private File mDirectory;
    private final List<File> mFiles;
    public transient boolean overrideDestroy;

    public abstract void paste();

    public abstract PasteMode canPaste();

    public BaseFileCab invalidateFab() {
        Log.v("Fab", "invalidateFab()");
        boolean hide = false;
        Log.v("Fab", "Mode: " + canPaste());
        if (canPaste() != PasteMode.NOT_AVAILABLE) {
            if (isActive() && canPaste() == PasteMode.DISABLED) {
                Log.v("Fab", "Can't paste");
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
        } else Log.v("Fab", "Paste mode not available");
        getContext().disableFab(hide);
        return this;
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

    public final int findFile(File file) {
        for (int i = 0; i < getFiles().size(); i++) {
            if (getFiles().get(i).equals(file)) {
                return i;
            }
        }
        return -1;
    }

    public final void setFile(int index, File file) {
        // Uncheck old file
        getFragment().mAdapter.setItemChecked(getFiles().get(index), false);
        // Replace old file with new one
        getFiles().set(index, file);
        // Check new file
        getFragment().mAdapter.setItemChecked(file, true);
        invalidate();
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
            getFragment().mAdapter.resetChecked();
            if (canPaste() == PasteMode.ENABLED) getContext().setPasteMode(PasteMode.DISABLED);
        }
        super.onDestroyActionMode(actionMode);
    }
}
