package com.creel.app;

public class Smali {
    private String path;
    private String body;
    private boolean isModified = false;

    public String getPath() {
        return this.path;
    }

    public void setModified(boolean state) {
        this.isModified = state;
    }

    public boolean isModified() {
        return this.isModified;
    }

    public void setPath(String newPath) {
        this.path = newPath;
    }

    public String getBody() {
        return this.body;
    }

    public void setBody(String newBody) {
        this.body = newBody;
    }

    public boolean equals(Object anObject) {
        if (this == anObject) {
            return true;
        }
        if (anObject instanceof Smali) {
            return this.path.equals(((Smali)anObject).getPath()) && this.isModified == ((Smali)anObject).isModified && this.body.equals(((Smali)anObject).getBody());
        }
        return false;
    }
}