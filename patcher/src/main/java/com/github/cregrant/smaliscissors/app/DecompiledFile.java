package com.github.cregrant.smaliscissors.app;

import java.io.File;

public class DecompiledFile {
    private final String projectPath;
    private String path;
    private String body;
    private boolean isModified = false;
    private final boolean isXML;

    DecompiledFile(String currentProjectPath, boolean isXmlFile) {
        projectPath = currentProjectPath;
        isXML = isXmlFile;
    }

    public String getPath() {
        return this.path;
    }

    public String getProjectPath() {
        return this.projectPath;
    }

    public void setModified(boolean state) {
        this.isModified = state;
    }

    public boolean isNotModified() {
        return !this.isModified;
    }

    public void setPath(String newPath) {
        this.path = newPath;
    }

    public String getBody() {
        if ((!isXML && Prefs.keepSmaliFilesInRAM) || (isXML && Prefs.keepXmlFilesInRAM))
            return this.body;
        else
            return new IO().read(projectPath + File.separator + path);
    }

    public void setBody(String newBody) {
        if ((!isXML && Prefs.keepSmaliFilesInRAM) || (isXML && Prefs.keepXmlFilesInRAM))
            this.body = newBody;
        else
            new IO().write(projectPath + File.separator + path, newBody);
    }

    public boolean equals(Object anObject) {
        if (this == anObject) {
            return true;
        }
        if (anObject instanceof DecompiledFile) {
            return this.path.equals(((DecompiledFile)anObject).getPath()) && this.isModified == ((DecompiledFile)anObject).isModified && this.body.equals(((DecompiledFile)anObject).getBody());
        }
        return false;
    }
}