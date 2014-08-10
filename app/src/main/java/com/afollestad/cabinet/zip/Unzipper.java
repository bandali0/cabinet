package com.afollestad.cabinet.zip;

import android.app.ProgressDialog;
import android.util.Log;

import com.afollestad.cabinet.R;
import com.afollestad.cabinet.file.LocalFile;
import com.afollestad.cabinet.file.base.File;
import com.afollestad.cabinet.fragments.DirectoryFragment;
import com.afollestad.cabinet.utils.Utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Unzipper {

    private static ProgressDialog mDialog;

    private static void unzip(final DirectoryFragment context, final LocalFile zipFile) throws Exception {
        FileInputStream fis = new FileInputStream(zipFile.toJavaFile());
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            File dest = new LocalFile(context.getActivity(), new java.io.File(context.getDirectory().getPath() + "/" + entry.getName()));
            dest.toJavaFile().getParentFile().mkdirs();
            dest = Utils.checkDuplicatesSync(context.getActivity(), dest);
            log("Unzipping: " + dest.getPath());
            int size;
            byte[] buffer = new byte[2048];
            FileOutputStream fos = new FileOutputStream(dest.toJavaFile());
            BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length);
            while ((size = zis.read(buffer, 0, buffer.length)) != -1) {
                bos.write(buffer, 0, size);
            }
            bos.flush();
            bos.close();
        }
        zis.close();
        fis.close();
    }

    public static void unzip(final DirectoryFragment context, final List<File> files, final Zipper.ZipCallback callback) {
        mDialog = ProgressDialog.show(context.getActivity(), "", context.getString(R.string.unzipping), true, false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (File fi : files) {
                        unzip(context, (LocalFile) fi);
                    }
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
                    if (context.getActivity() == null) return;
                    context.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDialog.dismiss();
                            Utils.showErrorDialog(context.getActivity(), R.string.failed_unzip_file, e);
                        }
                    });
                }
            }
        }).start();
    }

    private static void log(String message) {
        Log.v("Unzipper", message);
    }
}