package com.afollestad.cabinet.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import com.afollestad.cabinet.R;
import com.afollestad.cabinet.file.CloudFile;
import com.afollestad.cabinet.sftp.SftpClient;
import com.afollestad.cabinet.ui.DrawerActivity;

public class NetworkService extends Service {

    private final IBinder mBinder = new LocalBinder();
    private SftpClient mSftp;

    public class LocalBinder extends Binder {
        public NetworkService getService() {
            return NetworkService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null && intent.getAction().equals(DISCONNECT_SFTP)) {
            if (mSftp != null) {
                if (mSftp.isConnected())
                    mSftp.disconnect();
                mSftp = null;
            }
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(CONNECTION_NOTI);
            sendBroadcast(new Intent(DISCONNECT_SFTP));
        }
        return START_STICKY;
    }

    public interface SftpGetCallback {
        public abstract void onSftpClient(SftpClient client);

        public abstract void onError(Exception e);
    }

    private static final int CONNECTION_NOTI = 1000;
    private static final int OPEN_MAIN = 2000;
    private static final int DISCONNECT_RC = 3000;
    public static final String DISCONNECT_SFTP = "com.afollestad.cabinet.services.DISCONNECT_SFTP";

    private void startPersistedNotification(CloudFile file) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent mainIntent = new Intent(this, DrawerActivity.class)
                .putExtra("remote", file);
        PendingIntent mainPi = PendingIntent.getActivity(this, OPEN_MAIN,
                mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder nb = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_status)
                .setContentTitle(getString(R.string.cabinet_sftp))
                .setContentText(getString(R.string.connection_notification_content,
                        file.getRemote().getHost(), file.getRemote().getPort()))
                .setContentIntent(mainPi)
                .setOngoing(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            Intent disconnectIntent = new Intent(this, NetworkService.class)
                    .setAction(DISCONNECT_SFTP);
            PendingIntent disconnectPi = PendingIntent.getService(this, DISCONNECT_RC,
                    disconnectIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            nb.addAction(R.drawable.ic_stat_close, getString(R.string.disconnect), disconnectPi);
            nm.notify(CONNECTION_NOTI, nb.build());
            return;
        }
        nm.notify(CONNECTION_NOTI, nb.getNotification());
    }

    public SftpClient getSftpClient() {
        return mSftp;
    }

    public void getSftpClient(final SftpGetCallback callback, final CloudFile from) {
        if (mSftp == null) mSftp = new SftpClient();
        else if (mSftp.isConnected()) {
            if (!mSftp.getRemote().equals(from.getRemote())) {
                mSftp.disconnect();
            } else {
                callback.onSftpClient(mSftp);
                return;
            }
        }
        mSftp.setRemote(from.getRemote())
                .connect(new SftpClient.CompletionCallback() {
                    @Override
                    public void onComplete() {
                        callback.onSftpClient(mSftp);
                        startPersistedNotification(from);
                    }

                    @Override
                    public void onError(Exception e) {
                        callback.onError(e);
                    }
                });
    }

    @Override
    public void onDestroy() {
        if (mSftp != null) {
            mSftp.disconnect();
            mSftp = null;
        }
        super.onDestroy();
    }
}
