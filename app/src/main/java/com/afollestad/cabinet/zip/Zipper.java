package com.afollestad.cabinet.zip;

import android.app.ProgressDialog;
import android.util.Log;

import com.afollestad.cabinet.R;
import com.afollestad.cabinet.file.LocalFile;
import com.afollestad.cabinet.file.base.File;
import com.afollestad.cabinet.fragments.DirectoryFragment;
import com.afollestad.cabinet.utils.Utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Zipper {

    private static ProgressDialog mDialog;

    public static void zip(final DirectoryFragment context, final List<File> files, final ZipCallback callback) {
        LocalFile dest = new LocalFile(context.getActivity(), context.getDirectory(), files.get(0).getNameNoExtension() + ".zip");
        Utils.checkDuplicates(context.getActivity(), dest, new Utils.DuplicateCheckResult() {
            @Override
            public void onResult(final File dest) {
                mDialog = ProgressDialog.show(context.getActivity(), "", context.getString(R.string.zipping), true, false);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            FileOutputStream fout = new FileOutputStream(dest.toJavaFile());
                            ZipOutputStream zout = new ZipOutputStream(fout);
                            writeFiles("", zout, files);
                            zout.close();
                            if (context.getActivity() == null) return;
                            context.getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mDialog.dismiss();
                                    context.reload();
                                    if (callback != null) callback.onComplete();
                                }
                            });
                        } catch (final Exception e) {
                            e.printStackTrace();
                            try {
                                if (dest.existsSync())
                                    dest.deleteSync();
                            } catch (Exception e2) {
                                e2.printStackTrace();
                            }
                            if (context.getActivity() == null) return;
                            context.getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (mDialog != null) mDialog.dismiss();
                                    Utils.showErrorDialog(context.getActivity(), R.string.failed_zip_files, e);
                                }
                            });
                        }
                    }
                }).start();
            }
        });
    }

    private static void log(String message) {
        Log.v("Zipper", message);
    }

    private static void writeFiles(String currentDir, ZipOutputStream zout, List<File> files) throws Exception {
        log("Writing " + files.size() + " files to " + currentDir);
        byte[] buffer = new byte[1024];
        for (File fi : files) {
            if (fi.isDirectory()) {
                writeFiles(currentDir + "/" + fi.getName(), zout, fi.listFilesSync(true));
                continue;
            }
            log(" >>> Writing: " + currentDir + "/" + fi.getName());
            ZipEntry ze = new ZipEntry(currentDir + "/" + fi.getName());
            ze.setSize(fi.length());
            FileInputStream fin = new FileInputStream(fi.getPath());
            zout.putNextEntry(ze);
            int length;
            while ((length = fin.read(buffer)) > 0) {
                zout.write(buffer, 0, length);
            }
            zout.closeEntry();
            fin.close();
        }
    }

    public static interface ZipCallback {
        public abstract void onComplete();
    }
}