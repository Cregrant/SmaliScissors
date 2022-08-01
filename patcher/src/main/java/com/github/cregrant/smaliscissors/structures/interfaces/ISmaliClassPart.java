package com.github.cregrant.smaliscissors.structures.interfaces;

import com.github.cregrant.smaliscissors.structures.smali.SmaliClass;
import com.github.cregrant.smaliscissors.structures.smali.SmaliTarget;

public interface ISmaliClassPart {

    String clean(SmaliTarget target, SmaliClass smaliClass);

    int getEndPos();

    String getText();
}
