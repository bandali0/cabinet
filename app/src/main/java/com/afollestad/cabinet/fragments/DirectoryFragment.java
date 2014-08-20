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
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.cabinet.R;
import com.afollestad.cabinet.adapters.FileAdapter;
import com.afollestad.cabinet.cab.CopyCab;
import com.afollestad.cabinet.cab.CutCab;
import com.afollestad.cabinet.cab.MainCab;
import com.afollestad.cabinet.cab.base.BaseCab;
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
import com.afollestad.cabinet.file.base.FileFilter;
import com.afollestad.cabinet.services.NetworkService;
import com.afollestad.cabinet.sftp.SftpClient;
import com.afollestad.cabinet.ui.DrawerActivity;
import com.afollestad.cabinet.ui.SettingsActivity;
import com.afollestad.cabinet.utils.PauseOnScrollListener;
import com.afollestad.cabinet.utils.Pins;
import com.afollestad.cabinet.utils.ThemeUtils;
import com.afollestad.cabinet.utils.Utils;
import com.afollestad.cabinet.zip.Unzipper;
import com.afollestad.cabinet.zip.Zipper;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.lang.reflect.Field;
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
    public String filter;

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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mDirectory = (File) getArguments().getSerializable("path");
        mQuery = getArguments().getString("query");
        super.onCreate(savedInstanceState);

        if (mQuery != null) mQuery = mQuery.trim();
        Activity act = getActivity();
        showHidden = Utils.getShowHidden(act);
        sorter = Utils.getSorter(act);
        filter = Utils.getFilter(act);
    }

    @Override
    public void onResume() {
        super.onResume();

        DrawerActivity act = (DrawerActivity) getActivity();
        act.registerReceiver(mReceiver, new IntentFilter(NetworkService.DISCONNECT_SFTP));
        if (!((DrawerLayout) act.findViewById(R.id.drawer_layout)).isDrawerOpen(Gravity.START)) {
            if (mQuery != null) {
                act.setTitle(Html.fromHtml(getString(R.string.search_x, mQuery)));
            } else {
                act.setTitle(mDirectory.getDisplay());
            }
        }

        BaseCab cab = ((DrawerActivity) getActivity()).getCab();
        if (cab != null && cab instanceof BaseFileCab) {
            mAdapter.restoreCheckedPaths(((BaseFileCab) cab).getFiles());
            if (act.shouldAttachFab) {
                ((DrawerActivity) getActivity()).invalidateSystemBarTintManager();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ((DrawerActivity) getActivity()).waitFabInvalidate();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                DrawerActivity act = (DrawerActivity) getActivity();
                                BaseFileCab cab = (BaseFileCab) act.getCab()
                                        .setFragment(DirectoryFragment.this);
                                cab.start();
                                act.shouldAttachFab = false;
                            }
                        });
                    }
                }).start();
            } else cab.setFragment(this);
        }

        ((NavigationDrawerFragment) act.getFragmentManager().findFragmentByTag("NAV_DRAWER")).selectFile(mDirectory);
        String persistentFilter = Utils.getFilter(getActivity());
        if (showHidden != Utils.getShowHidden(getActivity()) ||
                sorter != Utils.getSorter(getActivity()) ||
                (filter == null && persistentFilter != null) ||
                (filter != null && persistentFilter == null) ||
                (filter != null && !filter.equals(persistentFilter))) {
            showHidden = Utils.getShowHidden(getActivity());
            sorter = Utils.getSorter(getActivity());
            filter = persistentFilter;
            reload();
        }
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

        if (filter != null) {
            if (filter.equals("archives")) {
                menu.findItem(R.id.filterArchives).setChecked(true);
            } else {
                String[] splitFilter = filter.split(":");
                if (splitFilter[0].equals("mime")) {
                    if (splitFilter[1].equals("text")) {
                        menu.findItem(R.id.filterText).setChecked(true);
                    } else if (splitFilter[1].equals("image")) {
                        menu.findItem(R.id.filterImage).setChecked(true);
                    } else if (splitFilter[1].equals("audio")) {
                        menu.findItem(R.id.filterAudio).setChecked(true);
                    } else if (splitFilter[1].equals("video")) {
                        menu.findItem(R.id.filterVideo).setChecked(true);
                    }
                } else if (splitFilter[0].equals("ext")) {
                    menu.findItem(R.id.filterOther).setChecked(true);
                }
            }
        } else menu.findItem(R.id.filterNone).setChecked(true);

        boolean canShow = !((DrawerLayout) getActivity().findViewById(R.id.drawer_layout)).isDrawerOpen(Gravity.START);
        if (!mDirectory.isRemote()) {
            try {
                canShow = canShow && mDirectory.existsSync();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        }
        boolean searchMode = mQuery != null;
        menu.findItem(R.id.sort).setVisible(canShow);
        menu.findItem(R.id.goUp).setVisible(!searchMode && canShow && mDirectory.getParent() != null);

        final MenuItem search = menu.findItem(R.id.search);
        if (canShow && !searchMode) {
            assert search != null;
            SearchView searchView = (SearchView) search.getActionView();
            EditText searchPlate = (EditText) searchView.findViewById(searchView.getContext().getResources().getIdentifier("android:id/search_src_text", null, null));
            searchPlate.setTextColor(getResources().getColor(android.R.color.white));
            // TODO uncomment if statement for Material
            if (Build.VERSION.SDK_INT < 20) {
                searchPlate.setBackgroundResource(Utils.resolveDrawable(getActivity(), android.R.attr.editTextBackground));
                try {
                    Field searchField = SearchView.class.getDeclaredField("mCloseButton");
                    searchField.setAccessible(true);
                    ImageView closeBtn = (ImageView) searchField.get(searchView);
                    closeBtn.setImageResource(R.drawable.ic_action_close);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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

    private void createNewFileDuplicate(final Activity context, final File file) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File newFile = Utils.checkDuplicatesSync(context, file);
                    newFile.createFile(new SftpClient.CompletionCallback() {
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
                        public void onError(final Exception e) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Utils.showErrorDialog(context, e.getMessage());
                                }
                            });
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Utils.showErrorDialog(context, e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void showNewFileDialog(final Activity context) {
        Utils.showInputDialog(context, R.string.new_file, R.string.untitled, null,
                new Utils.InputCallback() {
                    @Override
                    public void onInput(String newName) {
                        if (newName.isEmpty())
                            newName = getString(R.string.untitled);
                        final File newFile = mDirectory.isRemote() ?
                                new CloudFile(context, (CloudFile) mDirectory, newName, false) :
                                new LocalFile(context, mDirectory, newName);
                        newFile.exists(new File.BooleanCallback() {
                            @Override
                            public void onComplete(boolean result) {
                                if (!result) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            newFile.createFile(new SftpClient.CompletionCallback() {
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
                                                public void onError(final Exception e) {
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            Utils.showErrorDialog(context, e.getMessage());
                                                        }
                                                    });
                                                }
                                            });
                                        }
                                    });
                                } else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            CustomDialog.create(getActivity(), R.string.file_already_exists, getString(R.string.file_already_exists_warning),
                                                    android.R.string.ok, 0, android.R.string.cancel, new CustomDialog.SimpleClickListener() {
                                                        @Override
                                                        public void onPositive(int which, View view) {
                                                            createNewFileDuplicate(context, newFile);
                                                        }
                                                    }
                                            ).show(getFragmentManager(), "DUPLICATE_WARNING");
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                if (e != null)
                                    Utils.showErrorDialog(context, e.getMessage());
                            }
                        });
                    }
                }
        );
    }

    @Override
    public void onFabPressed(BaseFileCab.PasteMode pasteMode) {
        if (getActivity() != null) {
            if (pasteMode == BaseFileCab.PasteMode.ENABLED) {
                ((BaseFileCab) ((DrawerActivity) getActivity()).getCab()).paste();
            } else {
                final Activity context = getActivity();
                CustomDialog.create(getActivity(), R.string.newStr, R.array.new_options, new CustomDialog.SimpleClickListener() {
                    @Override
                    public void onPositive(int which, View view) {
                        switch (which) {
                            case 0: // File
                                showNewFileDialog(context);
                                break;
                            case 1: // Folder
                                showNewFolderDialog(context);
                                break;
                            case 2: // Remote connection
                                new RemoteConnectionDialog(context).show();
                                break;
                        }
                    }
                }).show(getActivity().getFragmentManager(), "NEW_DIALOG");
            }
        }
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

        ((DrawerActivity) getActivity()).setFabListener(this);
        reload();
    }

    protected void runOnUiThread(Runnable runnable) {
        Activity act = getActivity();
        if (act != null) act.runOnUiThread(runnable);
    }

    public final void setStatus(int message, String replacement) {
        View v = getView();
        if (v == null) return;
        TextView status = (TextView) v.findViewById(R.id.status);
        if (message == 0) status.setVisibility(View.GONE);
        else {
            status.setVisibility(View.VISIBLE);
            status.setText(getString(message, replacement));
        }
    }

    public final void setListShown(boolean shown) {
        View v = getView();
        if (v != null) {
            if (shown) {
                v.findViewById(R.id.listFrame).setVisibility(View.VISIBLE);
                v.findViewById(android.R.id.progress).setVisibility(View.GONE);
                boolean showEmpty = mAdapter.getItemCount() == 0;
                v.findViewById(android.R.id.empty).setVisibility(showEmpty ? View.VISIBLE : View.GONE);
                ((RecyclerView) v.findViewById(android.R.id.list)).setAdapter(mAdapter);
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
                    ((TextView) v.findViewById(R.id.emptyText)).setText(text);
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
                try {
                    LocalFile searchTarget;
                    if (mDirectory.requiresRoot()) {
                        searchTarget = new LocalFile(getActivity(), "/");
                    } else {
                        searchTarget = new LocalFile(getActivity(), Environment.getExternalStorageDirectory());
                    }
                    final List<File> results = searchDir(showHidden, searchTarget);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Collections.sort(results, getComparator());
                            mAdapter.set(results);
                            setListShown(true);
                        }
                    });
                } catch (final Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mDirectory.isRemote()) {
                                ((DrawerActivity) getActivity()).disableFab(false);
                            }
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
            }
        }).start();
    }

    private List<File> searchDir(boolean includeHidden, LocalFile dir) throws Exception {
        return dir.searchRecursive(includeHidden, new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (mQuery.startsWith("type:")) {
                    String target = mQuery.substring(mQuery.indexOf(':') + 1);
                    setEmptyText(getString(R.string.no_x_files, target));
                    return file.getExtension().equalsIgnoreCase(target);
                }
                return file.getName().toLowerCase().contains(mQuery.toLowerCase());
            }
        });
    }

    private String getFilterDisplay() {
        if (filter.equals("archives")) {
            return getString(R.string.archives);
        } else {
            String[] splitFilter = filter.split(":");
            if (splitFilter[0].equals("mime")) {
                if (splitFilter[1].equals("text/")) {
                    return getString(R.string.text);
                } else if (splitFilter[1].equals("image/")) {
                    return getString(R.string.image);
                } else if (splitFilter[1].equals("audio/")) {
                    return getString(R.string.audio);
                } else if (splitFilter[1].equals("video/")) {
                    return getString(R.string.video);
                }
            }
            return splitFilter[1];
        }
    }

    public void reload() {
        final View v = getView();
        if (getActivity() == null || v == null) {
            return;
        } else if (mQuery != null) {
            search();
            return;
        }

        setListShown(false);
        mAdapter.showLastModified = (sorter == 5);
        mDirectory.setContext(getActivity());
        if (mDirectory.isRemote()) {
            ((DrawerActivity) getActivity()).disableFab(true);
        }

        FileFilter lsFilter = null;
        if (filter != null) {
            String display = getFilterDisplay();
            setStatus(R.string.filter_active, display);
            setEmptyText(getString(R.string.no_files_filter, display));
            lsFilter = new FileFilter() {
                @Override
                public boolean accept(File file) {
                    if (file.isDirectory()) return true;
                    if (filter.equals("archives")) {
                        String ext = file.getExtension();
                        return ext.equals("zip") || ext.equals("rar") || ext.equals("tar") ||
                                ext.equals("tar.gz") || ext.equals(".7z");
                    } else {
                        String[] splitFilter = filter.split(":");
                        if (splitFilter[0].equals("mime")) {
                            return file.getMimeType().startsWith(splitFilter[1]);
                        } else {
                            return file.getExtension().equals(splitFilter[1]);
                        }
                    }
                }
            };
        } else {
            setStatus(0, null);
            setEmptyText(getString(R.string.no_files));
        }

        ((ImageView) v.findViewById(R.id.emptyImage)).setImageResource(
                Utils.resolveDrawable(getActivity(), R.attr.empty_image));

        mDirectory.listFiles(showHidden, lsFilter, new File.ArrayCallback() {
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
                        if (mDirectory.isRemote()) {
                            ((DrawerActivity) getActivity()).disableFab(false);
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
                        ((ImageView) v.findViewById(R.id.emptyImage)).setImageResource(
                                Utils.resolveDrawable(getActivity(), R.attr.empty_image_error));
                        if (mDirectory.isRemote()) {
                            ((DrawerActivity) getActivity()).disableFab(false);
                        }
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
            case R.id.filterNone:
                item.setChecked(true);
                Utils.setFilter(this, null);
                break;
            case R.id.filterText:
                item.setChecked(true);
                Utils.setFilter(this, "mime:text/");
                break;
            case R.id.filterImage:
                item.setChecked(true);
                Utils.setFilter(this, "mime:image/");
                break;
            case R.id.filterAudio:
                item.setChecked(true);
                Utils.setFilter(this, "mime:audio/");
                break;
            case R.id.filterVideo:
                item.setChecked(true);
                Utils.setFilter(this, "mime:video/");
                break;
            case R.id.filterArchives:
                item.setChecked(true);
                Utils.setFilter(this, "archives");
                break;
            case R.id.filterOther: {
                final MenuItem fItem = item;
                String prefill = null;
                if (filter != null && filter.startsWith("ext"))
                    prefill = filter.split(":")[1];
                Utils.showInputDialog(getActivity(), R.string.extension, R.string.extension_hint, prefill, new Utils.InputCancelCallback() {
                    @Override
                    public void onInput(String input) {
                        fItem.setChecked(true);
                        if (input.startsWith(".")) input = input.substring(1);
                        Utils.setFilter(DirectoryFragment.this, "ext:" + input);
                    }

                    @Override
                    public void onCancel() {
                        fItem.setChecked(false);
                    }
                });
                break;
            }
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
        BaseCab cab = ((DrawerActivity) getActivity()).getCab();
        if (cab != null && (cab instanceof CopyCab || cab instanceof CutCab) && cab.isActive()) {
            if (added) ((BaseFileCab) cab).addFile(file);
            else ((BaseFileCab) cab).removeFile(file);
        } else {
            boolean shouldCreateCab = cab == null || !cab.isActive() || !(cab instanceof MainCab) && added;
            if (shouldCreateCab)
                ((DrawerActivity) getActivity()).setCab(new MainCab()
                        .setFragment(this).setFile(file).start());
            else {
                if (added) ((BaseFileCab) cab).addFile(file);
                else ((BaseFileCab) cab).removeFile(file);
            }
        }
    }

    @Override
    public void onItemClicked(int index, File file) {
        if (file.isDirectory()) {
            ((DrawerActivity) getActivity()).switchDirectory(file, false);
        } else {
            if (((DrawerActivity) getActivity()).pickMode) {
                Activity act = getActivity();
                Intent intent = act.getIntent()
                        .setData(Uri.fromFile(file.toJavaFile()));
                act.setResult(Activity.RESULT_OK, intent);
                act.finish();
            } else {
                if (file.getExtension().equals("zip")) {
                    final File fFile = file;
                    ContextThemeWrapper context = new ContextThemeWrapper(getActivity(), new ThemeUtils(getActivity()).getCurrent());
                    CustomDialog.create(context, R.string.unzip, getString(R.string.auto_unzip_prompt),
                            android.R.string.ok, 0, android.R.string.cancel, new CustomDialog.ClickListener() {
                                @Override
                                public void onPositive(int which, View view) {
                                    List<File> files = new ArrayList<File>();
                                    files.add(fFile);
                                    Unzipper.unzip(DirectoryFragment.this, files, null);
                                }

                                @Override
                                public void onNeutral() {
                                }

                                @Override
                                public void onNegative() {
                                    Utils.openFile((DrawerActivity) getActivity(), fFile, false);
                                }
                            }).show(getFragmentManager(), "AUTO_UNZIP_PROMPT");
                } else {
                    Utils.openFile((DrawerActivity) getActivity(), file, false);
                }
            }
        }
    }

    @Override
    public void onMenuItemClick(final File file, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.pin:
                Pins.add(getActivity(), new Pins.Item(file));
                ((DrawerActivity) getActivity()).reloadNavDrawer(true);
                break;
            case R.id.openAs:
                Utils.openFile((DrawerActivity) getActivity(), file, true);
                break;
            case R.id.copy: {
                BaseCab cab = ((DrawerActivity) getActivity()).getCab();
                boolean shouldCreateCopy = cab == null || !cab.isActive() || !(cab instanceof CopyCab);
                if (shouldCreateCopy) {
                    if (cab != null && cab instanceof BaseFileCab) {
                        ((BaseFileCab) cab).overrideDestroy = true;
                    }
                    ((DrawerActivity) getActivity()).setCab(new CopyCab()
                            .setFragment(this).setFile(file).start());
                } else ((BaseFileCab) cab).setFragment(this).addFile(file);
                break;
            }
            case R.id.cut: {
                BaseCab cab = ((DrawerActivity) getActivity()).getCab();
                boolean shouldCreateCut = cab == null || !cab.isActive() || !(cab instanceof CutCab);
                if (shouldCreateCut) {
                    if (cab != null && cab instanceof BaseFileCab) {
                        ((BaseFileCab) cab).overrideDestroy = true;
                    }
                    ((DrawerActivity) getActivity()).setCab(new CutCab()
                            .setFragment(this).setFile(file).start());
                } else ((BaseFileCab) cab).setFragment(this).addFile(file);
                break;
            }
            case R.id.rename:
                Utils.showInputDialog(getActivity(), R.string.rename, 0, file.getName(), new Utils.InputCallback() {
                    @Override
                    public void onInput(String text) {
                        if (!text.contains("."))
                            text += file.getExtension();
                        final File newFile = file.isRemote() ?
                                new CloudFile(getActivity(), (CloudFile) file.getParent(), text, file.isDirectory()) :
                                new LocalFile(getActivity(), file.getParent(), text);
                        file.rename(newFile, new SftpClient.CompletionCallback() {
                            @Override
                            public void onComplete() {
                                reload();
                                if (((DrawerActivity) getActivity()).getCab() != null &&
                                        ((DrawerActivity) getActivity()).getCab() instanceof BaseFileCab) {
                                    int cabIndex = ((BaseFileCab) ((DrawerActivity) getActivity()).getCab()).findFile(file);
                                    if (cabIndex > -1)
                                        ((BaseFileCab) ((DrawerActivity) getActivity()).getCab()).setFile(cabIndex, newFile);
                                    Toast.makeText(getActivity(), getString(R.string.renamed_to, newFile.getPath()), Toast.LENGTH_SHORT).show();
                                }
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
                    String mime = file.getMimeType();
                    if (file.getExtension().equals("apk")) mime = "*/*";
                    getActivity().startActivity(new Intent(Intent.ACTION_SEND)
                            .setType(mime)
                            .putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file.toJavaFile())));
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getActivity(), R.string.no_apps_for_sharing, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.delete:
                Utils.showConfirmDialog(getActivity(), R.string.delete, R.string.confirm_delete, file.getName(), new CustomDialog.SimpleClickListener() {
                    @Override
                    public void onPositive(int which, View view) {
                        file.delete(new SftpClient.CompletionCallback() {
                            @Override
                            public void onComplete() {
                                if (Pins.remove(getActivity(), file))
                                    ((DrawerActivity) getActivity()).reloadNavDrawer();
                                mAdapter.remove(file, true);
                                DrawerActivity act = (DrawerActivity) getActivity();
                                if (act.getCab() != null && act.getCab() instanceof BaseFileCab) {
                                    BaseFileCab cab = (BaseFileCab) act.getCab();
                                    if (cab.getFiles().size() > 0) {
                                        List<File> files = new ArrayList<File>();
                                        files.addAll(cab.getFiles()); // copy so it doesn't get modified by CAB functions
                                        cab.removeFile(file);
                                        for (File fi : files) {
                                            if (fi.getPath().startsWith(file.getPath())) {
                                                cab.removeFile(fi);
                                            }
                                        }
                                    }
                                }
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