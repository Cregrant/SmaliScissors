package com.github.cregrant.smaliscissors.common.decompiledfiles;

import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.removecode.SmaliClass;

public class SmaliFile extends DecompiledFile {

    private SmaliClass smaliClass;

    public SmaliFile(Project project, String filePath) {
        super(project, filePath);
    }

    public SmaliClass getSmaliClass() {
        return smaliClass;
    }

    public void setSmaliClass(SmaliClass smaliClass) {
        this.smaliClass = smaliClass;
    }
}
