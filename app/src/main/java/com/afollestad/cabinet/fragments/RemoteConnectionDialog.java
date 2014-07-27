package com.afollestad.cabinet.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.cabinet.R;
import com.afollestad.cabinet.sftp.SftpClient;
import com.afollestad.cabinet.ui.DrawerActivity;
import com.afollestad.cabinet.utils.Shortcuts;
import com.afollestad.silk.dialogs.SilkDialog;

public class RemoteConnectionDialog extends SilkDialog implements SftpClient.CompletionCallback {

    public RemoteConnectionDialog() {
    }

    public static RemoteConnectionDialog create(Activity context) {
        return create(context, false);
    }

    public static RemoteConnectionDialog create(Activity context, boolean darkTheme) {
        RemoteConnectionDialog dialog = new RemoteConnectionDialog();
        dialog.mContext = context;
        Bundle args = new Bundle();
        args.putBoolean("dark_theme", darkTheme);
        dialog.setArguments(args);

        dialog.setCustomView(LayoutInflater.from(context).inflate(R.layout.dialog_add_remote, null));
        dialog.setTitle(R.string.new_remote_connection);
        dialog.setAccentColorRes(R.color.cabinet_color);

        return dialog;
    }

    private SftpClient client;
    private Button testConnection;
    private TextView host;
    private TextView port;
    private TextView user;
    private TextView pass;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
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
                    setCanSubmit(true);
                    testConnection.setEnabled(true);
                } else {
                    setCanSubmit(false);
                    testConnection.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        };
        host = (TextView) dialog.findViewById(R.id.host);
        host.addTextChangedListener(watcher);
        port = (TextView) dialog.findViewById(R.id.port);
        port.addTextChangedListener(watcher);
        user = (TextView) dialog.findViewById(R.id.user);
        user.addTextChangedListener(watcher);
        pass = (TextView) dialog.findViewById(R.id.pass);
        pass.addTextChangedListener(watcher);
        testConnection = (Button) dialog.findViewById(R.id.testConnection);
        testConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setEnabled(false);
                client = new SftpClient()
                        .setHost(host.getText().toString().trim(), Integer.parseInt(port.getText().toString().trim()))
                        .setUser(user.getText().toString())
                        .setPass(pass.getText().toString())
                        .connect(RemoteConnectionDialog.this);
            }
        });
        dialog.findViewById(android.R.id.button1).setEnabled(false);
        testConnection.setEnabled(false);
        return dialog;
    }

    @Override
    public void onComplete() {
        client.disconnect();
        client = null;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                testConnection.setEnabled(true);
                testConnection.setText(R.string.connection_successful);
            }
        });
    }

    @Override
    public void onError(final Exception e) {
        client = null;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                testConnection.setEnabled(true);
                testConnection.setText(e.getMessage());
            }
        });
    }

    @Override
    protected void onSubmit() {
        if (client != null) {
            client.disconnect();
            client = null;
        }
        super.onSubmit();
        Shortcuts.add(getActivity(), new Shortcuts.Item(
                host.getText().toString().trim(),
                Integer.parseInt(port.getText().toString().trim()),
                user.getText().toString().trim(),
                pass.getText().toString().trim(),
                "/"
        ));
        ((DrawerActivity) getActivity()).reloadNavDrawer();
    }
}
