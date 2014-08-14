package com.afollestad.cabinet.file.root;

import android.app.Activity;

import com.afollestad.cabinet.file.base.File;
import com.afollestad.cabinet.file.base.FileFilter;

import java.util.ArrayList;
import java.util.List;

public class LsParser {

    private Activity mContext;
    private String mPath;
    private FileFilter mFilter;
    private List<File> mFiles;
    public RootFile file;
    private boolean includeHidden;

    private LsParser() {
        mFiles = new ArrayList<File>();
    }

    public static LsParser parse(Activity context, String path, List<String> response, FileFilter filter, boolean includeHidden) {
        if (path.equals("/")) path = "";
        LsParser parser = new LsParser();
        parser.mContext = context;
        parser.mPath = path;
        parser.mFilter = filter;
        parser.includeHidden = includeHidden;
        for (String line : response) {
            parser.processLine(line);
        }
        return parser;
    }

    protected void processLine(String line) {
        file = new RootFile(mContext);
        LsTokenizer tokenizer = new LsTokenizer(line);
        int index = 0;
        String token;
        while ((token = tokenizer.nextToken()) != null) {
            if (index == 0) file.permissions = token;
            else if (index == 1) file.owner = token;
            else if (index == 2) file.creator = token;
            else if (index == 3) {
                if (token.contains("-")) {
                    file.date = token;
                    index++; // since there's no size, skip to next token
                } else {
                    file.size = Long.parseLong(token);
                }
            } else if (index == 4) {
                file.date = token;
            } else if (index == 5) {
                file.time = token;
            } else if (index == 6) {
                // Store the original name for the case that this is a link (name is displayed but path is different)
                file.originalName = token;
                file.setPath(mPath + "/" + token);
            } else {
                file.setPath(token); // this is a link to another file/folder
            }
            index++;
        }
        boolean skip = includeHidden && file.getName().startsWith(".");
        if ((mFilter == null || mFilter.accept(file)) && !skip)
            mFiles.add(file);
    }

    public List<File> getFiles() {
        return mFiles;
    }
}