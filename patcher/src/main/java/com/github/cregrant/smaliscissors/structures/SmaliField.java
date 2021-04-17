package com.github.cregrant.smaliscissors.structures;

public class SmaliField {
    private final String path;
    private final String name;

    public SmaliField(SmaliClass smaliClass, String fieldName) {
        name = fieldName;
        path = smaliClass.getPath()+";->"+fieldName;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    };
}
