package com.github.cregrant.smaliscissors.structures.smali;

public class SmaliTarget {
    private String globPath;
    private String smaliRef;
    private String skipPath;    //filepath starts with skipPath will be skipped

    public void setSkipPath(String shortPath) {
        skipPath = shortPath;
        smaliRef = "L" + shortPath.replace(".smali", ";");
        globPath = "smali{/,_classes{?,??}/}" + shortPath;
    }

    public void setRef(String smaliRef) {
        this.smaliRef = smaliRef;
        if (smaliRef.contains(";->")) {     //smali method has no file
            skipPath = "/stub/";
            globPath = skipPath;
        }
        else {           //smali class
            skipPath = smaliRef.substring(1, smaliRef.length() - 1) + ".smali";
            globPath = "smali{/,_classes{?,??}/}" + skipPath;
        }
    }

    public boolean containsInside(String string) {
        int pos = string.indexOf(smaliRef);
        while (pos >= 0) {
            char c;
            int backPos = pos;

            do
                c = string.charAt(--backPos);
            while (backPos > 0 && !(c == '\n' || c == '#' || c == '\"'));

            if (c == '\n') {     //line is not commented out and not inside an annotation
                return true;
            }
            pos = string.indexOf(smaliRef, pos + 5);
        }
        return false;
    }


    public String getRef() {
        return smaliRef;
    }

    public String getGlobPath() {
        return globPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SmaliTarget)) return false;
        SmaliTarget that = (SmaliTarget) o;
        return smaliRef.equals(that.smaliRef);
    }

    @Override
    public int hashCode() {
        return smaliRef.hashCode();
    }

    @Override
    public String toString() {
        return smaliRef;
    }

    public String getSkipPath() {
        return skipPath;
    }
}
