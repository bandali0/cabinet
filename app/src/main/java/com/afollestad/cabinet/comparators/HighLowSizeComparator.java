package com.afollestad.cabinet.comparators;

import com.afollestad.cabinet.file.base.File;

/**
 * Sorts files and folders by size, from large to small. Folders are considered large.
 *
 * @author Aidan Follestad (afollestad)
 */
public class HighLowSizeComparator implements java.util.Comparator<File> {

    @Override
    public int compare(File lhs, File rhs) {
        if (lhs.isDirectory() && !rhs.isDirectory()) {
            // Folders before files
            return -1;
        } else if (lhs.isDirectory() && rhs.isDirectory() || !lhs.isDirectory() && !rhs.isDirectory()) {
            // Once folders and files are separate, sort by size
            if (rhs.length() < lhs.length()) {
                return -1; // move smaller files down
            } else if (rhs.length() > lhs.length()) {
                return 1; // move larger files up
            } else {
                return 0; // equal in size
            }
        } else if (!lhs.isDirectory() && rhs.isDirectory()) {
            // Files below folders
            return 1;
        } else return 0; // stay where it is now
    }
}