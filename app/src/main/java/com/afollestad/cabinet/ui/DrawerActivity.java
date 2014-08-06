package com.afollestad.cabinet.ui;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.afollestad.cabinet.R;
import com.afollestad.cabinet.cab.base.BaseFileCab;
import com.afollestad.cabinet.file.CloudFile;
import com.afollestad.cabinet.file.LocalFile;
import com.afollestad.cabinet.file.base.File;
import com.afollestad.cabinet.fragments.CustomDialog;
import com.afollestad.cabinet.fragments.DirectoryFragment;
import com.afollestad.cabinet.fragments.NavigationDrawerFragment;
import com.afollestad.cabinet.fragments.WelcomeFragment;
import com.afollestad.cabinet.services.NetworkService;
import com.afollestad.cabinet.utils.Shortcuts;
import com.afollestad.cabinet.utils.ThemeUtils;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.faizmalkani.floatingactionbutton.FloatingActionButton;
import com.readystatesoftware.systembartint.SystemBarTintManager;

public class DrawerActivity extends Activity implements BillingProcessor.IBillingHandler {

    public interface FabListener {
        public abstract void onFabPressed(boolean pasteMode);
    }

    private BillingProcessor mBP;
    private boolean canExit;
    private BaseFileCab mFileCab;
    private NetworkService mNetworkService;
    private CloudFile mRemoteSwitch;
    private ThemeUtils mThemeUtils;

    private FloatingActionButton fab;
    private float fabLeft;
    private float fabRight;
    private boolean fabShown = true;
    private FabListener mFabListener;
    private boolean fabPasteMode;
    private boolean fabDisabled;

    public static void setupTransparentTints(Activity context) {
        // TODO change condition for Material
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT || !ThemeUtils.isTranslucentStatusbar(context))
            return;
        SystemBarTintManager tintManager = new SystemBarTintManager(context);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setStatusBarTintResource(R.color.cabinet_color);
    }

    public static void setupTranslucentBottomPadding(Activity context, View view) {
        if (!ThemeUtils.isTranslucentNavbar(context)) return;
        SystemBarTintManager tintManager = new SystemBarTintManager(context);
        SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
        view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(),
                view.getPaddingBottom() + config.getPixelInsetBottom());
    }

    public static void setupTranslucentTopPadding(Activity context, View view) {
        if (!ThemeUtils.isTranslucentStatusbar(context)) return;
        SystemBarTintManager tintManager = new SystemBarTintManager(context);
        SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
        view.setPadding(view.getPaddingLeft(), config.getPixelInsetTop(true), view.getPaddingRight(), view.getPaddingBottom());
    }

    public static void setupTranslucentBottomMargin(Activity context, View view) {
        if (!ThemeUtils.isTranslucentNavbar(context)) return;
        SystemBarTintManager tintManager = new SystemBarTintManager(context);
        SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        params.bottomMargin = config.getPixelInsetBottom();
        view.setLayoutParams(params);
    }

    public BaseFileCab getFileCab() {
        return mFileCab;
    }

    public void setFileCab(BaseFileCab cab) {
        mFileCab = cab;
    }

    public void toggleFab(boolean hide) {
        if (fabLeft == 0) {
            float translation = getResources().getDimension(R.dimen.fab_translation);
            fabLeft = fab.getX();
            fabRight = fab.getX() + translation;
        }
        if (hide) {
            if (fabShown) {
                ObjectAnimator outAnim = ObjectAnimator.ofFloat(fab, "x", fabLeft, fabRight);
                outAnim.setDuration(250);
                outAnim.start();
                fabShown = false;
            }
        } else {
            if (!fabShown && !fabDisabled) {
                ObjectAnimator inAnim = ObjectAnimator.ofFloat(fab, "x", fabRight, fabLeft);
                inAnim.setDuration(250);
                inAnim.start();
                fabShown = true;
            }
        }
    }

    public void disableFab(boolean disable) {
        fabDisabled = disable;
        toggleFab(disable);
    }

    public void setFabListener(FabListener mFabListener) {
        this.mFabListener = mFabListener;
    }

    public void setPasteMode(boolean pasteMode) {
        fabPasteMode = pasteMode;
        if (getFileCab() != null) getFileCab().invalidateFab();
        fab.setDrawable(getResources().getDrawable(pasteMode ? R.drawable.ic_paste : R.drawable.ic_add));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mThemeUtils = new ThemeUtils(this);
        setTheme(mThemeUtils.getCurrent());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer);

        NavigationDrawerFragment mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mFabListener != null) mFabListener.onFabPressed(fabPasteMode);
            }
        });
        fab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Toast.makeText(DrawerActivity.this, fabPasteMode ? R.string.paste : R.string.newStr, Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        setupTranslucentBottomMargin(this, fab);
        setupTransparentTints(this);

        mBP = new BillingProcessor(this, "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlPBB2hP/R0PrXtK8NPeDX7QV1fvk1hDxPVbIwRZLIgO5l/ZnAOAf8y9Bq57+eO5CD+ZVTgWcAVrS/QsiqDI/MwbfXcDydSkZLJoFofOFXRuSL7mX/jNwZBNtH0UrmcyFx1RqaHIe9KZFONBWLeLBmr47Hvs7dKshAto2Iy0v18kN48NqKxlWtj/PHwk8uIQ4YQeLYiXDCGhfBXYS861guEr3FFUnSLYtIpQ8CiGjwfU60+kjRMmXEGnmhle5lqzj6QeL6m2PNrkbJ0T9w2HM+bR7buHcD8e6tHl2Be6s/j7zn1Ypco/NCbqhtPgCnmLpeYm8EwwTnH4Yei7ACR7mXQIDAQAB", this);
        processIntent(getIntent());
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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processIntent(intent);
    }

    private void displayInfoDialog() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // TODO comment out if for Material
        if (!prefs.getBoolean("shown_material_dialog", false) && Build.VERSION.SDK_INT >= 20) {
            CustomDialog.create(R.string.material_version, getString(R.string.material_version_desc), R.string.yes, R.string.later, R.string.no, new CustomDialog.ClickListener() {
                @Override
                public void onPositive(int which) {
                    PreferenceManager.getDefaultSharedPreferences(DrawerActivity.this)
                            .edit().putBoolean("shown_material_dialog", true).commit();
                    startActivity(new Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse("https://plus.google.com/u/0/communities/110440751142118056139")));
                }

                @Override
                public void onNegative() {
                    PreferenceManager.getDefaultSharedPreferences(DrawerActivity.this)
                            .edit().putBoolean("shown_material_dialog", true).commit();
                }
            }).show(getFragmentManager(), "MATERIAL_DIALOG");
        } else if (!prefs.getBoolean("shown_rating_dialog", false)) {
            CustomDialog.create(R.string.rate, getString(R.string.rate_desc), R.string.sure, R.string.later, R.string.no_thanks, new CustomDialog.ClickListener() {
                @Override
                public void onPositive(int which) {
                    PreferenceManager.getDefaultSharedPreferences(DrawerActivity.this)
                            .edit().putBoolean("shown_rating_dialog", true).commit();
                    startActivity(new Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse("market://details?id=com.afollestad.cabinet")));
                }

                @Override
                public void onNegative() {
                    PreferenceManager.getDefaultSharedPreferences(DrawerActivity.this)
                            .edit().putBoolean("shown_rating_dialog", true).commit();
                }
            }).show(getFragmentManager(), "RATE_DIALOG");
        }
    }

    private void displayDisconnectPrompt() {
        CustomDialog.create(R.string.disconnect, getString(R.string.disconnect_prompt), R.string.yes, 0, R.string.no, new CustomDialog.ClickListener() {
            @Override
            public void onPositive(int which) {
                startService(new Intent(DrawerActivity.this, NetworkService.class)
                        .setAction(NetworkService.DISCONNECT_SFTP));
            }
        }).show(getFragmentManager(), "DISCONNECT_CONFIRM");
    }

    private void processIntent(Intent intent) {
        if (intent.hasExtra("remote")) {
            mRemoteSwitch = (CloudFile) intent.getSerializableExtra("remote");
            if (mNetworkService != null) {
                switchDirectory(mRemoteSwitch, true);
                displayDisconnectPrompt();
                mRemoteSwitch = null;
            }
        } else {
            if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("shown_welcome", false)) {
                getFragmentManager().beginTransaction().replace(R.id.container, new WelcomeFragment()).commit();
            } else {
                displayInfoDialog();
                switchDirectory(null, true);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, NetworkService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mConnection);
    }

    public void reloadNavDrawer() {
        ((NavigationDrawerFragment) getFragmentManager().findFragmentByTag("NAV_DRAWER")).reload();
    }

    public void switchDirectory(Shortcuts.Item to) {
        File file = to.toFile(this);
        boolean clearBackStack = file.isStorageDirectory();
        switchDirectory(file, clearBackStack);
    }

    public void switchDirectory(File to, boolean clearBackStack) {
        if (to == null) to = new LocalFile(this, Environment.getExternalStorageDirectory());
        canExit = false;
        if (clearBackStack)
            getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        FragmentTransaction trans = getFragmentManager().beginTransaction();
        trans.replace(R.id.container, DirectoryFragment.create(to));
        if (!clearBackStack) trans.addToBackStack(null);
        try {
            trans.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void search(File currentDir, String query) {
        getFragmentManager().beginTransaction().replace(R.id.container,
                DirectoryFragment.create(currentDir, query)).addToBackStack(null).commit();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            if (mFileCab != null && mFileCab.isActive()) {
                onBackPressed();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            if (canExit) super.onBackPressed();
            else {
                canExit = true;
                Toast.makeText(getApplicationContext(), R.string.press_back_to_exit, Toast.LENGTH_SHORT).show();
            }
        } else getFragmentManager().popBackStack();
    }

    public NetworkService getNetworkService() {
        return mNetworkService;
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            NetworkService.LocalBinder binder = (NetworkService.LocalBinder) service;
            mNetworkService = binder.getService();
            if (mRemoteSwitch != null) {
                switchDirectory(mRemoteSwitch, true);
                displayDisconnectPrompt();
                mRemoteSwitch = null;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    /* Donation stuff via in app billing */

    @Override
    public void onBillingInitialized() {
    }

    @Override
    public void onProductPurchased(String productId) {
        mBP.consumePurchase(productId);
        Toast.makeText(this, R.string.thank_you, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBillingError(int errorCode, Throwable error) {
        Toast.makeText(this, "Billing error: code = " + errorCode + ", error: " +
                (error != null ? error.getMessage() : "?"), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPurchaseHistoryRestored() {
        /*
         * Called then purchase history was restored and the list of all owned PRODUCT ID's
         * was loaded from Google Play
         */
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!mBP.handleActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);
    }

    public void donate(int index) {
        mBP.purchase("donation" + index);
    }

    @Override
    public void onDestroy() {
        if (mBP != null) mBP.release();
        super.onDestroy();
    }
}
