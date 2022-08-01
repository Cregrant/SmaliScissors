package com.github.cregrant.smaliscissors.structures.smali.parts;

import com.github.cregrant.smaliscissors.Gzip;
import com.github.cregrant.smaliscissors.Prefs;
import com.github.cregrant.smaliscissors.smali.MethodCleaner;
import com.github.cregrant.smaliscissors.structures.interfaces.ISmaliClassPart;
import com.github.cregrant.smaliscissors.structures.smali.SmaliClass;
import com.github.cregrant.smaliscissors.structures.smali.SmaliTarget;

import java.util.ArrayList;

public class SmaliMethod implements ISmaliClassPart {
    private final String ref;
    private final String name;
    private final String returnObject;
    private final boolean isStatic;
    private final boolean isAbstract;
    private String modifiers;
    private Object body;
    private ArrayList<String> inputObjects = new ArrayList<>(2);
    private final int end;
    private boolean deleted;

    public SmaliMethod(SmaliClass smaliClass, String text, int pos) {
        if (text.charAt(pos) == '#')
            deleted = true;
        int signatureEnd = text.indexOf('\n', pos);
        String signature = text.substring(pos, signatureEnd);
        end = text.indexOf(".end method", pos) + 13;
        if (end == 12)
            throw new IllegalArgumentException();

        setBody(text.substring(signatureEnd, Math.min(end, text.length())));
        int inputStart = signature.indexOf('(');
        int inputEnd = signature.indexOf(')');
        int namePos = signature.lastIndexOf(' ') + 1;
        modifiers = signature.substring(0, namePos);
        isStatic = signature.contains(" static ");
        isAbstract = signature.contains(" abstract ");
        name = signature.substring(namePos, inputStart);
        ref = smaliClass.getRef().replace(".smali", "") + "->" + signature.substring(namePos);
        returnObject = signature.substring(inputEnd + 1);

        if (inputEnd - inputStart > 1)
            parseInputObjects(signature);
    }

    private void parseInputObjects(String input) {
        int startIndex = input.indexOf("(") + 1;
        int start = startIndex;
        char[] chars = input.toCharArray();
        char prevChar = '(';
        loop:
        for (int i = startIndex; i < chars.length; i++) {
            char currentChar = chars[i];
            switch (currentChar) {
                case '[':
                    if (prevChar != '[') {
                        start = i;
                    }
                    break;

                case 'I':
                case 'B':
                case 'Z':
                case 'V':
                case 'S':
                case 'C':
                case 'D':
                case 'J':
                case 'F':
                    if (prevChar != '/' && (prevChar == '[' || start == i)) {
                        if (prevChar == '[')
                            inputObjects.add(input.substring(start, i + 1));
                        else
                            inputObjects.add(String.valueOf(currentChar));
                        start = i + 1;
                    }
                    break;

                case ';':
                    inputObjects.add(input.substring(start, i + 1));
                    start = i + 1;
                    break;
                case ')':
                    break loop;
            }
            prevChar = currentChar;
        }
    }

    private String buildSignature(ArrayList<String> input) {
        StringBuilder sb = new StringBuilder();
        for (String obj : input)
            sb.append(obj);
        return modifiers + name + '(' + sb + ')' + returnObject;
    }

    public String getBody() {
        if (Prefs.allowCompression)
            return ((Gzip) body).decompress();
        else
            return (String) body;
    }

    public void setBody(String newBody) {
        if (Prefs.allowCompression)
            body = new Gzip(newBody);
        else
            body = newBody;
    }

    public String arrayToString(String[] lines) {
        StringBuilder sb = new StringBuilder();
        for (String s : lines)
            sb.append(s).append('\n');
        return sb.toString();
    }

    private boolean deleteBody(String string) {
        if (modifiers.contains(" bridge "))     //it is better to delete the class
            return false;

        StringBuilder sb = new StringBuilder(string);
        int pos = -5;
        while ((pos = sb.indexOf("\n ", pos + 5)) != -1)
            sb.insert(pos + 1, '#');

        sb.insert(sb.lastIndexOf(".end method"), '#');
        setBody(sb.toString());

        modifiers = '#' + modifiers;
        return true;
    }

    private String delete(String string) {
        if (deleteBody(string))
            return ref;                                     //delete method
        else
            return ref.substring(0, ref.indexOf("->"));     //delete class

    }

    @Override
    public String clean(SmaliTarget target, SmaliClass smaliClass) {
        String oldBody = getBody();
        if (smaliClass.getDeletedSuperClass() != null && name.equals("<init>") && !modifiers.contains(" synthetic "))    //insert call to Object constructor
            fixConstructor(smaliClass, oldBody);

        if (deleted || !target.containsInside(oldBody))
            return null;
        if (returnObject.contains(target.getRef()))
            return delete(oldBody);

        MethodCleaner cleaner = new MethodCleaner(this, target.getRef());
        String[] cleanedBody = cleaner.cleanup();
        if (cleaner.isSuccessful()) {
            setBody(arrayToString(cleanedBody));
            if (getBody().equals(oldBody))
                throw new IllegalStateException("SmaliMethod clean failed");
            return null;
        } else
            return delete(arrayToString(cleanedBody));
    }

    private void fixConstructor(SmaliClass smaliClass, String curBody) {
        int pos = curBody.indexOf(smaliClass.getDeletedSuperClass() + "-><init>(");
        int start = curBody.lastIndexOf('\n', pos) + 1;
        int end = curBody.indexOf('\n', pos);
        if (pos == -1)
            throw new IllegalArgumentException("<init> clean error");
        setBody(
                curBody.substring(0, start) +
                        '#' + curBody.substring(start, end) +
                        "\n\n    invoke-direct {p0}, Ljava/lang/Object;-><init>()V" +
                        curBody.substring(end));
        smaliClass.setDeletedSuperClass(null);
    }

    @Override
    public int getEndPos() {
        return end;
    }

    @Override
    public String getText() {
        return buildSignature(inputObjects) + getBody();
    }


    public String getReturnObject() {
        return returnObject;
    }

    public String getRef() {
        return ref;
    }

    public String getName() {
        return name;
    }

    public ArrayList<String> getInputObjects() {
        return inputObjects;
    }

    public void setInputObjects(ArrayList<String> inputObjects) {
        this.inputObjects = inputObjects;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isAbstract() {
        return isAbstract;
    }
}
