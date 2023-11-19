package com.github.cregrant.smaliscissors.removecode.classparts;

import com.github.cregrant.smaliscissors.removecode.SmaliClass;
import com.github.cregrant.smaliscissors.removecode.SmaliCleanResult;
import com.github.cregrant.smaliscissors.removecode.SmaliTarget;

public class ClassAnnotation implements ClassPart {
    private final String text;
    private final int end;
    private boolean deleted;

    public ClassAnnotation(String string, int pos) {
        if (string.charAt(pos) == '#') {
            deleted = true;
        }
        end = string.indexOf("\n.end annotation", pos) + 18;
        if (end == 17) {
            throw new IllegalArgumentException();
        }
        text = string.substring(pos, Math.min(end, string.length()));
    }

    @Override
    public SmaliCleanResult clean(SmaliTarget target, SmaliClass smaliClass) {
        //if (text.contains(target))    //todo clean annotations?
        return null;
    }

    @Override
    public void makeStub(SmaliClass smaliClass) {
        //text = '#' + text;
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
