package com.github.cregrant.smaliscissors.removecode.classparts;

import com.github.cregrant.smaliscissors.removecode.SmaliClass;
import com.github.cregrant.smaliscissors.removecode.SmaliTarget;

public class ClassInterface implements ClassPart {
    private String text;
    private int end;
    private boolean deleted;

    public ClassInterface(String string, int pos) {
        if (string.charAt(pos) == '#') {
            deleted = true;
        }
        end = string.indexOf("\n\n", pos) + 2;
        if (end == 1) {
            end = string.length();
        }
        text = string.substring(pos, end);
    }

    public SmaliTarget clean(SmaliTarget target, SmaliClass smaliClass) {
        if (!deleted && !target.isMethod() && text.contains(target.getRef())) {
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