package com.github.cregrant.smaliscissors.removecode;

import com.github.cregrant.smaliscissors.removecode.classparts.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.InputMismatchException;

public class ClassParser {

    private static final Logger logger = LoggerFactory.getLogger(ClassParser.class);
    private final SmaliClass smaliClass;
    private final String body;
    private int pos;

    public ClassParser(SmaliClass smaliClass, String body) {
        this.smaliClass = smaliClass;
        this.body = body;
    }

    public ArrayList<ClassPart> parseParts() {
        ArrayList<ClassPart> parts = new ArrayList<>();
        while (hasNextPart()) {
            parts.add(getNextPart());
        }

        if (parts.isEmpty() || !(parts.get(0) instanceof ClassHeader) || pos < body.length()) {
            throw new InputMismatchException("Smali class " + smaliClass.getFile().getPath() + " broken");      //should never happen (broken structure)
        }
        return parts;
    }

    public ClassPart getNextPart() {
        int searchPos = pos;
        if (body.charAt(pos) == '#') {
            searchPos++;
        }

        if (body.startsWith(".class", searchPos)) {
            ClassHeader header = new ClassHeader(body, pos);
            pos = header.getEndPos();
            return header;
        } else if (body.startsWith(".field", searchPos)) {
            ClassField field = new ClassField(body, pos);
            pos = field.getEndPos();
            return field;
        } else if (body.startsWith(".method", searchPos)) {
            ClassMethod method = new ClassMethod(smaliClass, body, pos);
            pos = method.getEndPos();
            return method;
        } else if (body.startsWith(".annotation", searchPos)) {
            ClassAnnotation annotation = new ClassAnnotation(body, pos);
            pos = annotation.getEndPos();
            return annotation;
        } else if (body.startsWith(".implements", searchPos)) {
            ClassInterface classInterface = new ClassInterface(body, pos);
            pos = classInterface.getEndPos();
            return classInterface;
        } else if (body.startsWith("\n#", searchPos)) {
            ClassMetadata metadata = new ClassMetadata(body, pos);
            pos = metadata.getEndPos();
            return metadata;
        } else {
            throw new InputMismatchException("Error parsing " + smaliClass.getFile().getPath());
        }
    }

    public boolean hasNextPart() {
        return pos < body.length();
    }
}
