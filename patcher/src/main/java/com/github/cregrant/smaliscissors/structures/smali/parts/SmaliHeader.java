package com.github.cregrant.smaliscissors.structures.smali.parts;

import com.github.cregrant.smaliscissors.structures.interfaces.ISmaliClassPart;
import com.github.cregrant.smaliscissors.structures.smali.SmaliClass;
import com.github.cregrant.smaliscissors.structures.smali.SmaliTarget;

public class SmaliHeader implements ISmaliClassPart {
    private String text;
    private int end;
    private boolean deleted;

    public SmaliHeader(String string, int pos) {
        if (string.charAt(pos) == '#')
            deleted = true;
        end = string.indexOf("\n\n", pos) + 2;
        if (string.charAt(end) != '\n')     //there are 1 or 2 line breaks after header
            end--;
        text = string.substring(pos, end);
    }

    @Override
    public String clean(SmaliTarget target, SmaliClass smaliClass) {
        if (!deleted && text.contains(target.getRef())) {
            int start = text.indexOf(".super") + 7;
            int end = text.indexOf(';', start);
            String deletedClass = text.substring(start, end + 1);
            text = text.substring(0, start) + "Ljava/lang/Object" + text.substring(end);
            smaliClass.setDeletedSuperClass(deletedClass);
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
