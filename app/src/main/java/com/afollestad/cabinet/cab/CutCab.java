package com.afollestad.cabinet.cab;

import android.text.Html;
import android.text.Spanned;
import android.view.ActionMode;
import android.view.MenuItem;

import com.afollestad.cabinet.R;
import com.afollestad.cabinet.cab.base.BaseFileCab;
import com.afollestad.cabinet.file.CloudFile;
import com.afollestad.cabinet.file.LocalFile;
import com.afollestad.cabinet.file.base.File;
import com.afollestad.cabinet.sftp.SftpClient;

public class CutCab extends BaseFileCab {

    public CutCab() {
        super();
    }

    @Override
    public Spanned getTitle() {
        if (getFiles().size() == 1)
            return Html.fromHtml(getContext().getString(R.string.cut_x, getFiles().get(0).getName()));
        return Html.fromHtml(getContext().getString(R.string.cut_xfiles, getFiles().size()));
    }

    private boolean shouldCancel;

    @Override
    public void paste() {
        for (final File file : getFiles()) {
            if (shouldCancel) break;
            File newFile = getDirectory().isRemote() ?
                    new CloudFile(getContext(), (CloudFile) getDirectory(), file.getName(), file.isDirectory()) :
                    new LocalFile(getContext(), getDirectory(), file.getName());
            file.rename(newFile, new SftpClient.FileCallback() {
                @Override
                public void onComplete(File newFile) {
                    addAdapter(newFile);
                }

                @Override
                public void onError(Exception e) {
                    shouldCancel = true;
                }
            });
        }
        finish();
    }

    @Override
    public boolean canPaste() {
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        return false;
    }
}
