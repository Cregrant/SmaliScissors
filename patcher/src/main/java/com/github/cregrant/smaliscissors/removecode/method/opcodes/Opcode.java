package com.github.cregrant.smaliscissors.removecode.method.opcodes;

import java.util.ArrayList;

public class Opcode {
    protected final ArrayList<String> inputRegisters = new ArrayList<>(3);
    protected String outputRegister = "";
    protected boolean deleted;
    protected String line;

    protected Opcode(String line) {
        this.line = line;
    }

    public static Opcode parseOpcode(String lineString, String target) {
        Opcode result;

        int offset = lineString.startsWith("#", 4) ? 5 : 4;
        if (lineString.isEmpty()) {
            result = new Blank();
        } else if (lineString.startsWith("invoke", offset)) {
            result = new Invoke(lineString, target);
        } else if (lineString.startsWith("const", offset) || lineString.startsWith("new-instance", offset)) {
            result = new Const(lineString);
        } else if (lineString.startsWith("sget", offset) || lineString.startsWith("iget", offset)) {
            result = new Get(lineString);
        } else if (lineString.startsWith("sput", offset) || lineString.startsWith("iput", offset)) {
            result = new Put(lineString);
        } else if (lineString.startsWith("move-result", offset) && lineString.charAt(9) != 'e') {    //not move-exception
            result = new InvokeResult(lineString);
        } else if (lineString.startsWith("move", offset) && lineString.charAt(9) != 'e') {
            result = new Move(lineString);
        } else if (lineString.startsWith("goto", offset)) {
            result = new Goto(lineString);
        } else if (lineString.startsWith("if", offset)) {
            result = new If(lineString);
        } else if (lineString.startsWith(":", offset)) {
            result = new Tag(lineString);
        } else if (lineString.startsWith("return", offset)) {
            result = new Return(lineString);
        } else if (lineString.startsWith("instance-of", offset)) {
            result = new InstanceOf(lineString);
        } else if (lineString.startsWith("check-cast", offset)) {
            result = new Cast(lineString);
        } else if (lineString.startsWith("new-array", offset)) {
            result = new NewArray(lineString);
        } else if (lineString.startsWith("aput", offset)) {
            result = new ArrayPut(lineString);
        } else if (lineString.startsWith("aget", offset)) {
            result = new ArrayGet(lineString);
        } else if (lineString.startsWith(".catch", offset)) {
            result = new Catch(lineString);
        } else if (lineString.startsWith("neg-", offset) || lineString.contains("-to-")) {
            result = new Move(lineString);
        } else {
            result = new Unknown(lineString);
        }

        return result;
    }

    public boolean inputRegisterUsed(String register) {
        return !inputRegisters.isEmpty() && inputRegisters.contains(register);
    }

    public void scanInputRegisters() {
        int start = line.indexOf(" ", 8) + 1;
        int end = line.lastIndexOf(',') + 1;
        char[] chars = line.substring(start, end).toCharArray();
        StringBuilder sb = new StringBuilder(3);
        boolean started = false;
        for (char ch : chars) {
            if (ch == 'v' || ch == 'p') {
                sb.append(ch);
                started = true;
            } else if (started && ch >= 48 && ch <= 57) {
                sb.append(ch);
            } else if (started && ch == ',' || ch == '}') {
                inputRegisters.add(sb.toString());
                sb = new StringBuilder(3);
            } else if (ch == 'L') {
                break;
            }
        }
    }

    public void deleteLine() {
        if (!deleted) {
            line = "#" + line;
        }
        deleted = true;
    }

    public String getOutputRegister() {
        return outputRegister;
    }

    public ArrayList<String> getInputRegisters() {
        return inputRegisters;
    }

    @Override
    public String toString() {
        return line;
    }

    public boolean isDeleted() {
        return deleted;
    }
}
