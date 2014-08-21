package com.afollestad.cabinet.ui.base;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;

import com.afollestad.cabinet.R;
import com.afollestad.cabinet.file.CloudFile;
import com.afollestad.cabinet.file.base.File;
import com.afollestad.cabinet.fragments.CustomDialog;
import com.afollestad.cabinet.services.NetworkService;

/**
 * @author Aidan Follestad (afollestad)
 */
public abstract class NetworkedActivity extends ThemableActivity {

    private NetworkService mNetworkService;
    private CloudFile mRemoteSwitch; // used by SFTP notification intent

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        processIntent(getIntent(), savedInstanceState);
    }

    public CloudFile getRemoteSwitch() {
        return mRemoteSwitch;
    }

    protected void displayDisconnectPrompt() {
        String host = getString(R.string.unknown);
        if (mRemoteSwitch != null) {
            host = mRemoteSwitch.getRemote().getHost();
        }
        CustomDialog.create(this, R.string.disconnect, getString(R.string.disconnect_prompt, host),
                R.string.yes, 0, R.string.no, new CustomDialog.SimpleClickListener() {
                    @Override
                    public void onPositive(int which, View view) {
                        startService(new Intent(NetworkedActivity.this, NetworkService.class)
                                .setAction(NetworkService.DISCONNECT_SFTP));
                    }
                }
        ).show(getFragmentManager(), "DISCONNECT_CONFIRM");
    }

    protected boolean disconnectOnNotify() {
        return true;
    }

    protected void processIntent(Intent intent, Bundle savedInstanceState) {
        if (intent.hasExtra("remote")) {
            mRemoteSwitch = (CloudFile) intent.getSerializableExtra("remote");
            if (mNetworkService != null) {
                mNetworkService.setRemote(mRemoteSwitch);
                if (disconnectOnNotify()) {
                    switchDirectory(mRemoteSwitch, true);
                    displayDisconnectPrompt();
                    mRemoteSwitch = null;
                }
            }
        }
    }

    public void switchDirectory(File to, boolean clearBackStack) {
        // Only overridden by the DrawerActivity
    }

    //
    // SERVICE CONNECTION
    //

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, NetworkService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mConnection);
    }

    public NetworkService getNetworkService() {
        return mNetworkService;
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            NetworkService.LocalBinder binder = (NetworkService.LocalBinder) service;
            mNetworkService = binder.getService();
            if (mRemoteSwitch != null) {
                mNetworkService.setRemote(mRemoteSwitch);
                if (disconnectOnNotify()) {
                    switchDirectory(mRemoteSwitch, true);
                    displayDisconnectPrompt();
                    mRemoteSwitch = null;
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };
}
