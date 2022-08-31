package com.github.cregrant.smaliscissors.removecode.classparts;

import com.github.cregrant.smaliscissors.removecode.SmaliClass;
import com.github.cregrant.smaliscissors.removecode.SmaliTarget;

public class ClassHeader implements ClassPart {
    private String text;
    private final String superclass;
    private int end;
    private boolean deleted;

    public ClassHeader(String string, int pos) {
        if (string.charAt(pos) == '#') {
            deleted = true;
        }
        end = string.indexOf("\n\n", pos) + 2;
        if (end == 1) {
            throw new IllegalStateException("Bug detected - class body contains \"\\r\" symbols.");
        }
        if (string.charAt(end) != '\n') {
            end--;     //there are 1 or 2 line breaks after header
        }
        text = string.substring(pos, end);
        int start = text.indexOf(".super") + 7;
        superclass = text.substring(start, text.indexOf(';', start) + 1);
    }

    @Override
    public SmaliTarget clean(SmaliTarget target, SmaliClass smaliClass) {
        if (!deleted && !target.isMethod()) {
            int start = text.indexOf(".super") + 7;
            if (!text.startsWith(target.getRef(), start))
                return null;

            text = text.replace(getSuperclass(), "Ljava/lang/Object;");
            if (!smaliClass.changeSuperclass(superclass)) {
                SmaliTarget dep = new SmaliTarget();
                dep.setRef(smaliClass.getRef());
                return dep;
            }
            deleted = true;
        }
        return null;
    }

    @Override
    public void makeStub(SmaliClass smaliClass) {
    }

    @Override
    public int getEndPos() {
        return end;
    }

    @Override
    public String getText() {
        return text;
    }

    public String getSuperclass() {
        return superclass;
    }
}
