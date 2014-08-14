package com.afollestad.cabinet.file;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Environment;
import android.util.Log;

import com.afollestad.cabinet.R;
import com.afollestad.cabinet.file.base.File;
import com.afollestad.cabinet.services.NetworkService;
import com.afollestad.cabinet.sftp.SftpClient;
import com.afollestad.cabinet.utils.Utils;

import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class LocalFile extends File {

    public LocalFile(Activity context) {
        super(context, "/");
    }

    public LocalFile(Activity context, String path) {
        super(context, path);
    }

    public LocalFile(Activity context, java.io.File local) {
        super(context, local.getAbsolutePath());
    }

    public LocalFile(Activity context, File parent, String name) {
        super(context, parent.getPath() + (parent.getPath().equals("/") ? "" : "/") + name);
    }

    public boolean isSearchResult;

    @Override
    public boolean isHidden() {
        java.io.File mFile = new java.io.File(getPath());
        return mFile.isHidden() || mFile.getName().startsWith(".");
    }

    private List<String> runAsRoot(String command) throws Exception {
        Log.v("Cabinet-SU", command);
        boolean suAvailable = Shell.SU.available();
        if (!suAvailable) throw new Exception("Superuser is not available.");
        return Shell.SU.run(new String[]{
                "mount -o remount,rw /",
                command
        });
    }

    public final boolean requiresRoot() {
        return !getPath().contains(Environment.getExternalStorageDirectory().getAbsolutePath());
    }

    @Override
    public void createFile(final SftpClient.CompletionCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (requiresRoot()) {
                        runAsRoot("touch \"" + getPath() + "\"");
                    } else if (!toJavaFile().createNewFile())
                        throw new Exception("An unknown error occurred while creating your file.");
                    callback.onComplete();
                } catch (Exception e) {
                    e.printStackTrace();
                    callback.onError(e);
                }
            }
        }).start();
    }

    @Override
    public void mkdir(final SftpClient.CompletionCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mkdirSync();
                    callback.onComplete();
                } catch (Exception e) {
                    e.printStackTrace();
                    callback.onError(e);
                }
            }
        }).start();
    }

    public void mkdirSync() throws Exception {
        if (requiresRoot()) {
            runAsRoot("mkdir -P \"" + getPath() + "\"");
        } else {
            new java.io.File(getPath()).mkdirs();
        }
        if (!new java.io.File(getPath()).exists())
            throw new Exception("Unknown error");
    }

    @Override
    public void rename(final File newFile, final SftpClient.CompletionCallback callback) {
        if (newFile.isRemote()) {
            final ProgressDialog connectProgress = Utils.showProgressDialog(getContext(), R.string.connecting);
            getContext().getNetworkService().getSftpClient(new NetworkService.SftpGetCallback() {
                @Override
                public void onSftpClient(final SftpClient client) {
                    connectProgress.dismiss();
                    final ProgressDialog uploadProgress = Utils.showProgressDialog(getContext(), R.string.uploading);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                uploadRecursive(client, LocalFile.this, (CloudFile) newFile, true);
                                getContext().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        setPath(newFile.getPath());
                                        uploadProgress.dismiss();
                                        callback.onComplete();
                                    }
                                });
                            } catch (final Exception e) {
                                e.printStackTrace();
                                getContext().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        uploadProgress.dismiss();
                                        callback.onError(null);
                                        Utils.showErrorDialog(getContext(), R.string.failed_upload_file, e);
                                    }
                                });
                            }
                        }
                    }).start();
                }

                @Override
                public void onError(final Exception e) {
                    getContext().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            connectProgress.dismiss();
                            callback.onError(null);
                            Utils.showErrorDialog(getContext(), R.string.failed_connect_server, e);
                        }
                    });
                }
            }, (CloudFile) newFile);
        } else {
            Utils.checkDuplicates(getContext(), newFile, new Utils.DuplicateCheckResult() {
                @Override
                public void onResult(final File newFile) {
                    if (requiresRoot()) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    runAsRoot("mv \"" + getPath() + "\" \"" + newFile.getPath() + "\"");
                                    getContext().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            setPath(newFile.getPath());
                                            callback.onComplete();
                                            notifyMediaScannerService(newFile);
                                        }
                                    });
                                } catch (final Exception e) {
                                    e.printStackTrace();
                                    getContext().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Utils.showErrorDialog(getContext(), R.string.failed_rename_file, e);
                                            callback.onError(null);
                                        }
                                    });
                                }
                            }
                        }).start();
                    } else if (new java.io.File(getPath()).renameTo(newFile.toJavaFile())) {
                        getContext().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setPath(newFile.getPath());
                                callback.onComplete();
                                notifyMediaScannerService(newFile);
                            }
                        });
                    } else {
                        getContext().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Utils.showErrorDialog(getContext(), R.string.failed_rename_file, new Exception("Unknown error"));
                                callback.onError(null);
                            }
                        });
                    }
                }
            });
        }
    }

    @Override
    public void copy(File newFile, final SftpClient.FileCallback callback) {
        Utils.checkDuplicates(getContext(), newFile, new Utils.DuplicateCheckResult() {
            @Override
            public void onResult(final File dest) {
                if (dest.isRemote()) {
                    final ProgressDialog connectProgress = Utils.showProgressDialog(getContext(), R.string.connecting);
                    getContext().getNetworkService().getSftpClient(new NetworkService.SftpGetCallback() {
                        @Override
                        public void onSftpClient(final SftpClient client) {
                            connectProgress.dismiss();
                            final ProgressDialog uploadProgress = Utils.showProgressDialog(getContext(), R.string.uploading);
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        uploadRecursive(client, LocalFile.this, (CloudFile) dest, false);
                                        getContext().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                uploadProgress.dismiss();
                                                callback.onComplete(dest);
                                            }
                                        });
                                    } catch (final Exception e) {
                                        e.printStackTrace();
                                        getContext().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                uploadProgress.dismiss();
                                                callback.onError(null);
                                                Utils.showErrorDialog(getContext(), R.string.failed_upload_file, e);
                                            }
                                        });
                                    }
                                }
                            }).start();
                        }

                        @Override
                        public void onError(final Exception e) {
                            getContext().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    connectProgress.dismiss();
                                    callback.onError(null);
                                    Utils.showErrorDialog(getContext(), R.string.failed_connect_server, e);
                                }
                            });
                        }
                    }, (CloudFile) dest);
                } else {
                    if (isDirectory()) {
                        try {
                            copyRecursive(toJavaFile(), dest.toJavaFile(), false);
                            getContext().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onComplete(dest);
                                }
                            });
                        } catch (Exception e) {
                            getContext().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Utils.showErrorDialog(getContext(), R.string.failed_copy_file, new Exception("Unable to create the destination directory."));
                                    callback.onError(null);
                                }
                            });
                        }

                    } else {
                        try {
                            final LocalFile result = copySync(toJavaFile(), dest.toJavaFile());
                            getContext().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onComplete(result);
                                }
                            });
                        } catch (final Exception e) {
                            e.printStackTrace();
                            getContext().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Utils.showErrorDialog(getContext(), R.string.failed_copy_file, e);
                                    callback.onError(null);
                                }
                            });
                        }
                    }
                }
            }
        });
    }

    private LocalFile copySync(java.io.File file, java.io.File newFile) throws Exception {
        LocalFile dest = (LocalFile) Utils.checkDuplicatesSync(getContext(), new LocalFile(getContext(), newFile));
        if (requiresRoot()) {
            runAsRoot("cp -R \"" + file.getAbsolutePath() + "\" \"" + dest.getPath() + "\"");
            return dest;
        }
        InputStream in = new FileInputStream(file);
        OutputStream out = new FileOutputStream(dest.toJavaFile());
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
        File scanFile = new LocalFile(getContext(), newFile);
        notifyMediaScannerService(scanFile);
        return dest;
    }

    private void copyRecursive(java.io.File dir, java.io.File to, boolean deleteAfter) throws Exception {
        if (!to.mkdir()) throw new Exception("Unable to create the destination directory.");
        java.io.File[] subFiles = dir.listFiles();
        for (java.io.File f : subFiles) {
            java.io.File dest = new java.io.File(to, f.getName());
            if (f.isDirectory()) copyRecursive(f, dest, deleteAfter);
            else {
                if (deleteAfter) {
                    if (!f.renameTo(dest))
                        throw new Exception("Failed to move a file to the new directory.");
                } else {
                    try {
                        copySync(f, dest);
                    } catch (Exception e) {
                        throw new Exception("Failed to copy a file to the new directory (" + e.getMessage() + ").");
                    }
                }
            }
        }
        if (deleteAfter) dir.delete();
    }

    private void log(String message) {
        Log.v("LocalFile", message);
    }

    private File uploadRecursive(SftpClient client, LocalFile local, CloudFile dest, boolean deleteAfter) throws Exception {
        dest = (CloudFile) Utils.checkDuplicatesSync(getContext(), dest);
        if (local.isDirectory()) {
            log("Uploading local directory " + local.getPath() + " to " + dest.getPath());
            try {
                client.mkdirSync(dest.getPath());
            } catch (Exception e) {
                throw new Exception("Failed to create the destination directory " + dest.getPath() + " (" + e.getMessage() + ")");
            }
            log("Getting file listing for " + local.getPath());
            List<File> contents = local.listFilesSync(true);
            for (File lf : contents) {
                CloudFile newFile = new CloudFile(getContext(), dest, lf.getName(), lf.isDirectory());
                if (lf.isDirectory()) {
                    uploadRecursive(client, (LocalFile) lf, newFile, deleteAfter);
                } else {
                    log(" >> Uploading sub-file: " + lf.getPath() + " to " + newFile.getPath());
                    client.putSync(lf.getPath(), newFile.getPath());
                    if (deleteAfter && !lf.toJavaFile().delete()) {
                        throw new Exception("Failed to delete old local file " + lf.getPath());
                    }
                }
            }
            if (deleteAfter) {
                wipeDirectory(local, null);
            }
        } else {
            log("Uploading file: " + local.getPath());
            try {
                client.putSync(local.getPath(), dest.getPath());
            } catch (Exception e) {
                throw new Exception("Failed to upload " + local.getPath() + " (" + e.getMessage() + ")");
            }
            if (deleteAfter && !local.toJavaFile().delete()) {
                throw new Exception("Failed to delete old local file " + local.getPath());
            }
        }
        return dest;
    }

    private void wipeDirectory(File dir, final SftpClient.CompletionCallback callback) throws Exception {
        List<File> contents = ((LocalFile) dir).listFilesSync(true);
        if (contents != null) {
            for (File fi : contents) {
                if (fi.isDirectory()) {
                    wipeDirectory(fi, null);
                } else if (!((LocalFile) fi).deleteSync()) {
                    if (callback != null) callback.onError(new Exception("Unknown error"));
                    else throw new Exception("Failed to delete " + fi.getPath());
                    break;
                }
            }
        }
        if (!((LocalFile) dir).deleteSync()) {
            if (callback != null) callback.onError(new Exception("Unknown error"));
            else throw new Exception("Failed to delete " + dir.getPath());
            return;
        }
        if (callback != null) callback.onComplete();
    }

    @Override
    public void delete(final SftpClient.CompletionCallback callback) {
        if (requiresRoot()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        runAsRoot("rm -rf \"" + getPath() + "\"");
                        getContext().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) callback.onComplete();
                            }
                        });
                    } catch (final Exception e) {
                        e.printStackTrace();
                        getContext().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Utils.showErrorDialog(getContext(), R.string.failed_delete_file, e);
                                if (callback != null) callback.onError(null);
                            }
                        });
                    }
                }
            }).start();
        } else {
            java.io.File mFile = new java.io.File(getPath());
            if (mFile.isDirectory()) {
                try {
                    wipeDirectory(this, callback);
                } catch (Exception e) {
                    // This will not happen since a callback is passed
                }
            } else if (mFile.delete()) {
                if (callback != null) callback.onComplete();
            } else {
                Utils.showErrorDialog(getContext(), R.string.failed_delete_file, new Exception("Unknown error"));
                if (callback != null) callback.onError(null);
            }
        }
    }

    public boolean deleteSync() {
        boolean val = new java.io.File(getPath()).delete();
        notifyMediaScannerService(this);
        return val;
    }

    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public boolean isDirectory() {
        java.io.File mFile = new java.io.File(getPath());
        try {
            return mFile.getCanonicalFile().isDirectory();
        } catch (IOException e) {
            e.printStackTrace();
            return mFile.isDirectory();
        }
    }

    @Override
    public void exists(BooleanCallback callback) {
        callback.onComplete(existsSync());
    }

    @Override
    public boolean existsSync() {
        java.io.File mFile = new java.io.File(getPath());
        return mFile.exists() && isDirectory() == mFile.isDirectory();
    }

    @Override
    public long length() {
        return new java.io.File(getPath()).length();
    }

    @Override
    public void listFiles(boolean includeHidden, final ArrayCallback callback) {
        List<File> results = listFilesSync(includeHidden);
        callback.onComplete(results != null ? results.toArray(new File[results.size()]) : null);
    }

    @Override
    public long lastModified() {
        return new java.io.File(getPath()).lastModified();
    }

    public List<File> listFilesSync(boolean includeHidden) {
        return listFilesSync(includeHidden, null);
    }

    public List<File> listFilesSync(boolean includeHidden, FileFilter filter) {
        java.io.File[] list;
        if (filter != null) list = new java.io.File(getPath()).listFiles(filter);
        else list = new java.io.File(getPath()).listFiles();
        if (list == null || list.length == 0) return new ArrayList<File>();
        List<File> results = new ArrayList<File>();
        for (java.io.File local : list) {
            if (!includeHidden && (local.isHidden() || local.getName().startsWith("."))) continue;
            LocalFile file = new LocalFile(getContext(), local);
            if (filter != null) file.isSearchResult = true;
            results.add(file);
        }
        return results;
    }

    public List<File> searchRecursive(boolean includeHidden, FileFilter filter) {
        java.io.File mFile = new java.io.File(getPath());
        Log.v("SearchRecursive", "Searching: " + mFile.getAbsolutePath());
        List<File> all = listFilesSync(includeHidden);
        if (all == null || all.size() == 0) {
            Log.v("SearchRecursive", "No files in " + mFile.getAbsolutePath());
            return null;
        }
        List<File> matches = new ArrayList<File>();
        matches.addAll(listFilesSync(includeHidden, filter));
        for (File fi : all) {
            List<File> subResults = ((LocalFile) fi).searchRecursive(includeHidden, filter);
            if (subResults != null && subResults.size() > 0)
                matches.addAll(subResults);
        }
        return matches;
    }


    @Override
    public File getParent() {
        if (getPath().contains("/")) {
            if (getPath().equals("/")) return null;
            String str = getPath().substring(0, getPath().lastIndexOf('/'));
            if (str.trim().isEmpty()) str = "/";
            return new LocalFile(getContext(), str);
        } else return null;
    }
}