package com.afollestad.cabinet.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.afollestad.cabinet.R;
import com.afollestad.cabinet.file.LocalFile;
import com.afollestad.cabinet.file.base.File;
import com.afollestad.cabinet.fragments.CustomDialog;
import com.afollestad.cabinet.fragments.DetailsDialog;
import com.afollestad.cabinet.utils.ThemeUtils;
import com.afollestad.cabinet.utils.Utils;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class TextEditor extends Activity implements TextWatcher {

    private ThemeUtils mThemeUtils;
    private EditText mInput;
    private java.io.File mFile;
    private String mOriginal;
    private Timer mTimer;
    private boolean mModified;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mThemeUtils = new ThemeUtils(this);
        setTheme(mThemeUtils.getCurrent());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_texteditor);

        mInput = (EditText) findViewById(R.id.input);
        mInput.addTextChangedListener(this);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        DrawerActivity.setupTransparentTints(this);

        if (getIntent().getData() != null) load(getIntent().getData());
        else mInput.setVisibility(View.VISIBLE);
    }

    private void setProgress(boolean show) {
        mInput.setVisibility(show ? View.GONE : View.VISIBLE);
        findViewById(R.id.progress).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void load(final Uri uri) {
        setProgress(true);
        Log.v("TextEditor", "Loading...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                mFile = new java.io.File(uri.getPath());
                if (!mFile.exists()) {
                    Log.v("TextEditor", "File doesn't exist...");
                    finish();
                    return;
                }

                String mime = File.getMimeType(File.getExtension(TextEditor.this, mFile.getName()));
                Log.v("TextEditor", "Mime: " + mime);
                if (!mime.startsWith("text/") && !mime.equals("application/json")) {
                    Log.v("TextEditor", "Unsupported extension");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            CustomDialog.create(TextEditor.this, R.string.unsupported_extension, getString(R.string.unsupported_extension_desc), new CustomDialog.SimpleClickListener() {
                                @Override
                                public void onPositive(int which, View view) {
                                    finish();
                                }
                            }).show(getFragmentManager(), "UNSUPPORTED_DIALOG");
                        }
                    });
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setTitle(mFile.getName());
                    }
                });
                Log.v("TextEditor", "Reading file...");
                try {
                    BufferedReader br = new BufferedReader(new FileReader(mFile));
                    String line;
                    final StringBuilder text = new StringBuilder();
                    while ((line = br.readLine()) != null) {
                        text.append(line);
                        text.append('\n');
                    }
                    Log.v("TextEditor", "Setting contents to input area...");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mOriginal = text.toString();
                                mInput.setText(mOriginal);
                            } catch (OutOfMemoryError e) {
                                Utils.showErrorDialog(TextEditor.this, e.getLocalizedMessage());
                            }
                            setProgress(false);
                        }
                    });
                } catch (final IOException e) {
                    Log.v("TextEditor", "Error: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Utils.showErrorDialog(TextEditor.this, e.getLocalizedMessage());
                            setProgress(false);
                        }
                    });
                }
            }
        }).start();
    }

    private void save(final boolean exitAfter) {
        final ProgressDialog mDialog = new ProgressDialog(this);
        mDialog.setIndeterminate(true);
        mDialog.setMessage(getString(R.string.saving));
        mDialog.setCancelable(false);
        mDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    FileOutputStream os = new FileOutputStream(mFile);
                    os.write(mInput.getText().toString().getBytes("UTF-8"));
                    os.close();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDialog.dismiss();
                            if (exitAfter) {
                                mOriginal = null;
                                finish();
                            } else {
                                mOriginal = mInput.getText().toString();
                                mModified = false;
                                invalidateOptionsMenu();
                            }
                        }
                    });
                } catch (final IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Utils.showErrorDialog(TextEditor.this, e.getLocalizedMessage());
                            mDialog.dismiss();
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mThemeUtils.isChanged()) {
            setTheme(mThemeUtils.getCurrent());
            recreate();
        }
    }

    @Override
    public void onBackPressed() {
        checkUnsavedChanges();
    }

    private void checkUnsavedChanges() {
        if (!mOriginal.equals(mInput.getText().toString())) {
            CustomDialog.create(this, R.string.unsaved_changes, getString(R.string.unsaved_changes_desc), R.string.yes, 0, R.string.no, new CustomDialog.ClickListener() {
                @Override
                public void onNeutral() {
                }

                @Override
                public void onNegative() {
                    finish();
                }

                @Override
                public void onPositive(int which, View view) {
                    save(true);
                }
            }).show(getFragmentManager(), "UNSAVED_CHANGES_DIALOG");
        } else finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.text_editor, menu);
        menu.findItem(R.id.save).setVisible(mModified);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            checkUnsavedChanges();
            return true;
        } else if (item.getItemId() == R.id.save) {
            save(false);
            return true;
        } else if (item.getItemId() == R.id.details) {
            DetailsDialog.create(new LocalFile(this, mFile)).show(getFragmentManager(), "DETAILS_DIALOG");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
        }
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mModified = !mInput.getText().toString().equals(mOriginal);
                invalidateOptionsMenu();
            }
        }, 250);
    }

    @Override
    public void afterTextChanged(Editable editable) {
    }
}
