package com.github.cregrant.smaliscissors.removecode.classparts;

import com.github.cregrant.smaliscissors.removecode.SmaliClass;
import com.github.cregrant.smaliscissors.removecode.SmaliCleanResult;
import com.github.cregrant.smaliscissors.removecode.SmaliTarget;

import java.util.InputMismatchException;

public class ClassMetadata implements ClassPart {
    private final String text;
    private final int end;

    public ClassMetadata(String string, int pos) {
        end = string.indexOf('\n', pos + 1) + 1;
        if (end == 0) {
            throw new InputMismatchException("Cannot find the end of the metadata!");
        }
        text = string.substring(pos, end);
    }

    @Override
    public SmaliCleanResult clean(SmaliTarget target, SmaliClass smaliClass) {
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
}