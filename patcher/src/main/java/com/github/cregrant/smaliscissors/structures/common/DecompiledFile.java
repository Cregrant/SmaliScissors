package com.github.cregrant.smaliscissors.structures.common;

import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.utils.IO;

import java.io.File;
import java.util.Objects;

public abstract class DecompiledFile {
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
        if (isCacheEnabled())
            return getRamBody();
        else
            return IO.read(project.getPath() + File.separator + path);
    }

    private String getRamBody() {
        if (body == null)
            body = IO.read(project.getPath() + File.separator + path);
        return body;
    }

    public void setBody(String newBody) {
        if (isCacheEnabled()) {
            modified = true;
            body = newBody;
        }
        else
            IO.write(project.getPath() + File.separator + path, newBody);
    }

    public void save() {
        if (!modified)
            return;
        modified = false;
        if (isCacheEnabled() && body != null)
            IO.write(project.getPath() + File.separator + path, body);
    }

    public String getPath() {
        return this.path;
    }

    public void setSize(int newSize) {
        size = newSize;
    }

    public int getSize() {
        return size;
    }

    private boolean isCacheEnabled() {
        if (this instanceof SmaliFile)
            return project.isSmaliCacheEnabled();
        else
            return project.isXmlCacheEnabled();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DecompiledFile)) return false;
        DecompiledFile that = (DecompiledFile) o;
        return project.equals(that.project) && path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(project, path);
    }

    @Override
    public String toString() {
        return path;
    }
}