package com.afollestad.cabinet.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.cabinet.R;

public class CustomDialog extends DialogFragment implements View.OnClickListener {

    @Override
    public void onClick(View view) {
        mListener.onPositive((Integer) view.getTag());
    }

    public static abstract class ClickListener {
        public abstract void onPositive(int which);

        public void onNeutral() {
        }

        public void onNegative() {
        }
    }

    public CustomDialog() {
    }

    int title;
    String body;
    int itemsRes;
    View view;
    ClickListener mListener;
    int positive;
    int neutral;
    int negative;

    public static CustomDialog create(int title, String body, CustomDialog.ClickListener listener) {
        return create(title, body, 0, 0, 0, listener);
    }

    public static CustomDialog create(int title, int itemsRes, CustomDialog.ClickListener listener) {
        return create(title, itemsRes, 0, 0, 0, listener);
    }

    public static CustomDialog create(int title, String body, int positive, int neutral, int negative, CustomDialog.ClickListener listener) {
        return create(title, body, 0, null, positive, neutral, negative, listener);
    }

    public static CustomDialog create(int title, int itemsRes, int positive, int neutral, int negative, CustomDialog.ClickListener listener) {
        return create(title, null, itemsRes, null, positive, neutral, negative, listener);
    }

    public static CustomDialog create(int title, View view, int positive, int neutral, int negative, CustomDialog.ClickListener listener) {
        return create(title, null, 0, view, positive, neutral, negative, listener);
    }

    public static CustomDialog create(int title, String body, int itemsRes, View view, int positive, int neutral, int negative, CustomDialog.ClickListener listener) {
        CustomDialog dialog = new CustomDialog();
        dialog.title = title;
        dialog.body = body;
        dialog.itemsRes = itemsRes;
        dialog.view = view;
        dialog.mListener = listener;
        if (positive == 0 && itemsRes == 0) positive = android.R.string.ok;
        dialog.positive = positive;
        dialog.neutral = neutral;
        dialog.negative = negative;
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View rootView = layoutInflater.inflate(R.layout.dialog_custom, null);
        TextView title = (TextView) rootView.findViewById(R.id.title);
        title.setText(this.title);

        TextView body = (TextView) rootView.findViewById(R.id.body);
        LinearLayout list = (LinearLayout) rootView.findViewById(R.id.list);
        if (this.body != null) {
            body.setText(this.body);
            body.setMovementMethod(new LinkMovementMethod());
            body.setVisibility(View.VISIBLE);
            list.setVisibility(View.GONE);
        } else if (itemsRes != 0) {
            String[] items = getResources().getStringArray(itemsRes);
            for (int i = 0; i < items.length; i++) {
                TextView itemView = (TextView) layoutInflater.inflate(R.layout.list_item_dialog, null);
                itemView.setText(items[i]);
                list.addView(itemView);
                itemView.setTag(i);
                itemView.setOnClickListener(this);
            }
            list.requestLayout();
            list.setVisibility(View.VISIBLE);
            body.setVisibility(View.GONE);
        } else {
            body.setVisibility(View.GONE);
            list.setVisibility(View.VISIBLE);
            list.addView(view);
            list.requestLayout();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setView(rootView);
        if (positive != 0) {
            builder.setPositiveButton(positive, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int i) {
                    dialog.dismiss();
                    if (mListener != null) mListener.onPositive(-1);
                }
            });
        }
        if (neutral != 0) {
            builder.setNeutralButton(neutral, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int i) {
                    dialog.dismiss();
                    if (mListener != null) mListener.onNeutral();
                }
            });
        }
        if (negative != 0) {
            builder.setNegativeButton(negative, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int i) {
                    dialog.dismiss();
                    if (mListener != null) mListener.onNegative();
                }
            });
        }
        return builder.create();
    }
}