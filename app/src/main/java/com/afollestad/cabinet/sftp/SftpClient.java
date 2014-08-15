package com.afollestad.cabinet.sftp;

import android.app.Activity;

import com.afollestad.cabinet.file.CloudFile;
import com.afollestad.cabinet.file.Remote;
import com.afollestad.cabinet.file.base.File;
import com.afollestad.cabinet.file.base.FileFilter;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

public class SftpClient {

    public SftpClient() {
        mSsh = new JSch();
    }

    private String mHost;
    private int mPort;
    private String mUser;
    private String mPass;

    private final JSch mSsh;
    private Session mSession;
    private ChannelSftp mChannel;
    private ChannelExec mExecChannel;

    public SftpClient setHost(String host, int port) {
        this.mHost = host;
        this.mPort = port;
        return this;
    }

    public SftpClient setUser(String user) {
        this.mUser = user;
        return this;
    }

    public SftpClient setPass(String pass) {
        this.mPass = pass;
        return this;
    }

    public SftpClient setRemote(Remote remote) {
        mHost = remote.getHost();
        mPort = remote.getPort();
        mUser = remote.getUser();
        mPass = remote.getPass();
        return this;
    }

    public Remote getRemote() {
        return new Remote(mHost, mPort, mUser, mPass);
    }

    private void initSession() throws Exception {
        if (mSession != null && mSession.isConnected()) return;
        mSession = mSsh.getSession(mUser, mHost, mPort);
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        mSession.setConfig(config);
        mSession.setPassword(mPass);
        mSession.connect();
    }

    public SftpClient connect(final CompletionCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    initSession();
                    Channel channel = mSession.openChannel("sftp");
                    channel.connect();
                    mChannel = (ChannelSftp) channel;
                } catch (Exception e) {
                    e.printStackTrace();
                    callback.onError(e);
                    return;
                }
                callback.onComplete();
            }
        }).start();
        return this;
    }

    public SftpClient execute(final String command, final OutputStream os, final CompletionCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    initSession();
                    mExecChannel = (ChannelExec) mSession.openChannel("exec");
                    mExecChannel.setCommand(command);
                    if (os != null) {
                        mExecChannel.setErrStream(os);
                        mExecChannel.setOutputStream(os);
                    } else {
                        mExecChannel.setErrStream(System.err);
                        mExecChannel.setOutputStream(System.out);
                    }
                    mExecChannel.connect();
                    callback.onComplete();
                } catch (Exception e) {
                    e.printStackTrace();
                    mExecChannel.disconnect();
                    callback.onError(e);
                } finally {
                    if (mExecChannel != null && os == null && mExecChannel.isConnected())
                        mExecChannel.disconnect();
                }
            }
        }).start();
        return this;
    }

    public ChannelExec getExecChannel() {
        return mExecChannel;
    }

    public boolean isConnected() {
        return mChannel != null && mChannel.isConnected();
    }

    public void putSync(String local, String remote) throws Exception {
        mChannel.put(local, remote);
    }

    public void put(final String local, final String remote, final CancelableCompletionCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mChannel.put(remote, local, new SftpProgressMonitor() {
                        @Override
                        public void init(int i, String s, String s2, long l) {
                        }

                        @Override
                        public boolean count(long l) {
                            return !callback.shouldCancel();
                        }

                        @Override
                        public void end() {
                            callback.onComplete();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    callback.onError(e);
                }
            }
        }).start();
    }

    public void get(final String remote, final String local, final CancelableCompletionCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mChannel.get(remote, local, new SftpProgressMonitor() {
                        @Override
                        public void init(int i, String s, String s2, long l) {
                        }

                        @Override
                        public boolean count(long l) {
                            return !callback.shouldCancel();
                        }

                        @Override
                        public void end() {
                            callback.onComplete();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    callback.onError(e);
                }
            }
        }).start();
    }

    public void lstat(final String remote, final LsStatCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SftpATTRS attrs = lstatSync(remote);
                    callback.onComplete(attrs);
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        }).start();
    }

    public SftpATTRS lstatSync(String remote) throws Exception {
        try {
            return mChannel.lstat(remote);
        } catch (Exception e) {
            if (e instanceof SftpException) {
                SftpException sftpError = (SftpException) e;
                if (sftpError.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    throw new FileNotExistsException(remote);
                } else throw e;
            } else throw e;
        }
    }

    public void getSync(final String remote, final String local) throws Exception {
        mChannel.get(remote, local);
    }

    public void ls(final Activity context, final boolean includeHidden, final String dir, final FileFilter filter, final File.ArrayCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    List<CloudFile> results = lsSync(context, includeHidden, dir, filter);
                    callback.onComplete(results.toArray(new File[results.size()]));
                } catch (Exception e) {
                    e.printStackTrace();
                    callback.onError(e);
                }
            }
        }).start();
    }

    public List<CloudFile> lsSync(Activity context, boolean includeHidden, String dir, FileFilter filter) throws Exception {
        List<CloudFile> results = new ArrayList<CloudFile>();
        Vector vector = mChannel.ls(dir);
        Enumeration enumer = vector.elements();
        while (enumer.hasMoreElements()) {
            ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) enumer.nextElement();
            if (entry.getFilename().equals(".") || entry.getFilename().equals("..")) continue;
            else if (entry.getFilename().startsWith(".") && !includeHidden) continue;
            CloudFile file = new CloudFile(context, dir, entry, getRemote());
            if (filter == null || filter.accept(file))
                results.add(file);
        }
        return results;
    }

    public void mkdir(final String dir, final CompletionCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mkdirSync(dir);
                } catch (Exception e) {
                    e.printStackTrace();
                    callback.onError(e);
                    return;
                }
                callback.onComplete();
            }
        }).start();
    }

    public void mkdirSync(String dir) throws Exception {
        mChannel.mkdir(dir);
    }

    public void rm(final File file, final CompletionCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    rmSync(file);
                } catch (Exception e) {
                    e.printStackTrace();
                    callback.onError(e);
                    return;
                }
                callback.onComplete();
            }
        }).start();
    }

    public void rmSync(File file) throws Exception {
        if (file.isDirectory()) {
            mExecChannel = (ChannelExec) mSession.openChannel("exec");
            mExecChannel.setCommand("rm -rf \"" + file.getPath() + "\"");
            mExecChannel.setErrStream(System.err);
            mExecChannel.setOutputStream(System.out);
            mExecChannel.connect();
            mExecChannel.disconnect();
        } else mChannel.rm(file.getPath());
    }

    public void rename(final File file, final File newFile, final CompletionCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mChannel.rename(file.getPath(), newFile.getPath());
                } catch (Exception e) {
                    e.printStackTrace();
                    callback.onError(e);
                    return;
                }
                callback.onComplete();
            }
        }).start();
    }

    public void disconnect() {
        if (mChannel != null && mChannel.isConnected()) {
            mChannel.exit();
            mChannel.disconnect();
        }
        if (mExecChannel != null && mExecChannel.isConnected())
            mExecChannel.disconnect();
        if (mSession != null && mSession.isConnected())
            mSession.disconnect();
        mExecChannel = null;
        mChannel = null;
        mSession = null;
    }

    public static interface FileCallback {
        public abstract void onComplete(File file);

        public abstract void onError(Exception e);
    }

    public static interface CompletionCallback {
        public abstract void onComplete();

        public abstract void onError(Exception e);
    }

    public static interface CancelableCompletionCallback extends CompletionCallback {
        public abstract boolean shouldCancel();
    }

    public static interface LsStatCallback {
        public abstract void onComplete(SftpATTRS attrs);

        public abstract void onError(Exception e);
    }
}