package com.afollestad.cabinet.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.cabinet.R;
import com.afollestad.cabinet.adapters.FileAdapter;
import com.afollestad.cabinet.cab.CopyCab;
import com.afollestad.cabinet.cab.CutCab;
import com.afollestad.cabinet.cab.MainCab;
import com.afollestad.cabinet.cab.base.BaseFileCab;
import com.afollestad.cabinet.comparators.AlphabeticalComparator;
import com.afollestad.cabinet.comparators.ExtensionComparator;
import com.afollestad.cabinet.comparators.FoldersFirstComparator;
import com.afollestad.cabinet.comparators.HighLowSizeComparator;
import com.afollestad.cabinet.comparators.LastModifiedComparator;
import com.afollestad.cabinet.comparators.LowHighSizeComparator;
import com.afollestad.cabinet.file.CloudFile;
import com.afollestad.cabinet.file.LocalFile;
import com.afollestad.cabinet.file.base.File;
import com.afollestad.cabinet.services.NetworkService;
import com.afollestad.cabinet.sftp.SftpClient;
import com.afollestad.cabinet.ui.DrawerActivity;
import com.afollestad.cabinet.ui.SettingsActivity;
import com.afollestad.cabinet.utils.PauseOnScrollListener;
import com.afollestad.cabinet.utils.Shortcuts;
import com.afollestad.cabinet.utils.Utils;
import com.afollestad.cabinet.zip.Unzipper;
import com.afollestad.cabinet.zip.Zipper;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DirectoryFragment extends Fragment implements FileAdapter.IconClickListener, FileAdapter.ItemClickListener, FileAdapter.MenuClickListener, DrawerActivity.FabListener {

    private final transient BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(NetworkService.DISCONNECT_SFTP)) {
                ((DrawerActivity) getActivity()).switchDirectory(null, true);
            }
        }
    };

    public DirectoryFragment() {
    }

    private File mDirectory;
    private String mQuery;
    public FileAdapter mAdapter;
    private boolean showHidden;
    public int sorter;

    public File getDirectory() {
        return mDirectory;
    }

    public static DirectoryFragment create(File directory) {
        DirectoryFragment frag = new DirectoryFragment();
        Bundle b = new Bundle();
        b.putSerializable("path", directory);
        frag.setArguments(b);
        return frag;
    }

    public static DirectoryFragment create(File directory, String query) {
        DirectoryFragment frag = new DirectoryFragment();
        Bundle b = new Bundle();
        b.putSerializable("path", directory);
        b.putString("query", query);
        frag.setArguments(b);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mDirectory = (File) getArguments().getSerializable("path");
        mQuery = getArguments().getString("query");
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        if (mQuery != null) mQuery = mQuery.trim();
        showHidden = Utils.getShowHidden(getActivity());
        sorter = Utils.getSorter(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        Activity act = getActivity();
        act.registerReceiver(mReceiver, new IntentFilter(NetworkService.DISCONNECT_SFTP));
        if (mQuery != null) {
            act.setTitle(Html.fromHtml(getString(R.string.search_x, mQuery)));
        } else act.setTitle(mDirectory.getDisplay());

        BaseFileCab fileCab = ((DrawerActivity) getActivity()).getFileCab();
        if (fileCab != null) {
            mAdapter.restoreCheckedPaths(fileCab.getFiles());
            fileCab.setFragment(this);
        }
        ((NavigationDrawerFragment) act.getFragmentManager().findFragmentByTag("NAV_DRAWER")).selectFile(mDirectory);

        if (showHidden != Utils.getShowHidden(getActivity())) {
            showHidden = Utils.getShowHidden(getActivity());
            reload();
        } else if (sorter != Utils.getSorter(getActivity())) {
            sorter = Utils.getSorter(getActivity());
            reload();
        }

        ((DrawerActivity) act).checkMaterialAndRating();
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            getActivity().unregisterReceiver(mReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_menu, menu);
        switch (sorter) {
            default:
                menu.findItem(R.id.sortNameFoldersTop).setChecked(true);
                break;
            case 1:
                menu.findItem(R.id.sortName).setChecked(true);
                break;
            case 2:
                menu.findItem(R.id.sortExtension).setChecked(true);
                break;
            case 3:
                menu.findItem(R.id.sortSizeLowHigh).setChecked(true);
                break;
            case 4:
                menu.findItem(R.id.sortSizeHighLow).setChecked(true);
                break;
            case 5:
                menu.findItem(R.id.sortLastModified).setChecked(true);
                break;
        }

        boolean canShow = !((DrawerLayout) getActivity().findViewById(R.id.drawer_layout)).isDrawerOpen(Gravity.START);
        if (!mDirectory.isRemote()) {
            canShow = canShow && ((LocalFile) mDirectory).existsSync();
        }
        boolean searchMode = mQuery != null;
        menu.findItem(R.id.sort).setVisible(canShow);
        menu.findItem(R.id.goUp).setVisible(!searchMode && canShow && mDirectory.getParent() != null);

        final MenuItem search = menu.findItem(R.id.search);
        if (canShow && !searchMode) {
            assert search != null;
            SearchView searchView = (SearchView) search.getActionView();
            if (Build.VERSION.SDK_INT < 20) {
                View view = searchView.findViewById(searchView.getContext().getResources().getIdentifier("android:id/search_plate", null, null));
                view.setBackgroundResource(R.drawable.cabinet_edit_text_holo_light);
            }
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    search.collapseActionView();
                    ((DrawerActivity) getActivity()).search(mDirectory, query);
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    return false;
                }
            });
            searchView.setQueryHint(getString(R.string.search_files));
        } else search.setVisible(false);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recyclerview, null);
    }

    private void showNewFolderDialog(final Activity context) {
        Utils.showInputDialog(context, R.string.new_folder, R.string.untitled, null,
                new Utils.InputCallback() {
                    @Override
                    public void onInput(String newName) {
                        if (newName.isEmpty())
                            newName = getString(R.string.untitled);
                        final File dir = mDirectory.isRemote() ?
                                new CloudFile(context, (CloudFile) mDirectory, newName, true) :
                                new LocalFile(context, mDirectory, newName);
                        dir.exists(new File.BooleanCallback() {
                            @Override
                            public void onComplete(boolean result) {
                                if (!result) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            dir.mkdir(new SftpClient.CompletionCallback() {
                                                @Override
                                                public void onComplete() {
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            reload();
                                                        }
                                                    });
                                                }

                                                @Override
                                                public void onError(Exception e) {
                                                    Utils.showErrorDialog(context, e.getMessage());
                                                }
                                            });
                                        }
                                    });
                                } else {
                                    Utils.showErrorDialog(context, getString(R.string.directory_already_exists));
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                Utils.showErrorDialog(context, e.getMessage());
                            }
                        });
                    }
                }
        );
    }

    @Override
    public void onFabPressed(boolean pasteMode) {
        if (getActivity() != null) {
            if (pasteMode) {
                ((DrawerActivity) getActivity()).getFileCab().paste();
            } else {
                final Activity context = getActivity();
                CustomDialog.create(R.string.newStr, R.array.new_options, new CustomDialog.ClickListener() {
                    @Override
                    public void onPositive(int which) {
                        switch (which) {
                            case 0: // Folder
                                showNewFolderDialog(context);
                                break;
                            case 1: // Remote connection
                                new RemoteConnectionDialog(context).show();
                                break;
                        }
                    }
                }).show(getActivity().getFragmentManager(), "NEW_DIALOG");
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((DrawerActivity) getActivity()).setFabListener(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView mRecyclerView = (RecyclerView) view.findViewById(android.R.id.list);
        mRecyclerView.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(), true, true, new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(int scrollState) {
            }

            @Override
            public void onScrolled(int dx, int dy) {
                if (dy < 0) {
                    if (dy < -5) {
                        ((DrawerActivity) getActivity()).toggleFab(false);
                    }
                } else if (dy > 0) {
                    if (dy > 10) {
                        ((DrawerActivity) getActivity()).toggleFab(true);
                    }
                }
            }
        }));

        DrawerActivity.setupTranslucentBottomPadding(getActivity(), mRecyclerView);
        DrawerActivity.setupTranslucentTopPadding(getActivity(), view.findViewById(R.id.listFrame));

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new FileAdapter(getActivity(), this, this, this, mQuery != null);
        mRecyclerView.setAdapter(mAdapter);

        reload();
    }

    protected void runOnUiThread(Runnable runnable) {
        Activity act = getActivity();
        if (act != null) act.runOnUiThread(runnable);
    }

    public final void setListShown(boolean shown) {
        View v = getView();
        if (v != null) {
            if (shown) {
                v.findViewById(R.id.listFrame).setVisibility(View.VISIBLE);
                v.findViewById(android.R.id.progress).setVisibility(View.GONE);
                boolean showEmpty = mAdapter.getItemCount() == 0;
                v.findViewById(android.R.id.empty).setVisibility(showEmpty ? View.VISIBLE : View.GONE);
                v.findViewById(android.R.id.list).setVisibility(showEmpty ? View.GONE : View.VISIBLE);
            } else {
                v.findViewById(R.id.listFrame).setVisibility(View.GONE);
                v.findViewById(android.R.id.progress).setVisibility(View.VISIBLE);
            }
        }
    }

    protected final void setEmptyText(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View v = getView();
                if (v != null) {
                    ((TextView) v.findViewById(android.R.id.empty)).setText(text);
                }
            }
        });
    }

    private Comparator<File> getComparator() {
        Comparator<File> comparator;
        switch (sorter) {
            default:
                comparator = new FoldersFirstComparator();
                break;
            case 1:
                comparator = new AlphabeticalComparator();
                break;
            case 2:
                comparator = new ExtensionComparator();
                break;
            case 3:
                comparator = new LowHighSizeComparator();
                break;
            case 4:
                comparator = new HighLowSizeComparator();
                break;
            case 5:
                comparator = new LastModifiedComparator();
                break;
        }
        return comparator;
    }

    public void search() {
        setListShown(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<File> results = searchDir(showHidden, new LocalFile(getActivity(), Environment.getExternalStorageDirectory()));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Collections.sort(results, getComparator());
                        mAdapter.set(results);
                        setListShown(true);
                    }
                });
            }
        }).start();
    }

    private List<File> searchDir(boolean includeHidden, LocalFile dir) {
        return dir.searchRecursive(includeHidden, new FileFilter() {
            @Override
            public boolean accept(java.io.File file) {
                if (mQuery.startsWith("type:")) {
                    LocalFile currentFile = new LocalFile(getActivity(), file);
                    String target = mQuery.substring(mQuery.indexOf(':') + 1);
                    setEmptyText(getString(R.string.no_x_files, target));
                    return currentFile.getExtension().equalsIgnoreCase(target);
                }
                return file.getName().toLowerCase().contains(mQuery.toLowerCase());
            }
        });
    }

    public void reload() {
        if (getActivity() == null || getView() == null) {
            return;
        } else if (mQuery != null) {
            search();
            return;
        }
        setListShown(false);
        mAdapter.showLastModified = (sorter == 5);
        mDirectory.setContext(getActivity());
        mDirectory.listFiles(showHidden, new File.ArrayCallback() {
            @Override
            public void onComplete(final File[] results) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.clear();
                        if (results != null && results.length > 0) {
                            Arrays.sort(results, getComparator());
                            for (File fi : results) {
                                mAdapter.add(fi);
                            }
                        }
                        try {
                            setListShown(true);
                        } catch (IllegalStateException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onError(final Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String message = e.getMessage();
                            if (message.trim().isEmpty())
                                message = getString(R.string.error);
                            setEmptyText(message);
                            setListShown(true);
                        } catch (IllegalStateException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    public void resort() {
        Collections.sort(mAdapter.getFiles(), getComparator());
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.goUp:
                ((DrawerActivity) getActivity()).switchDirectory(mDirectory.getParent(), false);
                break;
            case R.id.sortNameFoldersTop:
                item.setChecked(true);
                Utils.setSorter(this, 0);
                break;
            case R.id.sortName:
                item.setChecked(true);
                Utils.setSorter(this, 1);
                break;
            case R.id.sortExtension:
                item.setChecked(true);
                Utils.setSorter(this, 2);
                break;
            case R.id.sortSizeLowHigh:
                item.setChecked(true);
                Utils.setSorter(this, 3);
                break;
            case R.id.sortSizeHighLow:
                item.setChecked(true);
                Utils.setSorter(this, 4);
                break;
            case R.id.sortLastModified:
                item.setChecked(true);
                Utils.setSorter(this, 5);
                break;
            case R.id.donation1:
                ((DrawerActivity) getActivity()).donate(1);
                break;
            case R.id.donation2:
                ((DrawerActivity) getActivity()).donate(2);
                break;
            case R.id.donation3:
                ((DrawerActivity) getActivity()).donate(3);
                break;
            case R.id.donation4:
                ((DrawerActivity) getActivity()).donate(4);
                break;
            case R.id.settings:
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onIconClicked(int index, File file, boolean added) {
        BaseFileCab cab = ((DrawerActivity) getActivity()).getFileCab();
        if (cab != null && (cab instanceof CopyCab || cab instanceof CutCab) && cab.isActive()) {
            if (added) ((DrawerActivity) getActivity()).getFileCab().addFile(file);
            else ((DrawerActivity) getActivity()).getFileCab().removeFile(file);
        } else {
            boolean shouldCreateCab = cab == null || !cab.isActive() || !(cab instanceof MainCab) && added;
            if (shouldCreateCab)
                ((DrawerActivity) getActivity()).setFileCab((BaseFileCab) new MainCab()
                        .setFragment(this).setFile(file).start());
            else {
                if (added) ((DrawerActivity) getActivity()).getFileCab().addFile(file);
                else ((DrawerActivity) getActivity()).getFileCab().removeFile(file);
            }
        }
    }

    @Override
    public void onItemClicked(int index, File file) {
        if (file.isDirectory()) {
            ((DrawerActivity) getActivity()).switchDirectory(file, false);
        } else {
            Utils.openFile((DrawerActivity) getActivity(), file, false);
        }
    }

    @Override
    public void onMenuItemClick(final File file, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.pin:
                Shortcuts.add(getActivity(), new Shortcuts.Item(file));
                ((DrawerActivity) getActivity()).reloadNavDrawer();
                break;
            case R.id.openAs:
                Utils.openFile((DrawerActivity) getActivity(), file, true);
                break;
            case R.id.copy: {
                BaseFileCab cab = ((DrawerActivity) getActivity()).getFileCab();
                boolean shouldCreateCopy = cab == null || !cab.isActive() || !(cab instanceof CopyCab);
                if (shouldCreateCopy) {
                    if (cab != null) cab.overrideDestroy = true;
                    ((DrawerActivity) getActivity()).setFileCab((BaseFileCab) new CopyCab()
                            .setFragment(this).setFile(file).start());
                } else cab.addFile(file);
                break;
            }
            case R.id.cut: {
                BaseFileCab cab = ((DrawerActivity) getActivity()).getFileCab();
                boolean shouldCreateCut = cab == null || !cab.isActive() || !(cab instanceof CutCab);
                if (shouldCreateCut) {
                    if (cab != null) cab.overrideDestroy = true;
                    ((DrawerActivity) getActivity()).setFileCab((BaseFileCab) new CutCab()
                            .setFragment(this).setFile(file).start());
                } else cab.addFile(file);
                break;
            }
            case R.id.rename:
                Utils.showInputDialog(getActivity(), R.string.rename, 0, file.getName(), new Utils.InputCallback() {
                    @Override
                    public void onInput(String text) {
                        if (!text.contains("."))
                            text += file.getExtension();
                        File newFile = file.isRemote() ?
                                new CloudFile(getActivity(), (CloudFile) file.getParent(), text, file.isDirectory()) :
                                new LocalFile(getActivity(), file.getParent(), text);
                        file.rename(newFile, new SftpClient.FileCallback() {
                            @Override
                            public void onComplete(File newFile) {
                                mAdapter.update(newFile);
                            }

                            @Override
                            public void onError(Exception e) {
                                // Ignore
                            }
                        });
                    }
                });
                break;
            case R.id.zip:
                final List<File> files = new ArrayList<File>();
                files.add(file);
                if (file.getExtension().equals("zip")) {
                    Unzipper.unzip(this, files, null);
                } else {
                    Zipper.zip(this, files, null);
                }
                break;
            case R.id.share:
                try {
                    getActivity().startActivity(new Intent(Intent.ACTION_SEND)
                            .setType(file.getMimeType())
                            .putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file.toJavaFile())));
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getActivity(), R.string.no_apps_for_sharing, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.delete:
                Utils.showConfirmDialog(getActivity(), R.string.delete, R.string.confirm_delete, file.getName(), new CustomDialog.ClickListener() {
                    @Override
                    public void onPositive(int which) {
                        file.delete(new SftpClient.CompletionCallback() {
                            @Override
                            public void onComplete() {
                                mAdapter.remove(file);
                            }

                            @Override
                            public void onError(Exception e) {
                                // Ignore
                            }
                        });
                    }
                });
                break;
            case R.id.details:
                DetailsDialog.create(file).show(getActivity().getFragmentManager(), "DETAILS_DIALOG");
                break;
        }
    }
}