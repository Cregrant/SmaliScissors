package com.github.cregrant.smaliscissors.structures.smali;

import com.github.cregrant.smaliscissors.smali.SmaliParser;
import com.github.cregrant.smaliscissors.structures.common.SmaliFile;
import com.github.cregrant.smaliscissors.structures.interfaces.ISmaliClassPart;
import com.github.cregrant.smaliscissors.structures.smali.parts.SmaliHeader;

import java.util.ArrayList;
import java.util.List;

public class SmaliClass {
    private final SmaliFile file;
    private final String ref;
    private final ArrayList<ISmaliClassPart> parts = new ArrayList<>();
    private String deletedSuperClass;
    private String newBody;

    public SmaliClass(SmaliFile df, String body) {
        file = df;
        String temp = df.getPath();
        ref = 'L' + temp.substring(temp.indexOf('/') + 1, temp.lastIndexOf(".smali")) + ';';
        fillParts(body);
    }

    private void fillParts(String body) {
        SmaliParser parser = new SmaliParser(this, body);
        while (parser.hasNextPart()) {
            parts.add(parser.nextPart());
        }

        if (parts.isEmpty() || !(parts.get(0) instanceof SmaliHeader) || parser.getPos() < body.length()) {
            throw new IllegalArgumentException("Smali class " + file.getPath() + " broken");      //should never happen (broken structure)
        }
    }

    public List<String> clean(SmaliTarget target) {
        long t1 = System.nanoTime();
        ArrayList<String> dependencies = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        for (ISmaliClassPart part : parts) {
            String dependency = part.clean(target, this);
            builder.append(part.getText());
            if (dependency != null) {
                dependencies.add(dependency);
            }
        }
        newBody = builder.toString();
        System.out.println((System.nanoTime()-t1));     //640000 FIXME
        return dependencies;
    }

    public String getNewBody() {
        if (newBody == null) {
            throw new IllegalStateException("Use clean() method before.");
        }
        return newBody;
    }

    public String getRef() {
        return ref;
    }

    public SmaliFile getFile() {
        return file;
    }

    public String getDeletedSuperClass() {
        return deletedSuperClass;
    }

    public void setDeletedSuperClass(String string) {
        deletedSuperClass = string;
    }

    @Override
    public String toString() {
        return ref;
    }
}
