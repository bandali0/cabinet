package com.afollestad.cabinet.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.cabinet.App;
import com.afollestad.cabinet.R;
import com.afollestad.cabinet.cab.CopyCab;
import com.afollestad.cabinet.cab.CutCab;
import com.afollestad.cabinet.cab.base.BaseFileCab;
import com.afollestad.cabinet.file.base.File;
import com.afollestad.cabinet.ui.DrawerActivity;
import com.afollestad.cabinet.utils.Shortcuts;
import com.afollestad.cabinet.utils.TimeUtils;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> implements View.OnClickListener, View.OnLongClickListener {

    public FileAdapter(Activity context, ItemClickListener listener, IconClickListener iconClickListener, MenuClickListener menuListener, boolean showDirectories) {
        mContext = context;
        mFiles = new ArrayList<File>();
        mListener = listener;
        mIconListener = iconClickListener;
        mMenuListener = menuListener;
        mShowDirs = showDirectories;
        checkedPaths = new ArrayList<String>();
    }

    @Override
    public void onClick(View view) {
        String[] split = ((String) view.getTag()).split(":");
        int type = Integer.parseInt(split[0]);
        final int index = Integer.parseInt(split[1]);
        File file = mFiles.get(index);
        if (type == 0) {  // item
            if (mListener != null)
                mListener.onItemClicked(index, mFiles.get(index));
        } else if (type == 1) {  // icon
            boolean checked = !isItemChecked(file);
            setItemChecked(file, checked);
            if (mIconListener != null)
                mIconListener.onIconClicked(index, file, checked);
        } else {  // menu
            ContextThemeWrapper context = new ContextThemeWrapper(mContext, R.style.Theme_PopupMenuTheme);
            PopupMenu mPopupMenu = new PopupMenu(context, view);
            mPopupMenu.inflate(file.isDirectory() ? R.menu.dir_options : R.menu.file_options);
            boolean foundInCopyCab = false;
            boolean foundInCutCab = false;
            DrawerActivity act = (DrawerActivity) mContext;
            if (act.getCab() instanceof CopyCab) {
                foundInCopyCab = ((BaseFileCab) act.getCab()).containsFile(file);
            } else if (act.getCab() instanceof CutCab) {
                foundInCutCab = ((BaseFileCab) act.getCab()).containsFile(file);
            }
            mPopupMenu.getMenu().findItem(R.id.copy).setVisible(!foundInCopyCab);
            mPopupMenu.getMenu().findItem(R.id.cut).setVisible(!foundInCutCab);
            if (file.isDirectory()) {
                mPopupMenu.getMenu().findItem(R.id.pin).setVisible(!Shortcuts.contains(mContext, new Shortcuts.Item(file)));
            } else {
                MenuItem zip = mPopupMenu.getMenu().findItem(R.id.zip);
                if (!file.isRemote()) {
                    zip.setVisible(true);
                    if (file.getExtension().equals("zip"))
                        zip.setTitle(R.string.unzip);
                    else zip.setTitle(R.string.zip);
                } else zip.setVisible(false);
            }
            mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    mMenuListener.onMenuItemClick(mFiles.get(index), menuItem);
                    return true;
                }
            });
            mPopupMenu.show();
        }
    }

    @Override
    public boolean onLongClick(View view) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (!prefs.getBoolean("shown_iconpress_hint", false)) {
            prefs.edit().putBoolean("shown_iconpress_hint", true).commit();
            Toast.makeText(mContext, R.string.iconpress_hint, Toast.LENGTH_LONG).show();
        }
        view.findViewById(R.id.image).performClick();
        return true;
    }

    public static class FileViewHolder extends RecyclerView.ViewHolder {

        View view;
        ImageView icon;
        TextView title;
        TextView content;
        TextView directory;
        View menu;

        public FileViewHolder(View itemView) {
            super(itemView);
            view = itemView;
            icon = (ImageView) itemView.findViewById(R.id.image);
            title = (TextView) itemView.findViewById(android.R.id.title);
            content = (TextView) itemView.findViewById(android.R.id.content);
            directory = (TextView) itemView.findViewById(R.id.directory);
            menu = itemView.findViewById(R.id.menu);
        }
    }

    private Activity mContext;
    private List<File> mFiles;
    private ItemClickListener mListener;
    private IconClickListener mIconListener;
    private MenuClickListener mMenuListener;
    private boolean mShowDirs;
    private List<String> checkedPaths;
    public boolean showLastModified;

    public void add(File file) {
        mFiles.add(file);
        notifyDataSetChanged();
    }

    public void set(List<File> files) {
        mFiles.clear();
        if (files != null)
            mFiles.addAll(files);
        notifyDataSetChanged();
    }

//    public void update(File file) {
//        for (int i = 0; i < mFiles.size(); i++) {
//            if (mFiles.get(i).getPath().equals(file.getPath())) {
//                mFiles.set(i, file);
//                break;
//            }
//        }
//        notifyDataSetChanged();
//    }

    public void remove(File file) {
        for (int i = 0; i < mFiles.size(); i++) {
            if (mFiles.get(i).getPath().equals(file.getPath())) {
                mFiles.remove(i);
                break;
            }
        }
        notifyDataSetChanged();
    }

    public void clear() {
        mFiles.clear();
        notifyDataSetChanged();
    }

    public List<File> getFiles() {
        return mFiles;
    }

    @Override
    public FileViewHolder onCreateViewHolder(ViewGroup parent, int index) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_file, parent, false);
        if (mShowDirs) v.findViewById(R.id.directory).setVisibility(View.VISIBLE);
        return new FileViewHolder(v);
    }

    @Override
    public void onBindViewHolder(FileViewHolder holder, int index) {
        File file = mFiles.get(index);
        holder.view.setTag("0:" + index);
        holder.view.setOnClickListener(this);
        holder.view.setOnLongClickListener(this);
        setupTouchDelegate(mContext, holder.menu);

        holder.title.setText(file.getName());
        if (file.isDirectory()) {
            holder.content.setText(R.string.directory);
        } else {
            holder.content.setText(file.getSizeString());
        }

        loadThumbnail(mContext, file, holder.icon);

        if (mShowDirs) {
            if (!file.isDirectory())
                holder.directory.setText(file.getParent().getPath());
            else holder.directory.setVisibility(View.GONE);
        } else if (showLastModified) {
            if (file.lastModified() == -1) {
                holder.directory.setVisibility(View.GONE);
            } else {
                holder.directory.setVisibility(View.VISIBLE);
                Calendar cal = new GregorianCalendar();
                cal.setTimeInMillis(file.lastModified());
                holder.directory.setText(mContext.getString(R.string.modified_x, TimeUtils.toString(cal, true, true)));
            }
        } else holder.directory.setVisibility(View.GONE);

        holder.view.setActivated(isItemChecked(file));
        holder.icon.setTag("1:" + index);
        holder.icon.setOnClickListener(this);

        holder.menu.setTag("2:" + index);
        holder.menu.setOnClickListener(this);
    }

    public static void loadThumbnail(Context context, File file, ImageView icon) {
        String mime = file.getMimeType();
        if (mime != null) {
            if (mime.startsWith("image/")) {
                Uri uri = Uri.fromFile(file.toJavaFile());
                ImageLoader.getInstance().displayImage(Uri.decode(uri.toString()), icon);
            } else if (mime.equals("application/vnd.android.package-archive")) {
                DisplayImageOptions options = App.getDisplayOptions(R.drawable.ic_file_apk);
                ImageLoader.getInstance().displayImage(file.getPath(), icon, options);
            } else {
                int resId = R.drawable.ic_file_misc;
                if (file.isDirectory()) {
                    if (file.isHidden()) resId = R.drawable.ic_folder_hidden;
                    else resId = R.drawable.ic_folder;
                } else {
                    String ext = file.getExtension();
                    List<String> codeExts = Arrays.asList(context.getResources().getStringArray(R.array.code_extensions));
                    if (mime.startsWith("audio/") || mime.equals("application/ogg")) {
                        resId = R.drawable.ic_file_audio;
                    } else if (mime.startsWith("image/")) {
                        resId = R.drawable.ic_file_image;
                    } else if (mime.startsWith("video/")) {
                        resId = R.drawable.ic_file_video;
                    } else if (mime.equals("application/pdf")) {
                        resId = R.drawable.ic_file_pdf;
                    } else if (ext.equals("zip") || ext.equals("gzip") || ext.equals("tar") || ext.equals("tar.gz") ||
                            ext.equals("rar") || ext.equals("7z") || ext.equals("gz")) {
                        resId = R.drawable.ic_file_zip;
                    } else if (mime.startsWith("model/")) {
                        resId = R.drawable.ic_file_model;
                    } else if (file.getExtension().equals("doc") || file.getExtension().equals("docx")) {
                        resId = R.drawable.ic_file_word;
                    } else if (file.getExtension().equals("ppt") || file.getExtension().equals("pptx")) {
                        resId = R.drawable.ic_file_present;
                    } else if (file.getExtension().equals("xls") || file.getExtension().equals("xlsx")) {
                        resId = R.drawable.ic_file_excel;
                    } else if (file.getExtension().equals("ttf")) {
                        resId = R.drawable.ic_file_font;
                    } else if (file.getExtension().equals("sh") || file.getExtension().equals("bat")) {
                        resId = R.drawable.ic_file_script;
                    } else if (codeExts.contains(ext.toLowerCase(Locale.getDefault()))) {
                        resId = R.drawable.ic_file_code;
                    } else if (mime.startsWith("text/")) {
                        resId = R.drawable.ic_file_doc;
                    }
                }
                icon.setImageResource(resId);
            }
        } else icon.setImageResource(R.drawable.ic_file_misc);
    }

    @Override
    public int getItemCount() {
        return mFiles.size();
    }

    public interface ItemClickListener {
        public abstract void onItemClicked(int index, File file);
    }

    public static interface IconClickListener {
        public abstract void onIconClicked(int index, File file, boolean added);
    }

    public static interface MenuClickListener {
        public abstract void onMenuItemClick(File file, MenuItem item);
    }


    public void setItemChecked(File file, boolean checked) {
        if (checkedPaths.contains(file.getPath()) && !checked) {
            for (int i = 0; i < checkedPaths.size(); i++) {
                if (checkedPaths.get(i).equals(file.getPath())) {
                    checkedPaths.remove(i);
                    break;
                }
            }
        } else if (!checkedPaths.contains(file.getPath()) && checked) {
            checkedPaths.add(file.getPath());
        }
        notifyDataSetChanged();
    }

    public void setItemsChecked(List<File> files, boolean checked) {
        for (File fi : files) {
            setItemChecked(fi, checked);
        }
    }

    public boolean isItemChecked(File file) {
        return checkedPaths.contains(file.getPath());
    }

    public void resetChecked() {
        checkedPaths.clear();
        notifyDataSetChanged();
    }

    public List<File> checkAll() {
        List<File> newlySelected = new ArrayList<File>();
        for (File file : mFiles) {
            String path = file.getPath();
            if (!checkedPaths.contains(path)) {
                checkedPaths.add(path);
                newlySelected.add(file);
            }
        }
        notifyDataSetChanged();
        return newlySelected;
    }

    public void restoreCheckedPaths(List<File> paths) {
        if (paths == null) return;
        checkedPaths.clear();
        for (File fi : paths)
            checkedPaths.add(fi.getPath());
        notifyDataSetChanged();
    }

    public static void setupTouchDelegate(Context context, final View menu) {
        final int offset = context.getResources().getDimensionPixelSize(R.dimen.menu_touchdelegate);
        assert menu.getParent() != null;
        ((View) menu.getParent()).post(new Runnable() {
            public void run() {
                Rect delegateArea = new Rect();
                menu.getHitRect(delegateArea);
                delegateArea.top -= offset;
                delegateArea.bottom += offset;
                delegateArea.left -= offset;
                delegateArea.right += offset;
                TouchDelegate expandedArea = new TouchDelegate(delegateArea, menu);
                ((View) menu.getParent()).setTouchDelegate(expandedArea);
            }
        });
    }
}
