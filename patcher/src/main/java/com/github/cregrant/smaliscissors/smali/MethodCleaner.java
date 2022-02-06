package com.github.cregrant.smaliscissors.smali;

import com.github.cregrant.smaliscissors.structures.SmaliMethod;
import com.github.cregrant.smaliscissors.structures.opcodes.Opcode;

import java.util.ArrayList;
import java.util.Stack;

public class MethodCleaner {
    String target;
    String returnRegister;
    boolean returnBroken = false;
    private final Stack<Line> stack = new Stack<>();

    public void cleanupMethod(SmaliMethod method, String target) {
        this.target = target;
        String body = method.getBody();
        String[] lines = body.split("\n");
        findReturnRegister(method, lines);
        scanMethodSignature(method);
        scanMethodBody(method, lines);
        cleanupBody(method, lines);
        if (returnBroken)
            method.delete(lines, body.length());
        else
            method.setBody(lines, body.length());
    }

    private void cleanupBody(SmaliMethod method, String[] lines) {
        ArrayList<String> preventLoopsList = new ArrayList<>(5);
        while (!stack.isEmpty()) {
            Line line = stack.pop();
            String register = line.register;
            int i = line.lineNum;

            for (; i < lines.length; i++) {
                Opcode op = Opcode.parseOpcode(target, method, lines, i);
                if (op == null || op.end)
                    continue;   //unknown opcode

                String outputRegister = op.outputRegister();
                if (outputRegister.contains(" "))
                    op = op;
                assert !outputRegister.contains(" ");
                boolean isReturnRegister = outputRegister.equals(returnRegister);
                if (isReturnRegister)
                    returnBroken = false;
                boolean inputRegisterUsed = op.inputRegisterUsed(register);
                if (op.canJump) {
                    int jumpLine = op.searchTag();
                    String hash = jumpLine + register;
                    if (!preventLoopsList.contains(hash)) {
                        preventLoopsList.add(hash);
                        if (op.absoluteJump) {      //goto
                            i = jumpLine;
                            continue;
                        } else {
                            stack.add(new Line(register, jumpLine));
                        }
                    }
                }

                if (inputRegisterUsed) {
                    op.deleteLine();
                    if (isReturnRegister)
                        returnBroken = true;
                    if (!outputRegister.equals(register))
                        stack.add(new Line(register, i));
                } else if (outputRegister.equals(register))
                    break;
            }
        }
    }

    private void scanMethodSignature(SmaliMethod method) {
        ArrayList<String> inputObjects = method.inputObjects;
        if (inputObjects == null)
            return;

        int size = inputObjects.size();
        ArrayList<String> inputObjectsCleaned = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            String obj = inputObjects.get(i);
            if (obj.contains(target)) {
                if (method.isStatic)
                    stack.add(new Line('p' + String.valueOf(i), 0));
                else
                    stack.add(new Line('p' + String.valueOf(i + 1), 0));   //skip p0 register
                inputObjectsCleaned.add("Z");
            } else
                inputObjectsCleaned.add(obj);
        }
        method.inputObjectsCleaned = inputObjectsCleaned;
    }

    private void scanMethodBody(SmaliMethod method, String[] lines) {
        int size = lines.length;
        for (int i = 0; i < size; i++) {
            if (!lines[i].isEmpty() && lines[i].charAt(0) != '#' && lines[i].contains(target)) {
                Opcode op = Opcode.parseOpcode(target, method, lines, i);
                if (op != null) {
                    String register = op.outputRegister();
                    op.deleteLine();
                    if (!register.isEmpty()) {
                        stack.add(new Line(register, i));
                        if (register.equals(returnRegister))
                            returnBroken = true;
                    }
                }
            }
        }
        //stack.sort(Collections.reverseOrder());
    }

    private void findReturnRegister(SmaliMethod method, String[] lines) {
        if (method.getReturnObject().equals("V"))
            returnRegister = "x";   //stub
        else {
            String tmpLine = lines[lines.length-2];
            returnRegister = tmpLine.substring(tmpLine.lastIndexOf(" ") + 1);
        }
    }

    static class Line {
        String register;
        int lineNum;

        public Line(String register, int num) {
            this.register = register;
            this.lineNum = num;
        }
    }
}

