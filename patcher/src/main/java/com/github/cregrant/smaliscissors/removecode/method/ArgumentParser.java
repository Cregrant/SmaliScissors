package com.github.cregrant.smaliscissors.removecode.method;

import java.util.ArrayList;

public class ArgumentParser {

    public ArrayList<String> parse(String input) {
        ArrayList<String> inputObjects = new ArrayList<>(5);
        char[] chars = input.toCharArray();
        char prevChar = '(';
        int start = input.indexOf("(") + 1;
        loop:
        for (int i = start; i < chars.length; i++) {
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
                        if (prevChar == '[') {
                            inputObjects.add(input.substring(start, i + 1));
                        } else {
                            inputObjects.add(String.valueOf(currentChar));
                        }
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
        return inputObjects;
    }
}
