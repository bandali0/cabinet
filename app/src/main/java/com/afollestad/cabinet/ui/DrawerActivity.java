package com.afollestad.cabinet.ui;

import android.app.ActionBar;
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
import android.widget.Toast;

import com.afollestad.cabinet.R;
import com.afollestad.cabinet.adapters.NavigationDrawerAdapter;
import com.afollestad.cabinet.cab.base.BaseFileCab;
import com.afollestad.cabinet.file.CloudFile;
import com.afollestad.cabinet.file.LocalFile;
import com.afollestad.cabinet.file.base.File;
import com.afollestad.cabinet.fragments.DirectoryFragment;
import com.afollestad.cabinet.fragments.NavigationDrawerFragment;
import com.afollestad.cabinet.fragments.WelcomeFragment;
import com.afollestad.cabinet.services.NetworkService;
import com.afollestad.cabinet.utils.Shortcuts;
import com.afollestad.silk.dialogs.SilkDialog;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.readystatesoftware.systembartint.SystemBarTintManager;

public class DrawerActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, BillingProcessor.IBillingHandler {

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private CharSequence mTitle;

    private BillingProcessor mBP;
    private NavigationDrawerAdapter drawerAdapter;
    private boolean canExit;
    private BaseFileCab mFileCab;
    private NetworkService mNetworkService;
    private CloudFile mRemoteSwitch;

    public static void setupTransparentTints(Activity context) {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.KITKAT) return;
        SystemBarTintManager tintManager = new SystemBarTintManager(context);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setStatusBarTintResource(R.color.cabinet_color);
    }

    public static void setupTranslucentPadding(Activity context, View view) {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.KITKAT) return;
        SystemBarTintManager tintManager = new SystemBarTintManager(context);
        SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
        view.setPadding(view.getPaddingLeft(), config.getPixelInsetTop(true), view.getPaddingRight(), view.getPaddingBottom());
    }

    public BaseFileCab getFileCab() {
        return mFileCab;
    }

    public void setFileCab(BaseFileCab cab) {
        mFileCab = cab;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        setupTransparentTints(this);
        setupTranslucentPadding(this, findViewById(R.id.container));

        mBP = new BillingProcessor(this, "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlPBB2hP/R0PrXtK8NPeDX7QV1fvk1hDxPVbIwRZLIgO5l/ZnAOAf8y9Bq57+eO5CD+ZVTgWcAVrS/QsiqDI/MwbfXcDydSkZLJoFofOFXRuSL7mX/jNwZBNtH0UrmcyFx1RqaHIe9KZFONBWLeLBmr47Hvs7dKshAto2Iy0v18kN48NqKxlWtj/PHwk8uIQ4YQeLYiXDCGhfBXYS861guEr3FFUnSLYtIpQ8CiGjwfU60+kjRMmXEGnmhle5lqzj6QeL6m2PNrkbJ0T9w2HM+bR7buHcD8e6tHl2Be6s/j7zn1Ypco/NCbqhtPgCnmLpeYm8EwwTnH4Yei7ACR7mXQIDAQAB", this);
        processIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processIntent(intent);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {

    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    private void displayRatingDialog() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean("shown_rating_dialog", false)) {
            SilkDialog.create(this)
                    .setAccentColorRes(R.color.cabinet_color)
                    .setTitle(R.string.rate)
                    .setMessage(R.string.rate_desc)
                    .setNeutralButtonText(R.string.later)
                    .setPostiveButtonText(R.string.sure)
                    .setNegativeButtonText(R.string.no_thanks)
                    .setButtonListener(new SilkDialog.DialogCallback() {
                        @Override
                        public void onPositive() {
                            PreferenceManager.getDefaultSharedPreferences(DrawerActivity.this)
                                    .edit().putBoolean("shown_rating_dialog", true).commit();
                            startActivity(new Intent(Intent.ACTION_VIEW)
                                    .setData(Uri.parse("market://details?id=com.afollestad.cabinet")));
                        }

                        @Override
                        public void onCancelled() {
                            PreferenceManager.getDefaultSharedPreferences(DrawerActivity.this)
                                    .edit().putBoolean("shown_rating_dialog", true).commit();
                        }
                    }).show(false);
        }
    }

    private void displayDisconnectPrompt() {
        SilkDialog.create(this)
                .setAccentColorRes(R.color.cabinet_color)
                .setTitle(R.string.disconnect)
                .setMessage(R.string.disconnect_promp, mRemoteSwitch.getRemote().getHost())
                .setPostiveButtonText(R.string.yes)
                .setNegativeButtonText(R.string.no)
                .setButtonListener(new SilkDialog.DialogCallback() {
                    @Override
                    public void onPositive() {
                        startService(new Intent(DrawerActivity.this, NetworkService.class)
                                .setAction(NetworkService.DISCONNECT_SFTP));
                    }
                })
                .show(true);
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
                displayRatingDialog();
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
        trans.commit();
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
