package com.afollestad.cabinet.adapters;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;
import com.afollestad.cabinet.R;
import com.afollestad.cabinet.file.LocalFile;
import com.afollestad.cabinet.file.base.File;
import com.afollestad.cabinet.utils.Shortcuts;
import com.afollestad.silk.adapters.SilkAdapter;

public class NavigationDrawerAdapter extends SilkAdapter<Shortcuts.Item> {

    public NavigationDrawerAdapter(Activity context) {
        super(context);
        init(context);
        reload(context);
    }

    private void init(Activity context) {
        if (Shortcuts.getAll(context).size() == 0) {
            LocalFile item = new LocalFile(context);
            Shortcuts.add(context, new Shortcuts.Item(item));
            item = new LocalFile(context, Environment.getExternalStorageDirectory());
            Shortcuts.add(context, new Shortcuts.Item(item));
            item = new LocalFile(context, new java.io.File(Environment.getExternalStorageDirectory(), "DCIM"));
            if (item.existsSync())
                Shortcuts.add(context, new Shortcuts.Item(item));
            item = new LocalFile(context, new java.io.File(Environment.getExternalStorageDirectory(), "Download"));
            if (item.existsSync())
                Shortcuts.add(context, new Shortcuts.Item(item));
            item = new LocalFile(context, new java.io.File(Environment.getExternalStorageDirectory(), "Music"));
            if (item.existsSync())
                Shortcuts.add(context, new Shortcuts.Item(item));
            item = new LocalFile(context, new java.io.File(Environment.getExternalStorageDirectory(), "Pictures"));
            if (item.existsSync())
                Shortcuts.add(context, new Shortcuts.Item(item));
        }
    }

    public void reload(Context context) {
        set(Shortcuts.getAll(context), null);
    }

    @Override
    protected int getLayout(int index, int type) {
        return R.layout.list_item_drawer;
    }

    @Override
    public View onViewCreated(int index, View recycled, Shortcuts.Item item) {
        TextView text = (TextView) recycled;
        if (item.isRemote()) {
            text.setText(item.getDisplay((Activity) getContext()));
        } else {
            File file = new LocalFile((Activity) getContext(), item.getPath());
            if (file.isRoot()) {
                text.setText(R.string.root);
            } else if (file.isStorageDirectory()) {
                text.setText(R.string.storage);
            } else {
                text.setText(item.getDisplay((Activity) getContext()));
            }
        }
        return recycled;
    }

    @Override
    public Object getItemId(Shortcuts.Item item) {
        return item.getDisplay((Activity) getContext());
    }
}
