package com.github.cregrant.smaliscissors.smali;

import com.github.cregrant.smaliscissors.structures.interfaces.ISmaliClassPart;
import com.github.cregrant.smaliscissors.structures.smali.SmaliClass;
import com.github.cregrant.smaliscissors.structures.smali.parts.*;

public class SmaliParser {
    private final SmaliClass smaliClass;
    private final String body;
    private int pos;

    public SmaliParser(SmaliClass smaliClass, String body) {
        this.smaliClass = smaliClass;
        this.body = body;
    }

    public ISmaliClassPart nextPart() {
        int searchPos = pos;
        if (body.charAt(pos) == '#') {
            searchPos++;
        }

        if (body.startsWith(".class", searchPos)) {
            SmaliHeader header = new SmaliHeader(body, pos);
            pos = header.getEndPos();
            return header;
        } else if (body.startsWith(".field", searchPos)) {
            SmaliField field = new SmaliField(body, pos);
            pos = field.getEndPos();
            return field;
        } else if (body.startsWith(".method", searchPos)) {
            SmaliMethod method = new SmaliMethod(smaliClass, body, pos);
            pos = method.getEndPos();
            return method;
        } else if (body.startsWith(".annotation", searchPos)) {
            SmaliAnnotation annotation = new SmaliAnnotation(body, pos);
            pos = annotation.getEndPos();
            return annotation;
        } else if (body.startsWith(".implements", searchPos)) {
            SmaliInterface smaliInterface = new SmaliInterface(body, pos);
            pos = smaliInterface.getEndPos();
            return smaliInterface;
        } else if (body.startsWith("\n#", searchPos)) {
            SmaliMetadata metadata = new SmaliMetadata(body, pos);
            pos = metadata.getEndPos();
            return metadata;
        } else {
            throw new IllegalArgumentException("Error parsing " + smaliClass.getFile().getPath());
        }
    }

    public boolean hasNextPart() {
        return pos < body.length();
    }

    public int getPos() {
        return pos;
    }
}
