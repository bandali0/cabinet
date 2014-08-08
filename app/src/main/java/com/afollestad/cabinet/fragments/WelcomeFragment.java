package com.afollestad.cabinet.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.afollestad.cabinet.R;
import com.afollestad.cabinet.ui.DrawerActivity;

/**
 * @author Aidan Follestad (afollestad)
 */
public class WelcomeFragment extends Fragment {

    @Override
    public void onDetach() {
        ((DrawerActivity) getActivity()).disableFab(false);
        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome, null);
    }

    private View fileCard;
    private PopupMenu menu;
    private ImageView icon;

    @Override
    public void onResume() {
        super.onResume();
        new Thread(new Runnable() {
            @Override
            public void run() {
                ((DrawerActivity) getActivity()).waitFabInvalidate();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((DrawerActivity) getActivity()).disableFab(true);
                    }
                });
            }
        }).start();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View root = view.findViewById(R.id.root);
        DrawerActivity.setupTranslucentTopPadding(getActivity(), root);
        DrawerActivity.setupTranslucentBottomPadding(getActivity(), root);
        ViewStub fileStub = (ViewStub) view.findViewById(R.id.fileCardStub);
        fileCard = fileStub.inflate();

        icon = (ImageView) fileCard.findViewById(R.id.image);
        icon.setImageResource(R.drawable.android_logo);
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fileCard.setActivated(!view.isActivated());
            }
        });

        fileCard.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                icon.performClick();
                return true;
            }
        });

        TextView title = (TextView) fileCard.findViewById(android.R.id.title);
        title.setText(R.string.file_sub_title);
        title.setTextColor(getResources().getColor(R.color.accent_color));

        ((TextView) fileCard.findViewById(android.R.id.content)).setText(R.string.file_stub_size);
        ((TextView) fileCard.findViewById(android.R.id.summary)).setText(R.string.file_stub_content);

        View menuButton = fileCard.findViewById(R.id.menu);
        ContextThemeWrapper context = new ContextThemeWrapper(getActivity(), R.style.Theme_PopupMenuTheme);
        menu = new PopupMenu(context, menuButton);
        menu.inflate(R.menu.file_options);
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menu.show();
            }
        });

        view.findViewById(R.id.finish).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean("shown_welcome", true).commit();
                ((DrawerActivity) getActivity()).switchDirectory(null, true);
            }
        });
    }
}
