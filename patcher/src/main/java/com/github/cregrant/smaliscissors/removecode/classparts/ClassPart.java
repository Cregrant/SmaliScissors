package com.github.cregrant.smaliscissors.removecode.classparts;

import com.github.cregrant.smaliscissors.removecode.SmaliClass;
import com.github.cregrant.smaliscissors.removecode.SmaliCleanResult;
import com.github.cregrant.smaliscissors.removecode.SmaliTarget;

public interface ClassPart {

    SmaliCleanResult clean(SmaliTarget target, SmaliClass smaliClass);

    void makeStub(SmaliClass smaliClass);     //save the class signature and delete other unnecessary things

    int getEndPos();

    String getText();
}
