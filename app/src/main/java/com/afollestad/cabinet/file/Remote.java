package com.afollestad.cabinet.file;

import java.io.Serializable;

public class Remote implements Serializable {

    public Remote(String host, int port, String user, String pass) {
        mHost = host;
        mPort = port;
        mUser = user;
        mPass = pass;
    }

    private final String mHost;
    private final int mPort;
    private final String mUser;
    private final String mPass;

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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Remote)) return false;
        Remote other = (Remote) o;
        return getHost().equals(other.getHost()) &&
                getPort() == other.getPort() &&
                getUser().equals(other.getUser()) &&
                getPass().equals(other.getPass());
    }
}
