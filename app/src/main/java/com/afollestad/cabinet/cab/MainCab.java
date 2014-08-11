package com.afollestad.cabinet.cab;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.afollestad.cabinet.R;
import com.afollestad.cabinet.cab.base.BaseFileCab;
import com.afollestad.cabinet.file.base.File;
import com.afollestad.cabinet.fragments.CustomDialog;
import com.afollestad.cabinet.sftp.SftpClient;
import com.afollestad.cabinet.zip.Unzipper;
import com.afollestad.cabinet.zip.Zipper;

import java.util.ArrayList;
import java.util.List;

public class MainCab extends BaseFileCab {

    public MainCab() {
        super();
    }

    @Override
    public void paste() {
        // Not implemented for the main cab
    }

    @Override
    public boolean canShowFab() {
        return false;
    }

    @Override
    public PasteMode canPaste() {
        return PasteMode.NOT_AVAILABLE;
    }

    @Override
    public boolean canPasteIntoSameDir() {
        return false;
    }

    @Override
    public CharSequence getTitle() {
        if (getFiles().size() == 1)
            return getFiles().get(0).getName();
        return getContext().getString(R.string.x_files, getFiles().size());
    }

    @Override
    public final int getMenu() {
        return R.menu.main_cab;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        boolean showUnzip = true;
        boolean showShare = true;
        for (File fi : getFiles()) {
            if (fi.isDirectory()) {
                showShare = false;
                showUnzip = false;
            } else if (!fi.getExtension().equals("zip")) {
                showUnzip = false;
            }
        }
        menu.findItem(R.id.zip).setTitle(showUnzip ? R.string.unzip : R.string.zip);
        menu.findItem(R.id.share).setVisible(showShare);
        return super.onPrepareActionMode(actionMode, menu);
    }

    @Override
    public boolean onActionItemClicked(final ActionMode actionMode, final MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.copy) {
            if (getContext().getCab() instanceof BaseFileCab)
                ((BaseFileCab) getContext().getCab()).overrideDestroy = true;
            getContext().setCab(new CopyCab()
                    .setFragment(getFragment()).setFiles(getFiles()).start());
            return super.onActionItemClicked(actionMode, menuItem);
        } else if (menuItem.getItemId() == R.id.cut) {
            if (getContext().getCab() instanceof BaseFileCab)
                ((BaseFileCab) getContext().getCab()).overrideDestroy = true;
            getContext().setCab(new CutCab()
                    .setFragment(getFragment()).setFiles(getFiles()).start());
            return super.onActionItemClicked(actionMode, menuItem);
        } else if (menuItem.getItemId() == R.id.delete) {
            CustomDialog.create(getContext(), R.string.delete, getFiles().size() == 1 ?
                            getContext().getString(R.string.confirm_delete, getFiles().get(0).getName()) :
                            getContext().getString(R.string.confirm_delete_xfiles, getFiles().size()), R.string.yes, 0, R.string.no,
                    new CustomDialog.SimpleClickListener() {
                        @Override
                        public void onPositive(int which, View view) {
                            deleteNextFile();
                            finish();
                        }
                    }
            ).show(getContext().getFragmentManager(), "DELETE_CONFIRM");
            return false;
        } else if (menuItem.getItemId() == R.id.selectAll) {
            List<File> newSelected = getFragment().mAdapter.checkAll();
            addFiles(newSelected);
            invalidate();
            return true;
        } else if (menuItem.getItemId() == R.id.share) {
            Intent intent = new Intent().setAction(Intent.ACTION_SEND_MULTIPLE);
            String mime = null;
            for (File fi : getFiles()) {
                if (mime == null) mime = fi.getMimeType();
                else if (!fi.getMimeType().equals(mime)) {
                    mime = "*/*";
                    break;
                }
            }
            intent.setType(mime);
            ArrayList<Uri> files = new ArrayList<Uri>();
            for (File fi : getFiles())
                files.add(Uri.fromFile(fi.toJavaFile()));
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
            try {
                getContext().startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(getContext(), R.string.no_apps_for_sharing, Toast.LENGTH_SHORT).show();
            }
        } else if (menuItem.getItemId() == R.id.zip) {
            if (menuItem.getTitle().toString().equals(getContext().getString(R.string.unzip))) {
                Unzipper.unzip(getFragment(), getFiles(), new Zipper.ZipCallback() {
                    @Override
                    public void onComplete() {
                        MainCab.super.onActionItemClicked(actionMode, menuItem);
                    }
                });
            } else {
                Zipper.zip(getFragment(), getFiles(), new Zipper.ZipCallback() {
                    @Override
                    public void onComplete() {
                        MainCab.super.onActionItemClicked(actionMode, menuItem);
                    }
                });
            }
            return true;
        }
        return false;
    }

    private void deleteNextFile() {
        if (getFiles().size() == 0) {
            getFragment().setListShown(true); // invalidates empty text
            invalidate();
            return;
        }
        getFiles().get(0).delete(new SftpClient.CompletionCallback() {
            @Override
            public void onComplete() {
                getFragment().mAdapter.remove(getFiles().get(0));
                getFiles().remove(0);
                deleteNextFile();
            }

            @Override
            public void onError(Exception e) {
                // Nothing is needed here
            }
        });
    }
}
