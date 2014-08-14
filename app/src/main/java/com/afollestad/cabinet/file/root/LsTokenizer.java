package com.afollestad.cabinet.file.root;

public class LsTokenizer {

    private String mLine;
    private int mIndex = 0;
    private boolean foundTime;

    public LsTokenizer(String line) {
        mLine = line;
    }

    public String nextToken() {
        if(mIndex == -1) return null;
        else if (mIndex == 0) {
            int endIndex = mLine.indexOf(' ');
            mIndex = endIndex;
            return mLine.substring(0, endIndex);
        }
        mIndex++;
        if (!foundTime && Character.isSpaceChar(mLine.charAt(mIndex)))
            return nextToken();
        int start = mIndex;
        if (foundTime) {
            if (mLine.indexOf("->", start) != -1) {
                // Represents a link, return everything before as name
                mIndex = mLine.indexOf("->", start);
                String token = mLine.substring(start, mIndex - 1);
                mIndex += 2;
                return token;
            } else {
                // Not a link, return the rest
                mIndex = -1;
                return mLine.substring(start, mLine.length());
            }
        }
        mIndex = mLine.indexOf(' ', start);
        String token = mLine.substring(start, mIndex);
        if (token.contains(":") && !foundTime)
            foundTime = true;
        return token;
    }
}
