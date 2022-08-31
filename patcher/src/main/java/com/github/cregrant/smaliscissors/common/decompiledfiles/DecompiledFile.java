package com.github.cregrant.smaliscissors.common.decompiledfiles;

import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.util.IO;

import java.io.File;

public abstract class DecompiledFile implements Comparable<DecompiledFile> {
    protected final Project project;
    protected final String path;

    protected String body;
    protected int size;
    protected boolean modified;

    public DecompiledFile(Project project, String filePath) {
        this.project = project;
        path = filePath;
    }

    public String getBody() {
        if (isCacheEnabled()) {
            return getRamBody();
        } else {
            return IO.read(getFilesystemPath());
        }
    }

    public void setBody(String newBody) {
        if (isCacheEnabled()) {
            modified = true;
            body = newBody;
        } else {
            IO.write(getFilesystemPath(), newBody);
        }
    }

    private String getRamBody() {
        if (body == null) {
            body = IO.read(getFilesystemPath());
        }
        return body;
    }

    public void save() {
        if (!modified) {
            return;
        }
        modified = false;
        if (isCacheEnabled() && body != null) {
            IO.write(getFilesystemPath(), body);
        }
    }

    private String getFilesystemPath() {
        return project.getPath() + File.separator + path;
    }

    public String getPath() {
        return this.path;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int newSize) {
        size = newSize;
    }

    private boolean isCacheEnabled() {
        if (this instanceof SmaliFile) {
            return project.getMemoryManager().isSmaliCacheEnabled();
        } else {
            return project.getMemoryManager().isXmlCacheEnabled();
        }
    }

    @Override
    public int compareTo(DecompiledFile other) {
        return Integer.compare(size, other.size);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DecompiledFile)) {
            return false;
        }
        DecompiledFile that = (DecompiledFile) o;
        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public String toString() {
        return path;
    }
}