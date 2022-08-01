package com.github.cregrant.smaliscissors.structures.opcodes;

import java.util.ArrayList;

public class Invoke extends Opcode {
    private ArrayList<String> replacedRegisters;
    private ArrayList<String> argumentsList;
    private boolean softRemove;
    private final boolean constructor;

    public Invoke(String target, String[] bodyLines, int i) {
        super(bodyLines, i);
        scanInputRegisters();
        scanTargetSignatures(target);
        constructor = lines[i].contains(";-><init>");
        if (lines[i + 2].startsWith("move-result", 4))
            outputRegister = lines[num + 2].substring(lines[num + 2].lastIndexOf(' ') + 1);
    }

    private void scanTargetSignatures(String target) {
        String line = lines[num];
        int startIndex = line.indexOf("(", 26) + 1;
        assert startIndex != 0;
        int firstMatch = line.indexOf(target);
        if (firstMatch < startIndex)
            return;

        int endIndex = line.lastIndexOf(")");       //return deleted object
        if (line.indexOf(target, endIndex) != -1)
            return;

        prepareSoftRemove(line, startIndex, target);
    }

    private void prepareSoftRemove(String line, int startIndex, String target) {
        argumentsList = new ArrayList<>(3);
        char[] chars = line.toCharArray();
        char prevChar = '(';
        if (line.charAt(14) != 't')     //stub if invoke is not static
            argumentsList.add("");

        int start = startIndex;
        boolean processingObject = false;
        outer:
        for (int i = startIndex; i < chars.length; i++) {       //todo move this parser somewhere
            char currentChar = chars[i];
            switch (currentChar) {
                case '[':
                    if (prevChar != '[')
                        start = i;
                    break;

                case 'L':
                    start = i;
                    processingObject = true;
                    break;

                case 'I':
                case 'B':
                case 'Z':
                case 'V':
                case 'S':
                case 'C':
                case 'F':
                case 'D':
                case 'J':
                    if (!processingObject) {
                        if (prevChar == '[')
                            argumentsList.add(line.substring(start, i + 1));
                        else
                            argumentsList.add(String.valueOf(currentChar));
                    }
                    break;

                case ';':
                    argumentsList.add(line.substring(start, i + 1));
                    processingObject = false;
                    break;
                case ')':
                    break outer;
            }
            prevChar = currentChar;
        }

        replacedRegisters = new ArrayList<>(3);
        for (int i = 0; i < argumentsList.size(); i++) {
            String link = argumentsList.get(i);
            if (link.contains(target)) {
                replacedRegisters.add(getInputRegisters().get(i));
                argumentsList.set(i, "Z");
            }
        }
        softRemove = true;
    }

    @Override
    public void scanInputRegisters() {
        String line = lines[num];
        int beginPosition = line.indexOf("{", 17) + 1;
        if (beginPosition == 0)
            throw new IllegalArgumentException("scanInputRegisters failed!");

        if (line.charAt(beginPosition + 4) == '.') {     //range call like {v1 .. v5}
            int dotsPosition = line.indexOf("..", beginPosition);
            int endPosition = line.indexOf('}', dotsPosition);

            String r = line.substring((beginPosition) + 1, dotsPosition - 1);
            int from = Integer.parseInt(r);
            int to = Integer.parseInt(line.substring(dotsPosition + 4, endPosition));
            String type = line.substring(beginPosition, beginPosition + 1);
            for (int i = from; i <= to; i++)
                getInputRegisters().add(type + i);
        } else {
            int end = line.indexOf("}", beginPosition) + 1;

            char[] chars = line.substring(beginPosition, end).toCharArray();
            StringBuilder sb = new StringBuilder(3);
            boolean started = false;
            for (char ch : chars) {
                if (ch == 'v' || ch == 'p') {
                    sb.append(ch);
                    started = true;
                } else if (started && ch >= 48 && ch <= 57)     //some digit
                    sb.append(ch);
                else if (started && ch == ',' || ch == '}') {
                    assert sb.length() <= 3;
                    getInputRegisters().add(sb.toString());
                    sb = new StringBuilder(3);
                }
            }
        }
    }

    @Override
    public void deleteLine() {
        if (softRemove) {
            lines[num] = generateFixedCall();
        } else if (!deleted) {
            lines[num] = "#" + lines[num];
            if (!outputRegister.isEmpty())
                lines[num + 2] = "#" + lines[num + 2];
            deleted = true;
        }
    }

    private String generateFixedCall() {
        String line = lines[num];
        StringBuilder sb = new StringBuilder();
        for (String reg : replacedRegisters)
            sb.append("    const/16 ").append(reg).append(", 0x0   #stub\n\n");

        int startIndex = line.indexOf("(", 26) + 1;
        sb.append(line, 0, startIndex);
        for (String arg : argumentsList) {
            if (!arg.isEmpty())
                sb.append(arg);
        }
        int endIndex = line.lastIndexOf(")");
        sb.append(line.substring(endIndex));
        return sb.toString();
    }

    public boolean isConstructor() {
        return constructor;
    }

    public boolean isSoftRemove() {
        return softRemove;
    }
}
