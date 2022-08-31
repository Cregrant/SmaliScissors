package com.github.cregrant.smaliscissors.removecode.method.opcode;

import com.github.cregrant.smaliscissors.removecode.method.ArgumentParser;

import java.util.ArrayList;

public class Invoke extends Opcode {
    private final boolean constructor;
    private ArrayList<String> replacedRegisters;
    private ArrayList<String> argumentsList;
    private ArrayList<Opcode> insertList = new ArrayList<>(5);
    private Opcode invokeResultLink;
    private boolean softRemove;

    public Invoke(String line, String target) {
        super(line);
        scanInputRegisters();
        scanTargetSignatures(target);
        constructor = this.line.contains(";-><init>");
    }

    private void scanTargetSignatures(String target) {
        int start = line.indexOf("(", 26) + 1;
        int end = line.lastIndexOf(")");
        int leftMatch = line.indexOf(target);
        int rightMatch = line.lastIndexOf(target);
        if (start != end && leftMatch >= start && rightMatch <= end) {    //only arguments should contain target string
            prepareSoftRemove(target);
        }
    }

    private void prepareSoftRemove(String target) {
        argumentsList = new ArgumentParser().parse(line);
        boolean isStatic = line.charAt(12) == 't';
        int offset = isStatic ? 0 : 1;
        replacedRegisters = new ArrayList<>(3);
        for (int i = 0; i < argumentsList.size(); i++) {
            String link = argumentsList.get(i);
            if (link.contains(target)) {
                replacedRegisters.add(inputRegisters.get(i + offset));
                argumentsList.set(i, "Z");
            }
        }
        softRemove = true;
    }

    @Override
    public void scanInputRegisters() {
        int beginPosition = line.indexOf("{", 17) + 1;
        if (beginPosition == 0) {
            throw new IllegalArgumentException("scanInputRegisters failed!");
        }

        if (line.charAt(beginPosition + 4) == '.') {     //range call like {v1 .. v5}
            int dotsPosition = line.indexOf("..", beginPosition);
            int endPosition = line.indexOf('}', dotsPosition);

            String r = line.substring((beginPosition) + 1, dotsPosition - 1);
            int from = Integer.parseInt(r);
            int to = Integer.parseInt(line.substring(dotsPosition + 4, endPosition));
            String type = line.substring(beginPosition, beginPosition + 1);
            for (int i = from; i <= to; i++) {
                getInputRegisters().add(type + i);
            }
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
                {
                    sb.append(ch);
                } else if (started && ch == ',' || ch == '}') {
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
            fixCall();
        } else if (!deleted) {
            line = "#" + line;
            if (invokeResultLink != null) {
                invokeResultLink.deleteLine();
            }
            deleted = true;
        }
    }

    private void fixCall() {
        if (replacedRegisters.isEmpty()) {
            return;
        }
        for (String reg : replacedRegisters) {
            insertList.add(new Const("    const/16 " + reg + ", 0x0   #stub"));
            insertList.add(new Blank());
        }
        replacedRegisters.clear();

        StringBuilder sb = new StringBuilder();         //replace all targets with a Z (smali boolean)
        int startIndex = line.indexOf("(", 26) + 1;
        sb.append(line, 0, startIndex);
        for (String arg : argumentsList) {
            if (!arg.isEmpty()) {
                sb.append(arg);
            }
        }
        int endIndex = line.lastIndexOf(")");
        sb.append(line.substring(endIndex));
        line = sb.toString();
    }

    public ArrayList<Opcode> getInsertList() {
        ArrayList<Opcode> list = insertList;
        insertList = new ArrayList<>(5);
        return list;
    }

    public boolean isConstructor() {
        return constructor;
    }

    public boolean isSoftRemove() {
        return softRemove;
    }

    public void setInvokeResultLink(Opcode invokeResultLink) {
        this.invokeResultLink = invokeResultLink;
        outputRegister = invokeResultLink.getOutputRegister();
    }
}
