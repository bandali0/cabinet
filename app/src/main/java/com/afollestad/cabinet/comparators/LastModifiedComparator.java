package com.afollestad.cabinet.comparators;

import com.afollestad.cabinet.file.base.File;

/**
 * @author Aidan Follestad (afollestad)
 */
public class LastModifiedComparator implements java.util.Comparator<File> {

    @Override
    public int compare(File lhs, File rhs) {
        return Long.valueOf(rhs.lastModified()).compareTo(lhs.lastModified());
    }
}