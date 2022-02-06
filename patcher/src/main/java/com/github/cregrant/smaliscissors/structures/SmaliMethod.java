package com.github.cregrant.smaliscissors.structures;

import com.github.cregrant.smaliscissors.Gzip;
import com.github.cregrant.smaliscissors.Prefs;

import java.util.ArrayList;

public class SmaliMethod {
    private final SmaliClass parentClass;
    private final String smaliPath;
    private final String name;
    private final String modifiers;
    private String modifiersCleaned;
    private Object body;
    public boolean isStatic;
    public ArrayList<String> inputObjects;
    public ArrayList<String> inputObjectsCleaned;
    private final String returnObject;

    public SmaliMethod(SmaliClass smaliClass, String signature) {
        int inputStart = signature.indexOf('(');
        int inputEnd = signature.indexOf(')');
        int nameBegin = signature.lastIndexOf(' ') + 1;
        modifiers = signature.substring(0, nameBegin);
        modifiersCleaned = modifiers;
        isStatic = signature.contains("static");
        name = signature.substring(nameBegin, inputStart);
        smaliPath = smaliClass.getPath().replace(".smali", "") + ";->" + signature.substring(nameBegin);
        returnObject = signature.substring(inputEnd + 1);
        if (inputEnd - inputStart > 1)
            inputObjects = parseInputObjects(signature);

        parentClass = smaliClass;
    }

    static ArrayList<String> parseInputObjects(String input) {
        ArrayList<String> result = new ArrayList<>(4);
        int startIndex = input.indexOf("(") + 1;
        int start = startIndex;
        char[] chars = input.toCharArray();
        char prevChar = '(';
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
                            result.add(input.substring(start, i));
                        else
                            result.add(String.valueOf(currentChar));
                        start = i + 1;
                    }
                    break;

                case ';':
                    result.add(input.substring(start, i + 1));
                    start = i + 1;
                    break;
                case ')':
                    return result;
            }
            prevChar = currentChar;
        }
        return result;
    }

    public String getOldSignature() {
        return buildSignature(modifiers, inputObjects);
    }

    public String getNewSignature() {
        return buildSignature(modifiersCleaned, inputObjectsCleaned);
    }

    private String buildSignature(String mod, ArrayList<String> input) {
        if (input == null || input.isEmpty())
            return mod + name + "()" + returnObject;

        StringBuilder sb = new StringBuilder();
        for (String obj : input) {
            sb.append(obj);
        }
        return mod + name + '(' + sb + ')' + returnObject;
    }

    private String commentOutEnd(String string, String target, String replacement) {
        int j = string.lastIndexOf(target);

        int tgtLen = target.length();
        int thisLen = string.length();
        int newLenHint = thisLen - tgtLen + replacement.length();

        return string.substring(0, j) +
                replacement +
                string.substring(j + tgtLen, thisLen);
    }

    public String getReturnObject() {
        return returnObject;
    }

    public String getSmaliPath() {
        return smaliPath;
    }

    public String getName() {
        return name;
    }

    public String getModifiers() {
        return modifiers;
    }

    public String getBody() {
        if (Prefs.reduceMemoryUsage)
            return ((Gzip) body).decompress();
        else
            return (String) body;
    }

    public void setBody(String newBody) {
        if (Prefs.reduceMemoryUsage)
            body = new Gzip(newBody);
        else
            body = newBody;
    }

    public void delete(String[] lines, int charsLength) {
        modifiersCleaned = '#' + modifiers;
        StringBuilder sb = new StringBuilder(Math.round(1.1f * charsLength));
        for (String s : lines) {
            if (!s.isEmpty() && s.charAt(0) != '#')
                sb.append('#');
            sb.append(s).append('\n');
        }
        setBody(sb.toString());
    }

    public void setBody(String[] lines, int charsLength) {
        StringBuilder sb = new StringBuilder(Math.round(1.1f * charsLength));
        for (String s : lines) {
            sb.append(s).append('\n');
        }
        setBody(sb.toString());
    }

    public SmaliClass getParentClass() {
        return parentClass;
    }
}
