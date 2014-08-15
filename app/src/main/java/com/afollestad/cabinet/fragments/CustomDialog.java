package com.afollestad.cabinet.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.cabinet.R;

public class CustomDialog extends DialogFragment implements View.OnClickListener {

    @Override
    public void onClick(View view) {
        dismiss();
        mListener.onPositive((Integer) view.getTag(), inflatedView);
    }

    public interface SimpleClickListener {
        void onPositive(int which, View view);
    }

    public interface ClickListener extends SimpleClickListener {
        void onNeutral();

        void onNegative();
    }

    public interface DismissListener {
        void onDismiss();
    }

    public CustomDialog() {
    }

    int title;
    String body;
    int itemsRes;
    int view;
    int positive;
    int neutral;
    int negative;
    SimpleClickListener mListener;
    View inflatedView;
    DismissListener mDismiss;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("title", title);
        outState.putString("body", body);
        outState.putInt("item_res", itemsRes);
        outState.putInt("view", view);
        outState.putInt("positive", positive);
        outState.putInt("neutral", neutral);
        outState.putInt("negative", negative);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            title = savedInstanceState.getInt("title");
            body = savedInstanceState.getString("body");
            itemsRes = savedInstanceState.getInt("item_res");
            view = savedInstanceState.getInt("view");
            positive = savedInstanceState.getInt("positive");
            neutral = savedInstanceState.getInt("neutral");
            negative = savedInstanceState.getInt("negative");
            if (view != 0) inflatedView = getActivity().getLayoutInflater().inflate(view, null);
        }
    }

    public View getInflatedView() {
        return inflatedView;
    }

    public static CustomDialog create(Activity context, int title, String body, CustomDialog.SimpleClickListener listener) {
        return create(context, title, body, 0, 0, 0, listener);
    }

    public static CustomDialog create(Activity context, int title, int itemsRes, CustomDialog.SimpleClickListener listener) {
        return create(context, title, null, itemsRes, 0, 0, 0, 0, listener);
    }

    public static CustomDialog create(Activity context, int title, String body, int positive, int neutral, int negative, CustomDialog.SimpleClickListener listener) {
        return create(context, title, body, 0, 0, positive, neutral, negative, listener);
    }

    public static CustomDialog create(Activity context, int title, String body, int itemsRes, int view, int positive, int neutral, int negative, CustomDialog.SimpleClickListener listener) {
        CustomDialog dialog = new CustomDialog();
        dialog.title = title;
        dialog.body = body;
        dialog.itemsRes = itemsRes;
        dialog.view = view;
        if (view != 0) dialog.inflatedView = context.getLayoutInflater().inflate(view, null);
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
            if (inflatedView.getParent() != null) {
                ((ViewGroup) inflatedView.getParent()).removeView(inflatedView);
            }
            list.addView(inflatedView);
            list.requestLayout();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setView(rootView);
        if (positive != 0) {
            builder.setPositiveButton(positive, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int i) {
                    dialog.dismiss();
                    if (mListener != null) mListener.onPositive(-1, inflatedView);
                }
            });
        }
        if (neutral != 0) {
            builder.setNeutralButton(neutral, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int i) {
                    dialog.dismiss();
                    if (mListener != null && mListener instanceof ClickListener)
                        ((ClickListener) mListener).onNeutral();
                }
            });
        }
        if (negative != 0) {
            builder.setNegativeButton(negative, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int i) {
                    dialog.dismiss();
                    if (mListener != null && mListener instanceof ClickListener)
                        ((ClickListener) mListener).onNegative();
                }
            });
        }
        return builder.create();
    }

    public void setDismissListener(DismissListener listener) {
        mDismiss = listener;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mDismiss != null) mDismiss.onDismiss();
    }
}