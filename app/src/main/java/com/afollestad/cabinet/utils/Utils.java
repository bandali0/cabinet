package com.afollestad.cabinet.utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import com.afollestad.cabinet.R;
import com.afollestad.cabinet.file.CloudFile;
import com.afollestad.cabinet.file.LocalFile;
import com.afollestad.cabinet.file.base.File;
import com.afollestad.cabinet.fragments.DirectoryFragment;
import com.afollestad.cabinet.services.NetworkService;
import com.afollestad.cabinet.sftp.SftpClient;
import com.afollestad.cabinet.ui.DrawerActivity;
import com.afollestad.silk.dialogs.SilkDialog;

public class Utils {

    public static interface DuplicateCheckResult {
        public abstract void onResult(File file);
    }

    public static void checkDuplicates(final Activity context, final File file, final DuplicateCheckResult callback) {
        checkDuplicates(context, file, file.getNameNoExtension(), 0, callback);
    }

    private static void checkDuplicates(final Activity context, final File file, final String originalNameNoExt, final int checks, final DuplicateCheckResult callback) {
        Log.v("checkDuplicates", "Checking: " + file.getPath());
        file.exists(new File.BooleanCallback() {
            @Override
            public void onComplete(boolean result) {
                if (result) {
                    String newName = originalNameNoExt;
                    if (checks > 0) newName += " (" + checks + ")";
                    if (!file.isDirectory()) newName += "." + file.getExtension();
                    File newFile = file.isRemote() ?
                            new CloudFile(context, (CloudFile) file.getParent(), newName, file.isDirectory()) :
                            new LocalFile(context, file.getParent(), newName);
                    checkDuplicates(context, newFile, originalNameNoExt, 1 + checks, callback);
                } else callback.onResult(file);
            }

            @Override
            public void onError(Exception e) {
                SilkDialog.create(context)
                        .setAccentColorRes(R.color.cabinet_color)
                        .setTitle(R.string.error)
                        .setMessage(e.getMessage())
                        .show(true);
            }
        });
    }

    public static File checkDuplicatesSync(final Activity context, final File file) throws Exception {
        return checkDuplicatesSync(context, file, file.getNameNoExtension(), 0);
    }

    private static File checkDuplicatesSync(final Activity context, final File file, final String originalNameNoExt, final int checks) throws Exception {
        Log.v("checkDuplicatesSync", "Checking: " + file.getPath());
        if (file.existsSync()) {
            String newName = originalNameNoExt;
            if (checks > 0) newName += " (" + checks + ")";
            if (!file.isDirectory()) newName += "." + file.getExtension();
            File newFile = file.isRemote() ?
                    new CloudFile(context, (CloudFile) file.getParent(), newName, file.isDirectory()) :
                    new LocalFile(context, file.getParent(), newName);
            return checkDuplicatesSync(context, newFile, originalNameNoExt, 1 + checks);
        } else return file;
    }

    public static void setSorter(DirectoryFragment context, int sorter) {
        PreferenceManager.getDefaultSharedPreferences(context.getActivity()).edit().putInt("sorter", sorter).commit();
        context.resort();
    }

    public static int getSorter(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt("sorter", 0);
    }

    public static void setShowHidden(DirectoryFragment context, boolean show) {
        PreferenceManager.getDefaultSharedPreferences(context.getActivity()).edit().putBoolean("show_hidden", show).commit();
        context.reload();
    }

    public static boolean getShowHidden(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("show_hidden", false);
    }

    public static void showConfirmDialog(Activity context, int title, int message, String replacement, final SilkDialog.DialogCallback callback) {
        context.setTheme(R.style.Theme_Cabinet);
        SilkDialog.create(context)
                .setAccentColorRes(R.color.cabinet_color)
                .setTitle(title)
                .setMessage(message, replacement)
                .setPostiveButtonText(R.string.yes)
                .setNegativeButtonText(R.string.no)
                .setButtonListener(callback)
                .show(true);
    }

    public static void showErrorDialog(final Activity context, final int message, final Exception e) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                context.setTheme(R.style.Theme_Cabinet);
                SilkDialog.create(context)
                        .setAccentColorRes(R.color.cabinet_color)
                        .setTitle(R.string.error)
                        .setMessage(message, e.getMessage())
                        .show(true);
            }
        });
    }

    public static void showErrorDialog(final Activity context, final String message) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                context.setTheme(R.style.Theme_Cabinet);
                SilkDialog.create(context)
                        .setAccentColorRes(R.color.cabinet_color)
                        .setTitle(R.string.error)
                        .setMessage(message)
                        .show(true);
            }
        });
    }

    public static ProgressDialog showProgressDialog(Activity context, int message, ProgressDialog.OnCancelListener cancelListener) {
        ProgressDialog mDialog = new ProgressDialog(context);
        mDialog.setCancelable(cancelListener != null);
        mDialog.setOnCancelListener(cancelListener);
        mDialog.setMessage(context.getString(message));
        mDialog.setIndeterminate(true);
        mDialog.show();
        return mDialog;
    }

    public static ProgressDialog showProgressDialog(Activity context, int message) {
        return showProgressDialog(context, message, null);
    }

    public static void showInputDialog(Activity context, int title, int hint, String prefillInput, final SilkDialog.InputCallback callback) {
        context.setTheme(R.style.Theme_Cabinet);
        String hintStr = null;
        if (hint != 0) hintStr = context.getString(hint);
        SilkDialog.create(context)
                .setAccentColorRes(R.color.cabinet_color)
                .setTitle(title)
                .setAcceptsInput(true, hintStr, prefillInput, callback)
                .show(true);
    }

    private static void openLocal(final Activity context, final File file, String mime) {
        if (mime == null) {
            SilkDialog.create(context)
                    .setAccentColorRes(R.color.cabinet_color)
                    .setTitle(R.string.open_as)
                    .setItems(R.array.open_as, new SilkDialog.SelectionCallback() {
                        @Override
                        public void onSelection(int index, String value) {
                            String newMime;
                            switch (index) {
                                default:
                                    newMime = "text/*";
                                    break;
                                case 1:
                                    newMime = "image/*";
                                    break;
                                case 2:
                                    newMime = "audio/*";
                                    break;
                                case 3:
                                    newMime = "video/*";
                                    break;
                                case 4:
                                    newMime = "*/*";
                                    break;
                            }
                            openLocal(context, file, newMime);
                        }
                    }).show(true);
            return;
        }
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(Uri.fromFile(file.toJavaFile()), mime));
        } catch (ActivityNotFoundException e) {
            if (mime == null) Toast.makeText(context, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            else openLocal(context, file, null);
        }
    }

//    public static File generateIndexedName(File file) {
//        if (!file.exists()) return file;
//        File newFile;
//        int index = 1;
//        while (true) {
//            newFile = new File(file.getContext(), file.getParent(),
//                    file.getNameNoExtension() + " (" + index + ")" + file.getExtension());
//            if (!newFile.exists()) break;
//            index++;
//        }
//        newFile.setSilkId(file.getSilkId());
//        return newFile;
//    }

    private static boolean cancelledDownload;

    public static void openFile(final DrawerActivity context, final File item, final boolean openAs) {
        if (item.isRemote()) {
            final java.io.File downloadDir = new java.io.File(Environment.getExternalStorageDirectory(), "Download");
            if (!downloadDir.exists()) downloadDir.mkdir();
            java.io.File tester = new java.io.File(downloadDir, item.getName());
            if (tester.exists() && tester.length() == item.length()) {
                openLocal(context, new LocalFile(context, tester), openAs ? null : item.getMimeType());
                return;
            }
            final java.io.File dest = new java.io.File(downloadDir, item.getName());
            cancelledDownload = false;
            final ProgressDialog connectDialog = Utils.showProgressDialog(context, R.string.connecting, new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    cancelledDownload = true;
                }
            });
            context.getNetworkService().getSftpClient(new NetworkService.SftpGetCallback() {
                @Override
                public void onSftpClient(SftpClient client) {
                    if (cancelledDownload) return;
                    connectDialog.dismiss();
                    final ProgressDialog downloadDialog = Utils.showProgressDialog(context, R.string.downloading, new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            cancelledDownload = true;
                        }
                    });
                    client.get(item.getPath(), dest.getPath(), new SftpClient.CancelableCompletionCallback() {
                        @Override
                        public void onComplete() {
                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (!cancelledDownload) {
                                        downloadDialog.dismiss();
                                        openLocal(context, new LocalFile(context, dest), openAs ? null : item.getMimeType());
                                    } else if (dest.exists()) dest.delete();
                                }
                            });
                        }

                        @Override
                        public boolean shouldCancel() {
                            return cancelledDownload;
                        }

                        @Override
                        public void onError(final Exception e) {
                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    downloadDialog.dismiss();
                                    Utils.showErrorDialog(context, R.string.failed_download_file, e);
                                }
                            });
                        }
                    });
                }

                @Override
                public void onError(final Exception e) {
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            connectDialog.dismiss();
                            Utils.showErrorDialog(context, R.string.failed_connect_server, e);
                        }
                    });
                }
            }, (CloudFile) item);
            return;
        }
        openLocal(context, item, openAs ? null : item.getMimeType());
    }
}