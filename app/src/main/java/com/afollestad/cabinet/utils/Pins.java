package com.afollestad.cabinet.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.afollestad.cabinet.file.CloudFile;
import com.afollestad.cabinet.file.LocalFile;
import com.afollestad.cabinet.file.Remote;
import com.afollestad.cabinet.file.base.File;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Pins {

    public static class Item {

        public Item(File file) {
            mPath = file.getPath();
            if (file.isRemote()) {
                isRemote = true;
                Remote remote = ((CloudFile) file).getRemote();
                mHost = remote.getHost();
                mPort = remote.getPort();
                mUser = remote.getUser();
                mPass = remote.getPass();
            }
        }

        public Item(String host, int port, String user, String pass, String path) {
            isRemote = true;
            mHost = host;
            mPort = port;
            mUser = user;
            mPass = pass;
            mPath = path;
        }

        public Item(JSONObject json) {
            mPath = json.optString("path");
            if (json.optBoolean("remote")) {
                isRemote = true;
                mHost = json.optString("host");
                mPort = json.optInt("port");
                mUser = json.optString("user");
                mPass = json.optString("pass");
            }
        }

        private boolean isRemote;
        private final String mPath;
        private String mHost;
        private int mPort;
        private String mUser;
        private String mPass;

        public boolean isRemote() {
            return isRemote;
        }

        public String getPath() {
            return mPath;
        }

        public String getHost() {
            return mHost;
        }

        public int getPort() {
            return mPort;
        }

        public String getUser() {
            return mUser;
        }

        public String getPass() {
            return mPass;
        }

        public String getDisplay(Activity context) {
            if (isRemote()) {
                return getHost() + getPath();
            } else {
                return new LocalFile(context, getPath()).getDisplay();
            }
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            try {
                json.put("path", mPath);
                json.put("remote", isRemote);
                if (isRemote) {
                    json.put("host", mHost);
                    json.put("port", mPort);
                    json.put("user", mUser);
                    json.put("pass", mPass);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return json;
        }

        @Override
        public String toString() {
            return toJSON().toString();
        }

        public File toFile(Activity context) {
            if (isRemote) {
                return new CloudFile(context, getPath(), new Remote(mHost, mPort, mUser, mPass));
            } else {
                return new LocalFile(context, getPath());
            }
        }
    }

    public static void save(Context context, List<Item> items) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        JSONArray toSet = new JSONArray();
        for (Item item : items) {
            toSet.put(item.toJSON());
        }
        prefs.edit().putString("pins", toSet.toString()).commit();
    }

    public static void add(Context context, Item item) {
        List<Item> items = getAll(context);
        items.add(item);
        save(context, items);
    }

    public static List<Item> getAll(Context context) {
        List<Item> items = new ArrayList<Item>();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String shortcuts = prefs.getString("pins", null);
        if (shortcuts == null) return items;
        try {
            JSONArray shortcutsJson = new JSONArray(shortcuts);
            for (int i = 0; i < shortcutsJson.length(); i++) {
                items.add(new Item(shortcutsJson.getJSONObject(i)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }

    public static boolean contains(Context context, Item item) {
        List<Item> shortcuts = getAll(context);
        for (Item i : shortcuts) {
            if (i.toString().equals(item.toString())) return true;
        }
        return false;
    }

    public static void remove(Context context, int index) {
        List<Item> items = getAll(context);
        items.remove(index);
        save(context, items);
    }

    public static boolean remove(Activity context, File file) {
        List<Item> items = getAll(context);
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            if (item.toFile(context).equals(file)) {
                remove(context, i);
                return true;
            }
        }
        return false;
    }
}