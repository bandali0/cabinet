package com.afollestad.cabinet.comparators;

import com.afollestad.cabinet.file.base.File;

/**
 * Sorts files and folders by size, from small to large. Folders are considered large.
 *
 * @author Aidan Follestad (afollestad)
 */
public class LowHighSizeComparator implements java.util.Comparator<File> {

    @Override
    public int compare(File lhs, File rhs) {
        if (lhs.isDirectory() && !rhs.isDirectory()) {
            // Folders after files
            return 1;
        } else if (lhs.isDirectory() && rhs.isDirectory() || !lhs.isDirectory() && !rhs.isDirectory()) {
            // Once folders and files are separate, sort by size
            if (rhs.length() < lhs.length()) {
                return 1; // move smaller files up
            } else if (rhs.length() > lhs.length()) {
                return -1; // move larger files down
            } else {
                return 0; // equal in size
            }
        } else if (!lhs.isDirectory() && rhs.isDirectory()) {
            // Files above folders
            return -1;
        } else return 0; // stay where it is now
    }
}