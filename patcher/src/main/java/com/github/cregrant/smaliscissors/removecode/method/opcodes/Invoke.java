package com.github.cregrant.smaliscissors.removecode.method.opcodes;

import com.github.cregrant.smaliscissors.Flags;
import com.github.cregrant.smaliscissors.removecode.classparts.ClassMethod;
import com.github.cregrant.smaliscissors.removecode.method.ArgumentParser;

import java.util.ArrayList;

public class Invoke extends Opcode {
    private final boolean constructor;
    private ArrayList<String> replacedRegisters;
    private ArrayList<String> argumentsList;
    private ArrayList<Opcode> insertList = new ArrayList<>();
    private Opcode moveResultLink;
    private boolean softRemove;

    public Invoke(String line, String target) {
        super(line);
        scanRegisters();
        scanTargetSignatures(target);
        constructor = line.contains("invoke-direct");
    }

    private void scanTargetSignatures(String target) {
        //finding target within brackets
        int start = line.indexOf("(", 26) + 1;
        int end = line.lastIndexOf(")");
        int leftMatch = line.indexOf(target);
        int rightMatch = line.lastIndexOf(target);
        if (Flags.SMALI_ALLOW_METHOD_ARGUMENTS_CLEANUP && start != end && leftMatch >= start && rightMatch <= end) {
            prepareSoftRemove(target);
        }
    }

    private void prepareSoftRemove(String target) {
        boolean isStatic = line.charAt(line.charAt(0) == '#' ? 13 : 12) == 't';
        int offset = isStatic ? 0 : 1;
        argumentsList = new ArgumentParser().parse(line);
        replacedRegisters = new ArrayList<>(3);
        ArrayList<String> newArgumentsList = new ArrayList<>(argumentsList);
        for (int i = 0; i < newArgumentsList.size(); i++) {
            String link = newArgumentsList.get(i);
            if (link.contains(target)) {
                String register = inputRegisters.get(i + offset);
                replacedRegisters.add(register);
                newArgumentsList.set(i, "Z");
            }
        }
        argumentsList = newArgumentsList;
        softRemove = true;
    }

    @Override
    public void deleteLine(ClassMethod method) {
        if (softRemove) {
            fixCall(method);
        } else if (!deleted) {
            line = "#" + line;
            if (moveResultLink != null) {
                moveResultLink.deleteLine(method);
            }
            deleted = true;
        }
    }

    private void fixCall(ClassMethod method) {
        if (replacedRegisters.isEmpty()) {
            return;
        }
        for (String reg : replacedRegisters) {
            if (!method.isStatic() && "p0".equals(reg)) {
                throw new IllegalArgumentException();   //fixme can't replace "p0", we can search for any other unused register
            }
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
        insertList = new ArrayList<>();
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
