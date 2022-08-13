package com.github.cregrant.smaliscissors.removecode.classparts;

import com.github.cregrant.smaliscissors.removecode.SmaliClass;
import com.github.cregrant.smaliscissors.removecode.SmaliTarget;

public interface ClassPart {

    SmaliTarget clean(SmaliTarget target, SmaliClass smaliClass);

    int getEndPos();

    String getText();
}
