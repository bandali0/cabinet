package com.afollestad.cabinet.cab;

import android.app.ProgressDialog;
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

public class CopyCab extends BaseFileCab {

    public CopyCab() {
        super();
    }

    @Override
    public Spanned getTitle() {
        if (getFiles().size() == 1)
            return Html.fromHtml(getContext().getString(R.string.copy_x, getFiles().get(0).getName()));
        return Html.fromHtml(getContext().getString(R.string.copy_xfiles, getFiles().size()));
    }

    private boolean shouldCancel;

    @Override
    public void paste() {
        final ProgressDialog mDialog = new ProgressDialog(getContext());
        mDialog.setMessage(getContext().getString(R.string.copying));
        if (getFiles().size() > 1) {
            mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mDialog.setMax(getFiles().size());
        } else mDialog.setIndeterminate(true);
        mDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (final File file : getFiles()) {
                    if (shouldCancel) break;
                    File newFile = getDirectory().isRemote() ?
                            new CloudFile(getContext(), (CloudFile) getDirectory(), file.getName(), file.isDirectory()) :
                            new LocalFile(getContext(), getDirectory(), file.getName());
                    file.copy(newFile, new SftpClient.FileCallback() {
                        @Override
                        public void onComplete(File newFile) {
                            getFragment().reload();
                            if (getFiles().size() > 0)
                                mDialog.setProgress(mDialog.getProgress() + 1);
                        }

                        @Override
                        public void onError(Exception e) {
                            shouldCancel = true;
                        }
                    });
                }
                getContext().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDialog.dismiss();
                        finish();
                    }
                });
            }
        }).start();
    }

    @Override
    public boolean canPaste() {
        return isActive();
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        return false;
    }
}
