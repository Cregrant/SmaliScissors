package com.github.cregrant.smaliscissors.removecode.classparts;

import com.github.cregrant.smaliscissors.removecode.SmaliClass;
import com.github.cregrant.smaliscissors.removecode.SmaliTarget;

public class ClassField implements ClassPart {
    private String text;
    private int end;
    private boolean singleLine;
    private boolean deleted;

    public ClassField(String string, int pos) {
        if (string.charAt(pos) == '#') {
            deleted = true;
        }
        int separatorPos = Math.min(string.indexOf('\n', pos) + 1, string.length() - 1);
        if (string.charAt(separatorPos) == '\n') {
            end = string.indexOf("\n\n", pos) + 2;      //one line field
            if (end == 1) {
                end = string.length();
            }
            text = string.substring(pos, end);
            singleLine = true;
        } else {
            end = string.indexOf(".end field\n\n", pos) + 12;      //field with annotations
            if (end == 11) {
                end = string.length();
            }
            text = string.substring(pos, end);
        }
    }

    @Override
    public SmaliTarget clean(SmaliTarget target, SmaliClass smaliClass) {
        if (!deleted && !target.isMethod() && text.contains(target.getRef())) {
            return delete(smaliClass);
        }
        return null;
    }

    @Override
    public void makeStub(SmaliClass smaliClass) {
        delete(smaliClass);
    }

    private SmaliTarget delete(SmaliClass smaliClass) {
        deleted = true;
        int end = text.indexOf(";") + 1;
        SmaliTarget target = new SmaliTarget();
        target.setRef(smaliClass.getRef() + "->" + text.substring(text.lastIndexOf(" ", end - 1) + 1, end));
        if (singleLine) {
            text = '#' + text;
        } else {
            StringBuilder sb = new StringBuilder(text);
            int i = -1;
            do {
                if (sb.charAt(i + 1) != '\n') {
                    sb.insert(i + 1, '#');
                }
                i = sb.indexOf("\n", i + 1);
            } while (i != -1 && i + 1 < sb.length());
            text = sb.toString();
        }
        return target;
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
