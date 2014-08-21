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
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.afollestad.cabinet.R;
import com.afollestad.cabinet.cab.PickerCab;
import com.afollestad.cabinet.cab.base.BaseCab;
import com.afollestad.cabinet.cab.base.BaseFileCab;
import com.afollestad.cabinet.file.CloudFile;
import com.afollestad.cabinet.file.LocalFile;
import com.afollestad.cabinet.file.base.File;
import com.afollestad.cabinet.fragments.CustomDialog;
import com.afollestad.cabinet.fragments.DirectoryFragment;
import com.afollestad.cabinet.fragments.NavigationDrawerFragment;
import com.afollestad.cabinet.fragments.WelcomeFragment;
import com.afollestad.cabinet.services.NetworkService;
import com.afollestad.cabinet.utils.Pins;
import com.afollestad.cabinet.utils.ThemeUtils;
import com.afollestad.cabinet.utils.Utils;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.faizmalkani.floatingactionbutton.FloatingActionButton;
import com.readystatesoftware.systembartint.SystemBarTintManager;

public class DrawerActivity extends Activity implements BillingProcessor.IBillingHandler {

    public interface FabListener {
        public abstract void onFabPressed(BaseFileCab.PasteMode pasteMode);
    }

    private BillingProcessor mBP; // used for donations
    private boolean canExit; // flag used for press back twice to exit
    private BaseCab mCab; // the current contextual action bar, saves state throughout fragments
    private NetworkService mNetworkService;
    private CloudFile mRemoteSwitch; // used by SFTP notification intent
    private ThemeUtils mThemeUtils; // used to detect theme changes and get current theme

    public FloatingActionButton fab; // the floating blue add/paste button
    private float fabVisibleY; // saves y position of the top of the visible fab
    private float fabHiddenY; // saves y position of the top of the hidden fab
    private boolean fabShown = true; // flag indicating whether the fab is currently visible
    private FabListener mFabListener; // a callback used to notify DirectoryFragment of fab press
    public BaseFileCab.PasteMode fabPasteMode = BaseFileCab.PasteMode.DISABLED;
    private boolean fabDisabled; // flag indicating whether fab should stay hidden while scrolling
    public boolean shouldAttachFab; // used during config change, tells fragment to reattach to cab
    public boolean pickMode; // flag indicating whether user is picking a file for another app
    public DrawerLayout mDrawerLayout;

    // Both fields used in waitFabInvalidate() so that they can be initialized on UI thread
    private SystemBarTintManager mTintManager;
    SystemBarTintManager.SystemBarConfig mTintConfig;

    public static void setupTransparentTints(Activity context) {
        if (!ThemeUtils.isTranslucentStatusbar(context)) return;
        SystemBarTintManager tintManager = new SystemBarTintManager(context);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setStatusBarTintResource(ThemeUtils.isTrueBlack(context) ? R.color.cabinet_gray_darker : R.color.cabinet_color_darker);
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
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (view.getLayoutParams() instanceof FrameLayout.LayoutParams) {
            ((FrameLayout.LayoutParams) params).bottomMargin = config.getPixelInsetBottom();
        } else {
            ((RelativeLayout.LayoutParams) params).bottomMargin = config.getPixelInsetBottom();
        }
        view.setLayoutParams(params);
    }

    public BaseCab getCab() {
        return mCab;
    }

    public void setCab(BaseCab cab) {
        mCab = cab;
    }

    public void invalidateSystemBarTintManager() {
        if (mTintManager == null)
            mTintManager = new SystemBarTintManager(DrawerActivity.this);
        if (mTintConfig == null)
            mTintConfig = mTintManager.getConfig();
    }

    public void waitFabInvalidate() {
        if (mTintManager == null || mTintConfig == null)
            throw new RuntimeException("Tint manager and config have not be initialized yet.");
        final float translation = getResources().getDimension(R.dimen.fab_translation) + mTintConfig.getPixelInsetBottom();
        while (fabVisibleY == 0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fabVisibleY = fab.getY();
                    fabHiddenY = fab.getY() + translation;
                }
            });
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // Make sure memory is released
        mTintManager = null;
        mTintConfig = null;
    }

    public void toggleFab(boolean hide) {
        if (fabVisibleY == 0) {
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
            float translation = getResources().getDimension(R.dimen.fab_translation) + config.getPixelInsetBottom();
            fabVisibleY = fab.getY();
            fabHiddenY = fab.getY() + translation;
            Log.v("Fab", "Invalidate position– top: " + fabVisibleY + ", bottom: " + fabHiddenY);
        }
        if (hide) {
            if (fabShown) {
                ObjectAnimator outAnim = ObjectAnimator.ofFloat(fab, "y", fabVisibleY, fabHiddenY);
                outAnim.setDuration(250);
                outAnim.setInterpolator(new AccelerateInterpolator());
                outAnim.start();
                fabShown = false;
            }
        } else {
            if (!fabShown && !fabDisabled) {
                ObjectAnimator inAnim = ObjectAnimator.ofFloat(fab, "y", fabHiddenY, fabVisibleY);
                inAnim.setDuration(250);
                inAnim.setInterpolator(new DecelerateInterpolator());
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

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (mCab != null && mCab.isActive())
            outState.putSerializable("cab", mCab);
        outState.putSerializable("fab_pastemode", fabPasteMode);
        outState.putBoolean("fab_disabled", fabDisabled);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mThemeUtils = new ThemeUtils(this);
        setTheme(mThemeUtils.getCurrent());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("cab")) {
                mCab = (BaseCab) savedInstanceState.getSerializable("cab");
                if (mCab instanceof BaseFileCab) {
                    shouldAttachFab = true;
                } else {
                    if (mCab instanceof PickerCab) pickMode = true;
                    mCab.setContext(this).start();
                }
            }
            fabPasteMode = (BaseFileCab.PasteMode) savedInstanceState.getSerializable("fab_pastemode");
            fabDisabled = savedInstanceState.getBoolean("fab_disabled");
        }

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationDrawerFragment mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, mDrawerLayout, savedInstanceState == null);

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
                Toast.makeText(DrawerActivity.this, fabPasteMode == BaseFileCab.PasteMode.ENABLED ? R.string.paste : R.string.newStr, Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        setupTranslucentBottomMargin(this, fab);
        setupTransparentTints(this);

        mBP = new BillingProcessor(this, "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlPBB2hP/R0PrXtK8NPeDX7QV1fvk1hDxPVbIwRZLIgO5l/ZnAOAf8y9Bq57+eO5CD+ZVTgWcAVrS/QsiqDI/MwbfXcDydSkZLJoFofOFXRuSL7mX/jNwZBNtH0UrmcyFx1RqaHIe9KZFONBWLeLBmr47Hvs7dKshAto2Iy0v18kN48NqKxlWtj/PHwk8uIQ4YQeLYiXDCGhfBXYS861guEr3FFUnSLYtIpQ8CiGjwfU60+kjRMmXEGnmhle5lqzj6QeL6m2PNrkbJ0T9w2HM+bR7buHcD8e6tHl2Be6s/j7zn1Ypco/NCbqhtPgCnmLpeYm8EwwTnH4Yei7ACR7mXQIDAQAB", this);
        processIntent(getIntent(), savedInstanceState);
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
        processIntent(intent, null);
    }

    private final static String MATERIAL_PROMPT = "material_version_prompt";

    private void checkMaterialAndRating() {
//        checkRating();
        // TODO toggle commented area for Material
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getInt(MATERIAL_PROMPT, -1) != Utils.getVersion(this) && Build.VERSION.SDK_INT >= 20) {
            CustomDialog.create(this, R.string.material_version, getString(R.string.material_version_desc), R.string.yes, R.string.later, R.string.no, new CustomDialog.ClickListener() {
                @Override
                public void onPositive(int which, View view) {
                    PreferenceManager.getDefaultSharedPreferences(DrawerActivity.this)
                            .edit().putInt(MATERIAL_PROMPT, Utils.getVersion(DrawerActivity.this)).commit();
                    startActivity(new Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse("https://plus.google.com/u/0/communities/110440751142118056139")));
                }

                @Override
                public void onNeutral() {
                }

                @Override
                public void onNegative() {
                    PreferenceManager.getDefaultSharedPreferences(DrawerActivity.this)
                            .edit().putInt(MATERIAL_PROMPT, Utils.getVersion(DrawerActivity.this)).commit();
                }
            }).show(getFragmentManager(), "MATERIAL_DIALOG");
        } else checkRating();
    }

    private void checkRating() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean("shown_rating_dialog", false)) {
            CustomDialog.create(this, R.string.rate, getString(R.string.rate_desc), R.string.sure, R.string.later, R.string.no_thanks, new CustomDialog.ClickListener() {
                @Override
                public void onPositive(int which, View view) {
                    PreferenceManager.getDefaultSharedPreferences(DrawerActivity.this)
                            .edit().putBoolean("shown_rating_dialog", true).commit();
                    startActivity(new Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse("market://details?id=com.afollestad.cabinet")));
                }

                @Override
                public void onNeutral() {
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
        String host = getString(R.string.unknown);
        if (mNetworkService != null) {
            host = mNetworkService.getRemote().getRemote().getHost();
        }
        CustomDialog.create(this, R.string.disconnect, getString(R.string.disconnect_prompt, host),
                R.string.yes, 0, R.string.no, new CustomDialog.SimpleClickListener() {
                    @Override
                    public void onPositive(int which, View view) {
                        startService(new Intent(DrawerActivity.this, NetworkService.class)
                                .setAction(NetworkService.DISCONNECT_SFTP));
                    }
                }
        ).show(getFragmentManager(), "DISCONNECT_CONFIRM");
    }

    private void processIntent(Intent intent, Bundle savedInstanceState) {
        pickMode = intent.getAction() != null && intent.getAction().equals(Intent.ACTION_GET_CONTENT);
        if (pickMode) {
            setCab(new PickerCab().setContext(this).start());
        }
        if (intent.hasExtra("remote")) {
            mRemoteSwitch = (CloudFile) intent.getSerializableExtra("remote");
            if (mNetworkService != null) {
                switchDirectory(mRemoteSwitch, true);
                displayDisconnectPrompt();
                mRemoteSwitch = null;
            }
        } else if (savedInstanceState == null) {
            if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("shown_welcome", false)) {
                getFragmentManager().beginTransaction().replace(R.id.container, new WelcomeFragment()).commit();
            } else {
                checkMaterialAndRating();
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

    public void reloadNavDrawer(boolean open) {
        ((NavigationDrawerFragment) getFragmentManager().findFragmentByTag("NAV_DRAWER")).reload(open);
    }

    public void reloadNavDrawer() {
        reloadNavDrawer(false);
    }

    public void switchDirectory(Pins.Item to) {
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
        if (!clearBackStack) trans.setCustomAnimations(R.anim.frag_enter, R.anim.frag_exit);
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
            if (mCab != null && mCab.isActive()) {
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
        if (errorCode != 110) {
            Toast.makeText(this, "Billing error: code = " + errorCode + ", error: " +
                    (error != null ? error.getMessage() : "?"), Toast.LENGTH_LONG).show();
        }
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
