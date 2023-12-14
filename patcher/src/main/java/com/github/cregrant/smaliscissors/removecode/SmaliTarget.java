package com.github.cregrant.smaliscissors.removecode;

import java.util.Objects;

public class SmaliTarget {

    private String smaliRef;
    private String skipPath;    //filepath starts with skipPath will be skipped
    private boolean isClass = true;

    public boolean containsInside(String string) {
        int pos = string.indexOf(smaliRef);
        while (pos >= 0) {
            char c;
            int backPos = pos;

            do {
                c = string.charAt(--backPos);
            } while (backPos > 0 && !(c == '\n' || c == '#' || c == '\"'));

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

    public SmaliTarget setRef(String smaliRef) {
        this.smaliRef = smaliRef;
        if (smaliRef.contains(";->")) {     //no file
            isClass = false;
        } else {
            skipPath = smaliRef.substring(1, smaliRef.length() - 1) + ".smali";
        }
        return this;
    }

    public String getSkipPath() {
        return skipPath;
    }

    public SmaliTarget setSkipPath(String shortPath) {
        skipPath = shortPath;
        smaliRef = "L" + shortPath.replace(".smali", ";");
        if (!smaliRef.endsWith(";") && !smaliRef.endsWith("/")) {
            smaliRef = smaliRef + ";";
        }
        return this;
    }

    public static String removePathObfuscation(String path) {     //abc.1.smali -> abc.smali
        int dotPos = path.indexOf('.');
        if (dotPos == path.length() - 6) {
            return path;
        }
        return path.substring(0, dotPos + 1) + "smali";
    }

    public boolean isClass() {
        return isClass;
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
        return Objects.hash(smaliRef, skipPath, isClass);
    }

    @Override
    public String toString() {
        return smaliRef;
    }
}
