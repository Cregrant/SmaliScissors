package com.github.cregrant.smaliscissors.removecode.classparts;

import com.github.cregrant.smaliscissors.removecode.SmaliClass;
import com.github.cregrant.smaliscissors.removecode.SmaliTarget;

public class ClassHeader implements ClassPart {
    private String text;
    private String superclass;
    private int end;
    private boolean deleted;

    public ClassHeader(String string, int pos) {
        end = string.indexOf("\n\n", pos) + 2;
        if (end == 1) {
            if (string.contains("\r")) {
                throw new IllegalStateException("Bug detected - class body contains \"\\r\" symbols.");
            } else {
                end = string.length();      //class consists only of a header
            }
        }
        if (end < string.length() && string.charAt(end) != '\n') {
            end--;     //there are 1 or 2 line breaks after header
        }
        text = string.substring(pos, end);
        int start = text.indexOf(".super") + 7;
        superclass = text.substring(start, text.indexOf(';', start) + 1);
    }

    @Override
    public SmaliTarget clean(SmaliTarget target, SmaliClass smaliClass) {
        if (!deleted && target.isClass()) {
            if (!superclass.startsWith(target.getRef())) {
                return null;
            }

            text = text.replace(superclass, "Ljava/lang/Object;");
            superclass = "Ljava/lang/Object;";
            if (!smaliClass.changeSuperclass(superclass)) {
                return new SmaliTarget().setRef(smaliClass.getRef());
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
