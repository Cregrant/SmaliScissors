package com.github.cregrant.smaliscissors.structures.smali.parts;

import com.github.cregrant.smaliscissors.structures.interfaces.ISmaliClassPart;
import com.github.cregrant.smaliscissors.structures.smali.SmaliClass;
import com.github.cregrant.smaliscissors.structures.smali.SmaliTarget;

public class SmaliAnnotation implements ISmaliClassPart {
    private final String text;
    private final int end;
    private boolean deleted;

    public SmaliAnnotation(String string, int pos) {
        if (string.charAt(pos) == '#')
            deleted = true;
        end = string.indexOf("\n.end annotation", pos) + 18;
        if (end == 17)
            throw new IllegalArgumentException();
        text = string.substring(pos, Math.min(end, string.length()));
    }

    @Override
    public String clean(SmaliTarget target, SmaliClass smaliClass) {
        //if (text.contains(target))    //todo clean annotations?
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
