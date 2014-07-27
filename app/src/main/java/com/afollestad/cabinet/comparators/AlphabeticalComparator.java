package com.afollestad.cabinet.comparators;

import com.afollestad.cabinet.file.base.File;

/**
 * Sorts files and folders by name, alphabetically.
 *
 * @author Aidan Follestad (afollestad)
 */
public class AlphabeticalComparator implements java.util.Comparator<File> {

    @Override
    public int compare(File lhs, File rhs) {
        return lhs.getName().compareToIgnoreCase(rhs.getName());
    }
}