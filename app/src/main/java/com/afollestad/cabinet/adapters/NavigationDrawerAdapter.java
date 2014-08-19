package com.afollestad.cabinet.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Environment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.cabinet.R;
import com.afollestad.cabinet.file.LocalFile;
import com.afollestad.cabinet.file.base.File;
import com.afollestad.cabinet.utils.Pins;

import java.util.ArrayList;
import java.util.List;

public class NavigationDrawerAdapter extends RecyclerView.Adapter<NavigationDrawerAdapter.ShortcutViewHolder> implements View.OnClickListener, View.OnLongClickListener {

    @Override
    public void onClick(View view) {
        mListener.onClick((Integer) view.getTag());
    }

    @Override
    public boolean onLongClick(View view) {
        mListener.onLongClick((Integer) view.getTag());
        return false;
    }

    public interface ClickListener {
        public abstract void onClick(int index);

        public abstract boolean onLongClick(int index);
    }

    public NavigationDrawerAdapter(Activity context, ClickListener listener) {
        mContext = context;
        mItems = new ArrayList<Pins.Item>();
        mListener = listener;
        if (Pins.getAll(context).size() == 0) {
            LocalFile item = new LocalFile(context);
            Pins.add(context, new Pins.Item(item));
            item = new LocalFile(context, Environment.getExternalStorageDirectory());
            Pins.add(context, new Pins.Item(item));
            try {
                item = new LocalFile(context, new java.io.File("/external_sd"));
                if (item.existsSync()) {
                    Pins.add(context, new Pins.Item(item));
                } else {
                    item = new LocalFile(context, new java.io.File("/extSdCard"));
                    if (item.existsSync())
                        Pins.add(context, new Pins.Item(item));
                }
                item = new LocalFile(context, new java.io.File(Environment.getExternalStorageDirectory(), "DCIM"));
                if (item.existsSync())
                    Pins.add(context, new Pins.Item(item));
                item = new LocalFile(context, new java.io.File(Environment.getExternalStorageDirectory(), "Download"));
                if (item.existsSync())
                    Pins.add(context, new Pins.Item(item));
                item = new LocalFile(context, new java.io.File(Environment.getExternalStorageDirectory(), "Music"));
                if (item.existsSync())
                    Pins.add(context, new Pins.Item(item));
                item = new LocalFile(context, new java.io.File(Environment.getExternalStorageDirectory(), "Pictures"));
                if (item.existsSync())
                    Pins.add(context, new Pins.Item(item));
            } catch (Exception e) {
                Toast.makeText(context, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        }
        reload(context);
    }

    private Activity mContext;
    private List<Pins.Item> mItems;
    private int mCheckedPos = -1;
    private ClickListener mListener;

    public void reload(Context context) {
        set(Pins.getAll(context));
    }

    public void set(List<Pins.Item> items) {
        mItems.clear();
        for (Pins.Item i : items)
            mItems.add(i);
        notifyDataSetChanged();
    }

    public int setCheckedFile(File file) {
        int index = -1;
        for (int i = 0; i < mItems.size(); i++) {
            Pins.Item item = mItems.get(i);
            if (item.getPath().equals(file.getPath())) {
                index = i;
                break;
            }
        }
        setCheckedPos(index);
        return index;
    }

    public void setCheckedPos(int index) {
        mCheckedPos = index;
        notifyDataSetChanged();
    }

    public Pins.Item getItem(int index) {
        return mItems.get(index);
    }

    public static class ShortcutViewHolder extends RecyclerView.ViewHolder {

        public ShortcutViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView;
        }

        TextView title;
    }

    @Override
    public ShortcutViewHolder onCreateViewHolder(ViewGroup parent, int index) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_drawer, parent, false);
        return new ShortcutViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ShortcutViewHolder holder, int index) {
        Pins.Item item = mItems.get(index);
        holder.title.setTag(index);
        holder.title.setOnClickListener(this);
        holder.title.setOnLongClickListener(this);
        holder.title.setActivated(mCheckedPos == index);
        if (mCheckedPos == index) {
            holder.title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        } else {
            holder.title.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        }
        if (item.isRemote()) {
            holder.title.setText(item.getDisplay(mContext));
        } else {
            File file = new LocalFile(mContext, item.getPath());
            if (file.isRoot()) {
                holder.title.setText(R.string.root);
            } else if (file.isStorageDirectory()) {
                holder.title.setText(R.string.storage);
            } else if (file.getName().startsWith("sdcard")) {
                holder.title.setText(R.string.sdcard);
            } else {
                holder.title.setText(item.getDisplay(mContext));
            }
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }
}
