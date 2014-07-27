package com.afollestad.cabinet.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome, null);
    }

    private View fileCard;
    private PopupMenu menu;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewStub fileStub = (ViewStub) view.findViewById(R.id.fileCardStub);
        fileCard = fileStub.inflate();

        ImageView icon = (ImageView) fileCard.findViewById(android.R.id.icon);
        icon.setImageResource(R.drawable.android_logo);
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fileCard.setActivated(!view.isActivated());
            }
        });

        TextView title = (TextView) fileCard.findViewById(android.R.id.title);
        title.setText(R.string.file_sub_title);
        title.setTextColor(getResources().getColor(R.color.accent_color));

        ((TextView) fileCard.findViewById(android.R.id.content)).setText(R.string.file_stub_content);

        View menuButton = fileCard.findViewById(android.R.id.button1);
        Context context = getActivity();
        context.setTheme(android.R.style.Theme_Holo_Light);
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
                ((DrawerActivity) getActivity()).switchDirectory(null, true);
            }
        });

        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean("shown_welcome", true).commit();
    }
}
