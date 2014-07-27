package com.afollestad.cabinet.sftp;

public class FileNotExistsException extends Exception {

    public FileNotExistsException(String path) {
        super(path + " doesn't exist.");
    }
}
