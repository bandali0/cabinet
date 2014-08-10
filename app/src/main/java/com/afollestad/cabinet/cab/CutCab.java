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

    private transient boolean shouldCancel;
    private transient int cutCount;
    private transient int cutTOtal;

    @Override
    public void paste() {
        final ProgressDialog mDialog = new ProgressDialog(getContext());
        mDialog.setMessage(getContext().getString(R.string.copying));
        if (getFiles().size() > 1) {
            mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mDialog.setMax(getFiles().size());
        } else mDialog.setIndeterminate(true);
        mDialog.show();
        cutCount = 0;
        cutTOtal = getFiles().size();
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (final File file : getFiles()) {
                    if (shouldCancel) break;
                    final File newFile = getDirectory().isRemote() ?
                            new CloudFile(getContext(), (CloudFile) getDirectory(), file.getName(), file.isDirectory()) :
                            new LocalFile(getContext(), getDirectory(), file.getName());
                    file.rename(newFile, new SftpClient.CompletionCallback() {
                        @Override
                        public void onComplete() {
                            getFragment().reload();
                            if (getFiles().size() > 0)
                                mDialog.setProgress(mDialog.getProgress() + 1);
                            cutCount++;
                            if (cutCount == cutTOtal) {
                                if (getDirectory().isRemote()) {
                                    Toast.makeText(getContext(), getContext().getString(R.string.uploaded_to, getDirectory().getPath()), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getContext(), getContext().getString(R.string.moved_to, newFile.getPath()), Toast.LENGTH_SHORT).show();
                                }
                            }
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
    public PasteMode canPaste() {
        return isActive() ? PasteMode.ENABLED : PasteMode.DISABLED;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        return false;
    }
}
