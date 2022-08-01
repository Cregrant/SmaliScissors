package com.github.cregrant.smaliscissors.smali;

import com.github.cregrant.smaliscissors.structures.opcodes.ArrayPut;
import com.github.cregrant.smaliscissors.structures.opcodes.Invoke;
import com.github.cregrant.smaliscissors.structures.opcodes.Opcode;
import com.github.cregrant.smaliscissors.structures.smali.parts.SmaliMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

public class MethodCleaner {
    private final SmaliMethod method;
    private final String removeString;
    private String[] lines;
    private ArrayList<Opcode> opcodes;
    private String returnRegister;
    private boolean returnBroken = false;

    public MethodCleaner(SmaliMethod method, String removeString) {
        this.method = method;
        this.removeString = removeString;
    }

    public String[] cleanup() {
        String body = method.getBody();
        lines = body.split("\n");
        if (body.endsWith("\n\n"))
            lines[lines.length - 1] = lines[lines.length - 1] + "\n";

        if (method.getName().equals("logNetworkAction"))
            returnBroken = returnBroken;

        Stack<Line> stack = new Stack<>();
        fixMethodSignature(stack);
        if (!method.isAbstract()) {
            parseOpcodes();
            findReturnRegister();
            scanMethodBody(stack);
            returnBroken = cleanupBody(stack);
            fixEmptyCatchBlocks();
        } else if (!stack.isEmpty())
            returnBroken = true;
        return lines;
    }

    public boolean isSuccessful() {
        return !returnBroken;
    }

    private boolean cleanupBody(Stack<Line> stack) {
        ArrayList<String> preventLoopsList = new ArrayList<>(5);
        boolean returnBrokenLocal = false;
        while (!stack.isEmpty() && !returnBrokenLocal) {
            Line line = stack.pop();
            String register = line.register;
            int i = line.lineNum;

            for (; i < opcodes.size(); i++) {
                Opcode op = opcodes.get(i);
                if (op == null || op.isDeleted())
                    continue;

                String outputRegister = op.getOutputRegister();
                boolean isReturnRegister = outputRegister.equals(returnRegister);
                if (isReturnRegister)
                    returnBrokenLocal = false;
                if (op.isJump()) {
                    int jumpLine = op.searchTag();
                    String hash = jumpLine + register;
                    if (!preventLoopsList.contains(hash)) {
                        preventLoopsList.add(hash);
                        if (op.isAbsoluteJump()) {      //goto
                            i = jumpLine;
                            continue;
                        } else {    //TODO add as experimental option
//                            boolean nextBroken = checkBranchBroken(preventLoopsList, lines, new Line(register, i + 1));
//                            boolean tagBroken = checkBranchBroken(preventLoopsList, lines, new Line(register, jumpLine));
//
//                            if (nextBroken) {
//                                if (tagBroken)
//                                    return true;
//                                else {
//                                    ((If) op).transformToGoto();
//                                    stack.add(new Line(register, jumpLine));
//                                    break;
//                                }
//                            }
                            stack.add(new Line(register, jumpLine));
                        }
                    }
                }

                if (op.inputRegisterUsed(register)) {
                    if (op instanceof ArrayPut) {    //remove array refs
                        String arrayRegister = op.getArrayRegister();
                        int arrayPos = deleteArray(i, arrayRegister);
                        if (arrayPos != -1) {
                            lines[arrayPos] = '#' + lines[arrayPos];
                            stack.add(new Line(op.getArrayRegister(), arrayPos));
                        }
                    } else if (op instanceof Invoke) {    //delete instance if constructor has deleted
                        Invoke invoke = ((Invoke) op);
                        if (invoke.isConstructor() && !invoke.isSoftRemove()) {
                            try {
                                String target = invoke.getInputRegisters().get(0);
                                int targetLine = findRegisterLine(i, target);
                                if (targetLine == -1)
                                    throw new IllegalStateException("Constructor error!");
                                lines[targetLine] = '#' + lines[targetLine];
                                stack.add(new Line(target, targetLine + 1));
                            } catch (IllegalStateException e) {
                                return true;    //too complicated. Let's just delete this method
                            }
                        }
                    }

                    op.deleteLine();
                    if (isReturnRegister)
                        returnBrokenLocal = true;
                    if (!outputRegister.equals(register))
                        stack.add(new Line(register, i));
                } else if (outputRegister.equals(register))
                    break;
                if (op.isEnd())
                    break;
            }
        }
        return returnBrokenLocal;
    }

    private boolean checkBranchBroken(ArrayList<String> loopsList, String[] lines, Line checkLine) {
        String[] linesLocal = Arrays.copyOf(lines, lines.length);   //FIXME broken
        ArrayList<String> preventLoopsList = new ArrayList<>(loopsList);
        ArrayList<Line> registersList = new ArrayList<>();
        registersList.add(checkLine);
        boolean returnBrokenLocal = false;
        int k = 0;

        while (k < registersList.size() && !returnBrokenLocal) {
            Line line = registersList.get(k);
            String register = line.register;
            int i = line.lineNum;

            for (; i < opcodes.size(); i++) {
                Opcode op = opcodes.get(i);
                if (op == null)
                    continue;

                String outputRegister = op.getOutputRegister();
                boolean isReturnRegister = outputRegister.equals(returnRegister);
                if (isReturnRegister)
                    returnBrokenLocal = false;
                if (op.isJump()) {
                    int jumpLine = op.searchTag();
                    String hash = jumpLine + register;
                    if (!preventLoopsList.contains(hash)) {
                        preventLoopsList.add(hash);
                        if (op.isAbsoluteJump()) {      //goto
                            i = jumpLine;
                            continue;
                        } else {
                            registersList.add(new Line(register, jumpLine));
                        }
                    }
                }

                if (op.inputRegisterUsed(register)) {
                    if (op instanceof ArrayPut) {
                        String arrayRegister = op.getArrayRegister();
                        int arrayPos = deleteArray(i, arrayRegister);
                        if (arrayPos != -1) {
                            linesLocal[arrayPos] = '#' + linesLocal[arrayPos];
                            registersList.add(new Line(op.getArrayRegister(), arrayPos));    //remove array
                        }
                    } else if (op instanceof Invoke) {    //delete instance if constructor has deleted
                        Invoke invoke = ((Invoke) op);
                        if (invoke.isConstructor() && !invoke.isSoftRemove()) {
                            try {
                                registersList.add(new Line(register, findRegisterLine(i, invoke.getInputRegisters().get(0))));
                            } catch (IllegalStateException e) {
                                return true;    //too complicated. Let's just delete this method
                            }
                        }
                    }

                    op.deleteLine();
                    if (isReturnRegister)
                        returnBrokenLocal = true;
                    if (!outputRegister.equals(register))
                        registersList.add(new Line(register, i));
                } else if (outputRegister.equals(register))
                    break;
                if (op.isEnd())
                    break;
            }
            k++;
        }
        return returnBrokenLocal;
    }

    private int deleteArray(int from, String register) {      //search and delete last array register
        String stringArray = "new-array " + register + ',';
        String stringGet = "get-object " + register + ',';
        for (int i = from - 2; i > 0; i--) {
            String line = lines[i];
            int startPos = 4;
            if (!line.isEmpty() && line.charAt(0) == '#')
                startPos = 5;
            if (line.startsWith(stringArray, startPos) || line.startsWith(stringGet, startPos + 1)) {
                if (startPos == 5)
                    return -1;
                else
                    return i;
            }
        }
        throw new IllegalArgumentException("error!");
    }

    private int findRegisterLine(int from, String register) {
        String searchString = "new-instance " + register + ',';
        for (int i = from - 2; i > 0; i--) {
            String line = lines[i];
            if (line.startsWith(":", 4))
                break;
            int startPos = 4;
            if (!line.isEmpty() && line.charAt(0) == '#')
                startPos = 5;
            if (line.startsWith(searchString, startPos)) {
                if (startPos == 5)
                    return -1;
                else
                    return i;
            }
        }
        throw new IllegalStateException();
    }

    private void fixMethodSignature(Stack<Line> stack) {
        ArrayList<String> inputObjects = method.getInputObjects();
        if (inputObjects == null)
            return;

        int offset = method.isStatic() ? 0 : 1;
        for (int i = 0; i < inputObjects.size(); i++) {
            String obj = inputObjects.get(i);
            if (obj.contains(removeString)) {
                stack.add(new Line('p' + String.valueOf(i + offset), 0));
                inputObjects.set(i, "Z");
            }
        }
        method.setInputObjects(inputObjects);
    }

    private void fixEmptyCatchBlocks() {   //try-catch block won't compile if empty
        String endString = "";
        int startLine = 0;
        boolean tryBlockEmpty = true;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (endString.isEmpty()) {
                if (line.startsWith(":try_start_", 4)) {
                    endString = "    :try_end_" + line.substring(line.lastIndexOf('_') + 1);
                    startLine = i;
                }
            } else {
                if (line.equals(endString)) {
                    endString = "";
                    if (tryBlockEmpty) {
                        int end = i + 1;
                        while (lines[end + 1].startsWith(".catch", 4))
                            end++;
                        for (int j = startLine; j <= end; j++) {
                            if (!lines[j].startsWith("#"))
                                lines[j] = '#' + lines[j];
                        }
                    }
                } else if (tryBlockEmpty && !line.isEmpty() && line.charAt(0) != '#')
                    tryBlockEmpty = false;
            }
        }
    }

    private void parseOpcodes() {
        opcodes = new ArrayList<>(lines.length + 20);
        for (int i = 0; i < lines.length; i++) {
            Opcode op = Opcode.parseOpcode(removeString, method, lines, i);
            opcodes.add(op);
        }
    }

    private void scanMethodBody(Stack<Line> stack) {
        for (int i = 0; i < opcodes.size(); i++) {
            Opcode op = opcodes.get(i);
            if (op != null && !op.isDeleted() && op.toString().contains(removeString)) {
                String register = op.getOutputRegister();
                op.deleteLine();
                if (!register.isEmpty()) {
                    stack.add(new Line(register, i));
                    if (register.equals(returnRegister))
                        returnBroken = true;
                }
            }
        }
        //stack.sort(Collections.reverseOrder());
    }

    private void findReturnRegister() {
        if (method.getReturnObject().equals("V"))
            returnRegister = "x";   //stub
        else {
            returnRegister = opcodes.get(opcodes.size() - 2).getOutputRegister();   //TODO handle multiple return registers
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