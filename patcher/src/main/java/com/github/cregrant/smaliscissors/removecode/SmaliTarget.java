package com.github.cregrant.smaliscissors.removecode;

public class SmaliTarget {
    private String smaliRef;
    private String skipPath;    //filepath starts with skipPath will be skipped
    private boolean isMethod;
    private boolean allowDeleteFiles = true;

    public boolean containsInside(String string) {
        int pos = string.indexOf(smaliRef);
        while (pos >= 0) {
            char c;
            int backPos = pos;

            do {
                c = string.charAt(--backPos);
            }
            while (backPos > 0 && !(c == '\n' || c == '#' || c == '\"'));

            if (c == '\n') {     //line is not commented out and not inside an annotation   //todo clean annotations?
                return true;
            }
            pos = string.indexOf(smaliRef, pos + 5);
        }
        return false;
    }

    public String getRef() {
        return smaliRef;
    }

    public void setRef(String smaliRef) {
        this.smaliRef = smaliRef;
        if (smaliRef.contains(";->")) {     //no file
            isMethod = true;
        } else {
            skipPath = smaliRef.substring(1, smaliRef.length() - 1) + ".smali";
        }
    }

    public String getSkipPath() {
        return skipPath;
    }

    public void setSkipPath(String shortPath) {
        skipPath = shortPath;
        smaliRef = "L" + shortPath.replace(".smali", ";");
    }

    public boolean isMethod() {
        return isMethod;
    }

    public boolean isDeletionAllowed() {
        return allowDeleteFiles;
    }

    public void denyDeletion() {
        this.allowDeleteFiles = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SmaliTarget)) {
            return false;
        }
        return smaliRef.equals(((SmaliTarget) o).getRef());
    }

    @Override
    public int hashCode() {
        return smaliRef.hashCode();
    }

    @Override
    public String toString() {
        return smaliRef;
    }

}
