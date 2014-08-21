package com.afollestad.cabinet.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.cabinet.R;
import com.afollestad.cabinet.sftp.SftpClient;
import com.afollestad.cabinet.ui.DrawerActivity;
import com.afollestad.cabinet.utils.Pins;

public class RemoteConnectionDialog implements SftpClient.CompletionCallback {

    public RemoteConnectionDialog(Activity context) {
        mContext = context;
    }

    private Activity mContext;
    private SftpClient client;
    private AlertDialog dialog;
    private Button testConnection;
    private TextView host;
    private TextView port;
    private TextView user;
    private TextView pass;

    public void show() {
        View view = mContext.getLayoutInflater().inflate(R.layout.dialog_add_remote, null);
        testConnection = (Button) view.findViewById(R.id.testConnection);
        host = (TextView) view.findViewById(R.id.host);
        port = (TextView) view.findViewById(R.id.port);
        user = (TextView) view.findViewById(R.id.user);
        pass = (TextView) view.findViewById(R.id.pass);

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if (host.getText().toString().trim().length() > 0 &&
                        port.getText().toString().trim().length() > 0 &&
                        user.getText().toString().trim().length() > 0 &&
                        pass.getText().toString().trim().length() > 0) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    testConnection.setEnabled(true);
                } else {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    testConnection.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        };
        host.addTextChangedListener(watcher);
        port.addTextChangedListener(watcher);
        user.addTextChangedListener(watcher);
        pass.addTextChangedListener(watcher);
        testConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setEnabled(false);
                host.setEnabled(false);
                port.setEnabled(false);
                user.setEnabled(false);
                pass.setEnabled(false);
                client = new SftpClient()
                        .setHost(host.getText().toString().trim(), Integer.parseInt(port.getText().toString().trim()))
                        .setUser(user.getText().toString())
                        .setPass(pass.getText().toString())
                        .connect(RemoteConnectionDialog.this);
            }
        });

        dialog = new AlertDialog.Builder(mContext)
                .setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        onSubmit();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (client.isConnected()) client.disconnect();
                        client = null;
                        dialogInterface.dismiss();
                    }
                }).create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
    }

    @Override
    public void onComplete() {
        if (client == null) return;
        client.disconnect();
        client = null;
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                testConnection.setEnabled(true);
                host.setEnabled(true);
                port.setEnabled(true);
                user.setEnabled(true);
                pass.setEnabled(true);
                testConnection.setText(R.string.connection_successful);
            }
        });
    }

    @Override
    public void onError(final Exception e) {
        if (client == null) return;
        client = null;
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                testConnection.setEnabled(true);
                host.setEnabled(true);
                port.setEnabled(true);
                user.setEnabled(true);
                pass.setEnabled(true);
                testConnection.setText(e.getMessage());
            }
        });
    }

    protected void onSubmit() {
        if (client != null) {
            client.disconnect();
            client = null;
        }
        Pins.add(mContext, new Pins.Item(
                host.getText().toString().trim(),
                Integer.parseInt(port.getText().toString().trim()),
                user.getText().toString().trim(),
                pass.getText().toString().trim(),
                "/"
        ));
        ((DrawerActivity) mContext).reloadNavDrawer(true);
    }
}
