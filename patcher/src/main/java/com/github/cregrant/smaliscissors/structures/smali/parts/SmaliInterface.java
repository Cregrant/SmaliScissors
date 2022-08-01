package com.github.cregrant.smaliscissors.structures.smali.parts;

import com.github.cregrant.smaliscissors.structures.interfaces.ISmaliClassPart;
import com.github.cregrant.smaliscissors.structures.smali.SmaliClass;
import com.github.cregrant.smaliscissors.structures.smali.SmaliTarget;

public class SmaliInterface implements ISmaliClassPart {
    private String text;
    private final int end;
    private boolean deleted;

    public SmaliInterface(String string, int pos) {
        if (string.charAt(pos) == '#')
            deleted = true;
        end = string.indexOf("\n\n", pos) + 2;
        text = string.substring(pos, end);
    }

    public String clean(SmaliTarget target, SmaliClass smaliClass) {
        if (!deleted && text.contains(target.getRef())) {
            text = '#' + text;
            deleted = true;
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