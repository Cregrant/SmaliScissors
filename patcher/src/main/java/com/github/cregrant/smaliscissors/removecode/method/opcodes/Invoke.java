package com.github.cregrant.smaliscissors.removecode.method.opcodes;

import com.github.cregrant.smaliscissors.removecode.method.ArgumentParser;

import java.util.ArrayList;

public class Invoke extends Opcode {
    private final boolean constructor;
    private ArrayList<String> replacedRegisters;
    private ArrayList<String> argumentsList;
    private ArrayList<Opcode> insertList = new ArrayList<>(5);
    private Opcode moveResultLink;
    private boolean softRemove;

    public Invoke(String line, String target) {
        super(line);
        scanRegisters();
        scanTargetSignatures(target);
        constructor = line.contains("invoke-direct");
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
        boolean isStatic = line.charAt(line.charAt(0) == '#' ? 13 : 12) == 't';
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
    public void deleteLine() {
        if (softRemove) {
            fixCall();
        } else if (!deleted) {
            line = "#" + line;
            if (moveResultLink != null) {
                moveResultLink.deleteLine();
            }
            deleted = true;
        }
    }

    private void fixCall() {
        if (replacedRegisters.isEmpty()) {
            return;
        }
        for (String reg : replacedRegisters) {
            insertList.add(new Create("    const/16 " + reg + ", 0x0   #stub"));
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

    public ArrayList<Opcode> getAndClearInsertList() {
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

    public void setMoveResultLink(Opcode moveResultLink) {
        this.moveResultLink = moveResultLink;
        outputRegister = moveResultLink.getOutputRegister();
    }
}
