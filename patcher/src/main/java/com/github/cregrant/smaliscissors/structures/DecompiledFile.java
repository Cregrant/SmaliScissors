package com.github.cregrant.smaliscissors.structures;

import com.github.cregrant.smaliscissors.IO;
import com.github.cregrant.smaliscissors.Prefs;

import java.io.File;

public class DecompiledFile {
    private final String projectPath;
    private final String path;
    private final boolean isXML;

    private String body;
    private int size;
    private boolean isModified = false;

    public DecompiledFile(String projectPath, String filePath, boolean isXmlFile) {
        this.projectPath = projectPath;
        path = filePath;
        isXML = isXmlFile;
    }

    public boolean isXML() {
        return this.isXML;
    }

    public String getPath() {
        return this.path;
    }

    public void setModified(boolean state) {
        this.isModified = state;
    }

    public boolean isModified() {
        return this.isModified;
    }

    public String getBody() {
        if (isXML ? Prefs.keepXmlFilesInRAM : Prefs.keepSmaliFilesInRAM)
            return this.body;
        else
            return IO.read(projectPath + File.separator + path);
    }

    public void setBody(String newBody) {
        if (isXML ? Prefs.keepXmlFilesInRAM : Prefs.keepSmaliFilesInRAM)
            this.body = newBody;
        else
            IO.write(projectPath + File.separator + path, newBody);
    }

    public void setSize(int newSize) {
        size = newSize;
    }

    public int getSize() {
        return size;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DecompiledFile) {
            return this.path.equals(((DecompiledFile) obj).path) && this.isModified == ((DecompiledFile) obj).isModified && this.body.equals(((DecompiledFile) obj).getBody());
        }
        return false;
    }
}