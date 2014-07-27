package com.afollestad.cabinet.adapters;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.cabinet.R;
import com.afollestad.cabinet.file.base.File;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> implements View.OnClickListener {

    public FileAdapter(Context context, ItemClickListener listener, IconClickListener iconClickListener, boolean showDirectories) {
        mContext = context;
        mFiles = new ArrayList<File>();
        mListener = listener;
        mIconListener = iconClickListener;
        mShowDirs = showDirectories;
        checkedPaths = new ArrayList<String>();
    }

    @Override
    public void onClick(View view) {
        String[] split = ((String) view.getTag()).split(":");
        int type = Integer.parseInt(split[0]);
        int index = Integer.parseInt(split[1]);
        if (type == 0) {
            if (mListener != null)
                mListener.onItemClicked(index, mFiles.get(index));
        } else {
            File file = mFiles.get(index);
            boolean checked = !isItemChecked(file);
            setItemChecked(file, checked);
            if (mIconListener != null)
                mIconListener.onIconClicked(index, file, checked);
        }
    }

    public static class FileViewHolder extends RecyclerView.ViewHolder {

        View view;
        ImageView icon;
        TextView title;
        TextView content;
        TextView directory;

        public FileViewHolder(View itemView) {
            super(itemView);
            view = itemView;
            icon = (ImageView) itemView.findViewById(R.id.image);
            title = (TextView) itemView.findViewById(android.R.id.title);
            content = (TextView) itemView.findViewById(android.R.id.content);
            directory = (TextView) itemView.findViewById(R.id.directory);
        }
    }

    private Context mContext;
    private List<File> mFiles;
    private ItemClickListener mListener;
    private IconClickListener mIconListener;
    private boolean mShowDirs;
    private List<String> checkedPaths;

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

    public void remove(File file) {
        for(int i = 0; i < mFiles.size(); i++) {
            if(mFiles.get(i).getPath().equals(file.getPath())) {
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
        v.setTag("0:" + index);
        v.setOnClickListener(this);
        if (mShowDirs) v.findViewById(R.id.directory).setVisibility(View.VISIBLE);
        return new FileViewHolder(v);
    }

    @Override
    public void onBindViewHolder(FileViewHolder holder, int index) {
        File file = mFiles.get(index);
        holder.view.setTag(index);

        holder.title.setText(file.getName());
        if (file.isDirectory()) holder.content.setText(R.string.directory);
        else holder.content.setText(file.getMimeType() + " – " + file.getSizeString());

        String mime = file.getMimeType();
        if (mime != null && mime.startsWith("image/")) {
            Uri uri = Uri.fromFile(file.toJavaFile());
            ImageLoader.getInstance().displayImage(Uri.decode(uri.toString()), holder.icon);
        } else if (mime != null && mime.equals("application/vnd.android.package-archive")) {
            PackageManager pm = mContext.getPackageManager();
            PackageInfo pi = pm.getPackageArchiveInfo(file.getPath(), 0);
            try {
                pi.applicationInfo.sourceDir = file.getPath();
                pi.applicationInfo.publicSourceDir = file.getPath();
                ApplicationInfo appInfo = pi.applicationInfo;
                if (appInfo.icon != 0) {
                    Uri uri = Uri.parse("android.resource://" + appInfo.packageName + "/" + appInfo.icon);
                    ImageLoader.getInstance().displayImage(uri.toString(), holder.icon);
                }
            } catch (Exception e) {
                e.printStackTrace();
                holder.icon.setImageResource(R.drawable.ic_file);
            }
        } else {
            int resId = R.drawable.ic_file;
            if (file.isDirectory()) {
                resId = R.drawable.ic_folder;
            } else if (mime != null) {
                if (mime.startsWith("image/")) {
                    resId = R.drawable.ic_image;
                } else if (mime.startsWith("video/")) {
                    resId = R.drawable.ic_video;
                } else if (mime.startsWith("audio/") || mime.equals("application/ogg")) {
                    resId = R.drawable.ic_audio;
                }
            }
            holder.icon.setImageResource(resId);
        }

        if (mShowDirs) {
            if (!file.isDirectory())
                holder.directory.setText(file.getParent().getPath());
            else holder.directory.setVisibility(View.GONE);
        }

        holder.view.setActivated(isItemChecked(file));
        holder.icon.setTag("1:" + index);
        holder.icon.setOnClickListener(this);
    }

//    @Override
//    protected void onPreMenuOpen(CardBase item, Menu menu) {
//        if (item instanceof File) {
//            File file = (File) item;
//            boolean foundInCopyCab = false;
//            boolean foundInCutCab = false;
//            if (((MainActivity) getContext()).getFileCab() instanceof CopyCab) {
//                foundInCopyCab = ((MainActivity) getContext()).getFileCab().containsFile(file);
//            } else if (((MainActivity) getContext()).getFileCab() instanceof CutCab) {
//                foundInCutCab = ((MainActivity) getContext()).getFileCab().containsFile(file);
//            }
//            menu.findItem(R.id.copy).setVisible(!foundInCopyCab);
//            menu.findItem(R.id.cut).setVisible(!foundInCutCab);
//
//            if (file.isDirectory()) {
//                menu.findItem(R.id.pin).setVisible(!Shortcuts.contains(getContext(), new Shortcuts.Item(file)));
//            } else {
//                MenuItem zip = menu.findItem(R.id.zip);
//                if (!file.isRemote()) {
//                    zip.setVisible(true);
//                    if (file.getExtension().equals("zip"))
//                        zip.setTitle(R.string.unzip);
//                    else zip.setTitle(R.string.zip);
//                } else zip.setVisible(false);
//                boolean canExecute = !file.getMimeType().startsWith("image/") &&
//                        !file.getMimeType().startsWith("video/") &&
//                        !file.getMimeType().startsWith("audio/");
//                menu.findItem(R.id.execute).setVisible(canExecute);
//            }
//        }
//    }

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

    public boolean isItemChecked(File file) {
        return checkedPaths.contains(file.getPath());
    }

    public void resetChecked() {
        checkedPaths.clear();
        notifyDataSetChanged();
    }

    public List<File> checkAll() {
        List<File> newlySelected = new ArrayList<File>();
        for (int i = 0; i < mFiles.size(); i++) {
            File file = (File) mFiles.get(i);
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
}
