package com.afollestad.cabinet.file.base;

import android.app.Activity;
import android.os.Environment;
import android.webkit.MimeTypeMap;

import com.afollestad.cabinet.R;
import com.afollestad.cabinet.file.CloudFile;
import com.afollestad.cabinet.sftp.SftpClient;
import com.afollestad.cabinet.ui.DrawerActivity;

import java.io.Serializable;

public abstract class File implements Serializable {

    public File() {
    }

    public File(Activity context, String path) {
        mContext = context;
        mPath = path;
    }

    private transient Activity mContext;
    private String mPath;

    /* BEGIN CARD METHODS */

    public String getDisplay() {
        String name = getName();
        if (getPath().equals("/")) {
            if (isRemote()) {
                return ((CloudFile) this).getRemote().getHost();
            } else return getContext().getString(R.string.root);
        } else if (isStorageDirectory() || name.equals("sdcard")) {
            return getContext().getString(R.string.storage);
        }
        return name;
    }

    /* END CARD METHODS */

    private String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    private int recursiveSize(java.io.File dir) {
        java.io.File[] contents = dir.listFiles();
        if (contents == null) return 0;
        int count = 0;
        for (java.io.File f : contents) {
            if (f.isDirectory()) {
                // One for directory itself, rest of contents
                count += (recursiveSize(f) + 1);
            } else count++; // One for regular file
        }
        return count;
    }

    public final String getSizeString() {
        if (isDirectory()) {
            return mContext.getString(R.string.x_filesdirs, recursiveSize(toJavaFile()));
        }
        return humanReadableByteCount(length(), true);
    }

    public final String getExtension() {
        if (isDirectory()) return "";
        String name = getName().toLowerCase();
        if (name.startsWith(".") || !name.substring(1).contains("."))
            return getContext().getString(R.string.unknown);
        return name.substring(name.lastIndexOf('.') + 1);
    }

    public final String getMimeType() {
        String type = null;
        String extension = getExtension();
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
            if (type == null) return extension;
        }
        return type;
    }

    public final boolean isRoot() {
        return getPath().equals("/");
    }

    public final boolean isStorageDirectory() {
        return !isRemote() && getPath().equals(Environment.getExternalStorageDirectory().getAbsolutePath());
    }

    public final DrawerActivity getContext() {
        return (DrawerActivity) mContext;
    }

    public final void setContext(Activity context) {
        mContext = context;
    }

    public final String getName() {
        if (getPath().contains("/")) {
            String str = getPath().substring(getPath().lastIndexOf('/') + 1);
            if (str.trim().isEmpty()) str = "/";
            return str;
        } else return getPath();
    }

    public abstract File getParent();

    public final String getNameNoExtension() {
        if (isDirectory()) return getName();
        String name = getName();
        if (name.startsWith(".") || !name.substring(1).contains(".")) return name;
        return name.substring(0, name.lastIndexOf('.'));
    }

    public final String getPath() {
        return mPath;
    }

    public abstract void mkdir(SftpClient.CompletionCallback callback);

    public abstract void rename(File newFile, SftpClient.FileCallback callback);

    public abstract void copy(File dest, SftpClient.FileCallback callback);

    public abstract void delete(SftpClient.CompletionCallback callback);

    public abstract boolean isRemote();

    public abstract boolean isDirectory();

    public abstract void exists(BooleanCallback callback);

    public abstract boolean existsSync() throws Exception;

    public abstract long length();

    public abstract void listFiles(boolean includeHidden, ArrayCallback callback);

    public abstract long lastModified();

    public static interface ArrayCallback {
        public abstract void onComplete(File[] results);

        public abstract void onError(Exception e);
    }

    public static interface BooleanCallback {
        public abstract void onComplete(boolean result);

        public abstract void onError(Exception e);
    }

    public final java.io.File toJavaFile() {
        return new java.io.File(getPath());
    }

    @Override
    public final boolean equals(Object o) {
        return o instanceof File && ((File) o).getPath().equals(getPath());
    }
}
