package com.afollestad.cabinet.file;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.Toast;

import com.afollestad.cabinet.R;
import com.afollestad.cabinet.file.base.File;
import com.afollestad.cabinet.services.NetworkService;
import com.afollestad.cabinet.sftp.FileNotExistsException;
import com.afollestad.cabinet.sftp.SftpClient;
import com.afollestad.cabinet.utils.Utils;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;

import java.io.IOException;
import java.util.List;

public class CloudFile extends File {

    public CloudFile(Activity context, String path, Remote remote) {
        super(context, path);
        mRemote = remote;
    }

    public CloudFile(Activity context, CloudFile parent, String name, boolean directory) {
        super(context, parent.getPath() + (parent.getPath().equals("/") ? "" : "/") + name);
        mRemote = parent.getRemote();
        mDirectory = directory;
        mLength = parent.length();
    }

    public CloudFile(Activity context, String dir, ChannelSftp.LsEntry entry, Remote remote) {
        super(context, dir + (dir.equals("/") ? "" : "/") + entry.getFilename());
        mRemote = remote;
        mDirectory = entry.getAttrs().isDir();
        mLength = entry.getAttrs().getSize();
    }

    private final Remote mRemote;
    private boolean mDirectory;
    private long mLength = -1;

    @Override
    public boolean isHidden() {
        return getName().startsWith(".");
    }

    public Remote getRemote() {
        return mRemote;
    }

    @Override
    public void createFile(final SftpClient.CompletionCallback callback) {
        final ProgressDialog connectProgress = Utils.showProgressDialog(getContext(), R.string.connecting);
        getContext().getNetworkService().getSftpClient(new NetworkService.SftpGetCallback() {
            @Override
            public void onSftpClient(SftpClient client) {
                connectProgress.dismiss();
                final ProgressDialog makeProgress = Utils.showProgressDialog(getContext(), R.string.making_file, new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        dialogInterface.dismiss();
                    }
                });
                java.io.File tempFile;
                try {
                    tempFile = java.io.File.createTempFile(getName(), null, getContext().getCacheDir());
                } catch (final IOException e) {
                    e.printStackTrace();
                    getContext().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            makeProgress.dismiss();
                            callback.onError(null);
                            Utils.showErrorDialog(getContext(), R.string.failed_make_file, e);
                        }
                    });
                    return;
                }
                client.put(tempFile.getAbsolutePath(), getPath(), new SftpClient.CancelableCompletionCallback() {
                    @Override
                    public boolean shouldCancel() {
                        return !makeProgress.isShowing();
                    }

                    @Override
                    public void onComplete() {
                        getContext().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                makeProgress.dismiss();
                                Toast.makeText(getContext(), getContext().getString(R.string.created_file, getName()), Toast.LENGTH_SHORT).show();
                                callback.onComplete();
                            }
                        });
                    }

                    @Override
                    public void onError(final Exception e) {
                        getContext().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                makeProgress.dismiss();
                                callback.onError(null);
                                Utils.showErrorDialog(getContext(), R.string.failed_make_file, e);
                            }
                        });
                    }
                });
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
        }, this);
    }

    @Override
    public void mkdir(final SftpClient.CompletionCallback callback) {
        final ProgressDialog connectProgress = Utils.showProgressDialog(getContext(), R.string.connecting);
        getContext().getNetworkService().getSftpClient(new NetworkService.SftpGetCallback() {
            @Override
            public void onSftpClient(SftpClient client) {
                connectProgress.dismiss();
                final ProgressDialog makeProgress = Utils.showProgressDialog(getContext(), R.string.making_folder);
                client.mkdir(getPath(), new SftpClient.CompletionCallback() {
                    @Override
                    public void onComplete() {
                        getContext().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                makeProgress.dismiss();
                                Toast.makeText(getContext(), getContext().getString(R.string.created_folder, getName()), Toast.LENGTH_SHORT).show();
                                callback.onComplete();
                            }
                        });
                    }

                    @Override
                    public void onError(final Exception e) {
                        getContext().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                makeProgress.dismiss();
                                callback.onError(null);
                                Utils.showErrorDialog(getContext(), R.string.failed_make_directory, e);
                            }
                        });
                    }
                });
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
        }, this);
    }

    private void log(String message) {
        Log.v("CloudFile", message);
    }

    private LocalFile getRecursive(SftpClient client, CloudFile remote, LocalFile dest, boolean deleteAfter) throws Exception {
        dest = (LocalFile) Utils.checkDuplicatesSync(getContext(), dest);
        if (remote.isDirectory()) {
            log("Downloading remote directory " + remote.getPath() + " to " + dest.getPath());
            try {
                dest.mkdirSync();
            } catch (Exception e) {
                throw new Exception("Failed to create the destination directory (" + e.getMessage() + ")");
            }
            log("Getting file listing for " + remote.getPath());
            List<CloudFile> contents = client.lsSync(getContext(), true, remote.getPath());
            for (CloudFile cf : contents) {
                LocalFile newFile = new LocalFile(getContext(), dest, cf.getName());
                if (cf.isDirectory()) {
                    getRecursive(client, cf, newFile, deleteAfter);
                } else {
                    log(" >> Downloading sub-file: " + cf.getPath() + " to " + newFile.getPath());
                    client.getSync(cf.getPath(), newFile.getPath());
                    if (deleteAfter) client.rmSync(cf);
                }
            }
            if (deleteAfter) client.rmSync(remote);
        } else {
            log("Downloading file: " + remote.getPath());
            try {
                client.getSync(remote.getPath(), dest.getPath());
            } catch (Exception e) {
                throw new Exception("Failed to download " + remote.getPath() + " (" + e.getMessage() + ")");
            }
            if (deleteAfter) client.rmSync(remote);
        }
        return dest;
    }

    @Override
    public void rename(final File newFile, final SftpClient.CompletionCallback callback) {
        final ProgressDialog connectProgress = Utils.showProgressDialog(getContext(), R.string.connecting);
        getContext().getNetworkService().getSftpClient(new NetworkService.SftpGetCallback() {
            @Override
            public void onSftpClient(final SftpClient client) {
                connectProgress.dismiss();
                final ProgressDialog renameProgress = Utils.showProgressDialog(getContext(),
                        !newFile.isRemote() ? R.string.downloading :
                                getParent().equals(newFile.getParent()) ? R.string.renaming : R.string.moving
                );
                if (newFile.isRemote()) {
                    Utils.checkDuplicates(getContext(), newFile, new Utils.DuplicateCheckResult() {
                        @Override
                        public void onResult(final File newFile) {
                            client.rename(CloudFile.this, newFile, new SftpClient.CompletionCallback() {
                                @Override
                                public void onComplete() {
                                    getContext().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            renameProgress.dismiss();
                                            Toast.makeText(getContext(), getContext().getString(getParent().equals(newFile.getParent()) ?
                                                    R.string.renamed_to : R.string.moved_to, newFile.getPath()), Toast.LENGTH_SHORT).show();
                                            callback.onComplete();
                                        }
                                    });
                                }

                                @Override
                                public void onError(final Exception e) {
                                    getContext().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            renameProgress.dismiss();
                                            callback.onError(null);
                                            Utils.showErrorDialog(getContext(), R.string.failed_rename_file, e);
                                        }
                                    });
                                }
                            });
                        }
                    });
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                final LocalFile result = getRecursive(client, CloudFile.this, (LocalFile) newFile, true);
                                getContext().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        renameProgress.dismiss();
                                        Toast.makeText(getContext(), getContext().getString(R.string.downloaded_to, newFile.getPath()), Toast.LENGTH_SHORT).show();
                                        callback.onComplete();
                                    }
                                });
                            } catch (final Exception e) {
                                e.printStackTrace();
                                getContext().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        renameProgress.dismiss();
                                        callback.onError(null);
                                        Utils.showErrorDialog(getContext(), R.string.failed_download_file, e);
                                    }
                                });
                            }
                        }
                    }).start();
                }
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
        }, CloudFile.this);
    }

    @Override
    public void copy(File newFile, final SftpClient.FileCallback callback) {
        Utils.checkDuplicates(getContext(), newFile, new Utils.DuplicateCheckResult() {
            @Override
            public void onResult(final File dest) {
                getContext().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final ProgressDialog connectProgress = Utils.showProgressDialog(getContext(), R.string.connecting);
                        getContext().getNetworkService().getSftpClient(new NetworkService.SftpGetCallback() {
                            @Override
                            public void onSftpClient(final SftpClient client) {
                                connectProgress.dismiss();
                                if (dest.isRemote()) {
                                    final ProgressDialog copyProgress = Utils.showProgressDialog(getContext(), R.string.copying);
                                    client.execute("cp -R \"" + getPath() + "\" \"" + dest.getPath() + "\"", null, new SftpClient.CompletionCallback() {
                                        @Override
                                        public void onComplete() {
                                            getContext().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    copyProgress.dismiss();
                                                    Toast.makeText(getContext(), getContext().getString(R.string.copied_to, dest.getPath()), Toast.LENGTH_SHORT).show();
                                                    callback.onComplete(dest);
                                                }
                                            });
                                        }

                                        @Override
                                        public void onError(final Exception e) {
                                            getContext().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    copyProgress.dismiss();
                                                    callback.onError(null);
                                                    Utils.showErrorDialog(getContext(), R.string.failed_copy_file, e);
                                                }
                                            });
                                        }
                                    });
                                } else {
                                    final ProgressDialog downloadProgress = Utils.showProgressDialog(getContext(), R.string.downloading);
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                final LocalFile result = getRecursive(client, CloudFile.this, (LocalFile) dest, false);
                                                getContext().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        downloadProgress.dismiss();
                                                        Toast.makeText(getContext(), getContext().getString(R.string.downloaded_to, dest.getPath()), Toast.LENGTH_SHORT).show();
                                                        callback.onComplete(result);
                                                    }
                                                });
                                            } catch (final Exception e) {
                                                e.printStackTrace();
                                                getContext().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        downloadProgress.dismiss();
                                                        callback.onError(null);
                                                        Utils.showErrorDialog(getContext(), R.string.failed_download_file, e);
                                                    }
                                                });
                                            }
                                        }
                                    }).start();
                                }
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
                        }, CloudFile.this);
                    }
                });
            }
        });
    }

    @Override
    public void delete(final SftpClient.CompletionCallback callback) {
        final ProgressDialog connectProgress = Utils.showProgressDialog(getContext(), R.string.connecting);
        getContext().getNetworkService().getSftpClient(new NetworkService.SftpGetCallback() {
            @Override
            public void onSftpClient(final SftpClient client) {
                connectProgress.dismiss();
                getContext().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final ProgressDialog deleteProgress = Utils.showProgressDialog(getContext(), R.string.deleting);
                        client.rm(CloudFile.this, new SftpClient.CompletionCallback() {
                            @Override
                            public void onComplete() {
                                getContext().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        deleteProgress.dismiss();
                                        callback.onComplete();
                                    }
                                });
                            }

                            @Override
                            public void onError(final Exception e) {
                                getContext().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        deleteProgress.dismiss();
                                        callback.onError(null);
                                        Utils.showErrorDialog(getContext(), R.string.failed_delete_file, e);
                                    }
                                });
                            }
                        });

                    }
                });
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
        }, this);
    }

    @Override
    public boolean deleteSync() throws Exception {
        return false;
    }

    @Override
    public boolean isRemote() {
        return true;
    }

    @Override
    public boolean isDirectory() {
        return mDirectory;
    }

    @Override
    public void exists(final BooleanCallback callback) {
        getContext().getNetworkService().getSftpClient(new NetworkService.SftpGetCallback() {
            @Override
            public void onSftpClient(SftpClient client) {
                client.lstat(getPath(), new SftpClient.LsStatCallback() {
                    @Override
                    public void onComplete(SftpATTRS attrs) {
                        callback.onComplete(attrs.isDir() == isDirectory());
                    }

                    @Override
                    public void onError(Exception e) {
                        if (e instanceof FileNotExistsException) {
                            callback.onComplete(false);
                        } else callback.onError(e);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        }, this);
    }

    @Override
    public boolean existsSync() throws Exception {
        SftpClient client = getContext().getNetworkService().getSftpClient();
        try {
            SftpATTRS attrs = client.lstatSync(getPath());
            return attrs.isDir() == isDirectory();
        } catch (Exception e) {
            if (e instanceof FileNotExistsException) return false;
            else throw e;
        }
    }

    @Override
    public long length() {
        return mLength;
    }

    @Override
    public void listFiles(final boolean includeHidden, final ArrayCallback callback) {
        getContext().getNetworkService().getSftpClient(new NetworkService.SftpGetCallback() {
            @Override
            public void onSftpClient(SftpClient client) {
                client.ls(getContext(), includeHidden, getPath(), callback);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        }, this);
    }

    @Override
    public long lastModified() {
        return -1;
    }

    @Override
    public File getParent() {
        if (getPath().contains("/")) {
            if (getPath().equals("/")) return null;
            String str = getPath().substring(0, getPath().lastIndexOf('/'));
            if (str.trim().isEmpty()) str = "/";
            return new CloudFile(getContext(), str, getRemote());
        } else return null;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CloudFile)) return false;
        CloudFile cf = (CloudFile) o;
        return cf.getPath().equals(getPath()) && cf.getRemote().equals(getRemote());
    }
}
