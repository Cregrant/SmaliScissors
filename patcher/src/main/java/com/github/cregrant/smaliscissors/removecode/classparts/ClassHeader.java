package com.github.cregrant.smaliscissors.removecode.classparts;

import com.github.cregrant.smaliscissors.removecode.SmaliClass;
import com.github.cregrant.smaliscissors.removecode.SmaliTarget;

public class ClassHeader implements ClassPart {
    private String text;
    private int end;
    private boolean deleted;

    public ClassHeader(String string, int pos) {
        if (string.charAt(pos) == '#') {
            deleted = true;
        }
        end = string.indexOf("\n\n", pos) + 2;
        if (string.charAt(end) != '\n')     //there are 1 or 2 line breaks after header
        {
            end--;
        }
        text = string.substring(pos, end);
    }

    @Override
    public SmaliTarget clean(SmaliTarget target, SmaliClass smaliClass) {
        if (!deleted && !target.isMethod()) {
            int start = text.indexOf(".super") + 7;
            int targetPos = text.indexOf(target.getRef(), start);
            if (targetPos == -1 || targetPos > end) {
                return null;        //ignore .source line
            }

            end = text.indexOf(';', start);
            String superclass = text.substring(start, end + 1);
            text = text.substring(0, start) + "Ljava/lang/Object" + text.substring(end);
            if (!smaliClass.deleteSuperclass(superclass)) {
                SmaliTarget dep = new SmaliTarget();
                dep.setRef(smaliClass.getRef());
                return dep;
            }
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
