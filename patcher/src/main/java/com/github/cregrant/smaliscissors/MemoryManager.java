package com.github.cregrant.smaliscissors;

import com.github.cregrant.smaliscissors.common.decompiledfiles.DecompiledFile;

public class MemoryManager {
    private final Project project;

    public MemoryManager(Project project) {
        this.project = project;
    }

    void tryEnableCache() {
        long max = Runtime.getRuntime().maxMemory() - 40000000;     //max heap size - 40MB
        if (project.isSmaliScanned()) {
            long smaliSize = 0;
            for (DecompiledFile df : project.getSmaliList()) {
                smaliSize += df.getSize();
            }
            if (max - smaliSize > 0) {
                project.setSmaliCacheEnabled(true);
                max -= smaliSize;
            }
        }
        if (project.isXmlScanned()) {
            long xmlSize = 0;
            for (DecompiledFile df : project.getXmlList()) {
                xmlSize += df.getSize();
            }
            if (max - xmlSize > 0) {
                project.setXmlCacheEnabled(true);
            }
        }
    }

    public boolean isSmaliCacheEnabled() {
        return project.isSmaliCacheEnabled();
    }

    public boolean isXmlCacheEnabled() {
        return project.isXmlCacheEnabled();
    }
}
