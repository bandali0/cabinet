package com.afollestad.cabinet.cab;

import android.app.ProgressDialog;
import android.text.Html;
import android.text.Spanned;
import android.view.ActionMode;
import android.view.MenuItem;
import android.widget.Toast;

import com.afollestad.cabinet.R;
import com.afollestad.cabinet.cab.base.BaseFileCab;
import com.afollestad.cabinet.file.CloudFile;
import com.afollestad.cabinet.file.LocalFile;
import com.afollestad.cabinet.file.base.File;
import com.afollestad.cabinet.sftp.SftpClient;
import com.afollestad.cabinet.utils.Utils;

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

    private transient boolean shouldCancel;
    private transient int copyCount;
    private transient int copyTotal;

    @Override
    public boolean canShowFab() {
        return true;
    }

    @Override
    public void paste() {
        Utils.lockOrientation(getContext());
        final ProgressDialog mDialog = new ProgressDialog(getContext());
        mDialog.setMessage(getContext().getString(R.string.copying));
        if (getFiles().size() > 1) {
            mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mDialog.setMax(getFiles().size());
        } else mDialog.setIndeterminate(true);
        mDialog.show();
        copyCount = 0;
        copyTotal = getFiles().size();
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
                            copyCount++;
                            if (copyCount == copyTotal) {
                                Utils.unlockOrientation(getContext());
                                if (getDirectory().isRemote()) {
                                    Toast.makeText(getContext(), getContext().getString(R.string.uploaded_to, getDirectory().getPath()), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getContext(), getContext().getString(R.string.copied_to, getDirectory().getPath()), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            Utils.unlockOrientation(getContext());
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
    public PasteMode canPaste() {
        return isActive() ? PasteMode.ENABLED : PasteMode.DISABLED;
    }

    @Override
    public boolean canPasteIntoSameDir() {
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        return false;
    }
}
