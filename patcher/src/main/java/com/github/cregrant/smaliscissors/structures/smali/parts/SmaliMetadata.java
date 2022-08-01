package com.github.cregrant.smaliscissors.structures.smali.parts;

import com.github.cregrant.smaliscissors.structures.interfaces.ISmaliClassPart;
import com.github.cregrant.smaliscissors.structures.smali.SmaliClass;
import com.github.cregrant.smaliscissors.structures.smali.SmaliTarget;

public class SmaliMetadata implements ISmaliClassPart {
    private final String text;
    private final int end;

    public SmaliMetadata(String string, int pos) {
        end = string.indexOf('\n', pos + 1) + 1;
        if (end == 0)
            throw new IllegalArgumentException();
        text = string.substring(pos, end);
    }

    @Override
    public String clean(SmaliTarget target, SmaliClass smaliClass) {
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