package com.afollestad.cabinet.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.afollestad.cabinet.R;
import com.afollestad.cabinet.file.base.File;
import com.afollestad.cabinet.utils.TimeUtils;

import java.util.GregorianCalendar;

public class DetailsDialog extends DialogFragment {

    public DetailsDialog() {
    }

    public static DetailsDialog create(File file) {
        DetailsDialog dialog = new DetailsDialog();
        Bundle args = new Bundle();
        args.putSerializable("file", file);
        dialog.setArguments(args);
        return dialog;
    }

    private TextView body;
    private File file;

    private Spanned getBody(boolean loadDirContents) {
        String content;
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(file.lastModified());
        if (file.isDirectory()) {
            String size = getString(R.string.unavailable);
            if (!file.isRemote()) {
                if (loadDirContents) size = file.getSizeString();
                else {
                    size = getString(R.string.loading);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final Spanned newBody = getBody(true);
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    body.setText(newBody);
                                }
                            });
                        }
                    }).start();
                }
            }
            content = getString(R.string.details_body_dir,
                    file.getName(), file.getPath(), size, TimeUtils.toStringLong(cal));
        } else {
            content = getString(R.string.details_body_file,
                    file.getName(), file.getPath(), file.getSizeString(), TimeUtils.toStringLong(cal), "TODO");
        }
        return Html.fromHtml(content);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        file = (File) getArguments().getSerializable("file");
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View rootView = layoutInflater.inflate(R.layout.dialog_custom, null);
        TextView title = (TextView) rootView.findViewById(R.id.title);
        title.setText(R.string.details);
        body = (TextView) rootView.findViewById(R.id.body);
        body.setText(getBody(false));
        return new AlertDialog.Builder(getActivity())
                .setView(rootView)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                    }
                }).create();
    }
}