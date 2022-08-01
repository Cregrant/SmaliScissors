package com.github.cregrant.smaliscissors.structures.smali.parts;

import com.github.cregrant.smaliscissors.structures.interfaces.ISmaliClassPart;
import com.github.cregrant.smaliscissors.structures.smali.SmaliClass;
import com.github.cregrant.smaliscissors.structures.smali.SmaliTarget;

public class SmaliField implements ISmaliClassPart {
    private String text;
    private int end;
    private boolean singleLine;
    private boolean deleted;

    public SmaliField(String string, int pos) {
        if (string.charAt(pos) == '#')
            deleted = true;
        int separatorPos = Math.min(string.indexOf('\n', pos) + 1, string.length() - 1);
        if (string.charAt(separatorPos) == '\n') {
            end = string.indexOf("\n\n", pos) + 2;      //one line field
            if (end == 1)
                end = string.length();
            text = string.substring(pos, end);
            singleLine = true;
        } else {
            end = string.indexOf(".end field\n\n", pos) + 12;      //field with annotations
            if (end == 11)
                end = string.length();
            text = string.substring(pos, end);
        }
    }

    @Override
    public String clean(SmaliTarget target, SmaliClass smaliClass) {
        if (!deleted && text.contains(target.getRef())) {
            deleted = true;
            if (singleLine)
                text = '#' + text;
            else {
                StringBuilder sb = new StringBuilder(text);
                int i = -1;
                do {
                    if (sb.charAt(i + 1) != '\n')
                        sb.insert(i + 1, '#');
                    i = sb.indexOf("\n", i + 1);
                } while (i != -1 && i + 1 < sb.length());
                text = sb.toString();
            }
        }
        return null;
    }

    @Override
    public int getEndPos() {
        return end;
    }

    @Override
    public String getText() {
        return text;
    }
}
