package com.github.cregrant.smaliscissors.structures.opcodes;

import com.github.cregrant.smaliscissors.structures.smali.parts.SmaliMethod;

import java.util.ArrayList;

public class Opcode {
    private final ArrayList<String> inputRegisters = new ArrayList<>(2);
    protected String outputRegister = "";
    protected String arrayRegister;
    protected boolean deleted;
    protected String[] lines;
    protected final int num;

    protected boolean jump;
    protected boolean absoluteJump;
    protected boolean end;

    public Opcode(String[] bodyLines, int i) {
        lines = bodyLines;
        num = i;
    }

    public static Opcode parseOpcode(String target, SmaliMethod method, String[] lines, int i) {
        String line = lines[i];
        if (line.isEmpty())
            return null;

        Opcode result = null;
        if (line.startsWith("invoke", 4))
            result = new Invoke(target, lines, i);
        else if (line.startsWith("const", 4) || line.startsWith("new-instance", 4))
            result = new Const(lines, i);
        else if (line.startsWith("sget", 4) || line.startsWith("iget", 4))
            result = new Get(lines, i);
        else if (line.startsWith("sput", 4) || line.startsWith("iput", 4))
            result = new Put(lines, i);
        else if (line.startsWith("move", 4) && line.charAt(9) != 'r' && line.charAt(9) != 'e')
            result = new Move(lines, i);
        else if (line.startsWith("goto", 4))
            result = new Goto(method, lines, i);
        else if (line.startsWith("if", 4))
            result = new If(method, lines, i);
        else if (line.startsWith("return", 4))
            result = new Return(lines, i);
        else if (line.startsWith("instance-of", 4))
            result = new InstanceOf(lines, i);
        else if (line.startsWith("check-cast", 4))
            result = new Cast(lines, i);
        else if (line.startsWith("new-array", 4))
            result = new NewArray(lines, i);
        else if (line.startsWith("aput", 4))
            result = new ArrayPut(lines, i);
        else if (line.startsWith("aget", 4))
            result = new ArrayGet(lines, i);
        return result;
    }

    public String getOutputRegister() {
        return outputRegister;
    }

    public boolean inputRegisterUsed(String register) {
        return !inputRegisters.isEmpty() && inputRegisters.contains(register);
    }

    public void scanInputRegisters() {
        String line = lines[num];
        int start = line.indexOf(" ", 8) + 1;
        int end = line.lastIndexOf(',') + 1;
        char[] chars = line.substring(start, end).toCharArray();
        StringBuilder sb = new StringBuilder(3);
        boolean started = false;
        for (char ch : chars) {
            if (ch == 'v' || ch == 'p') {
                sb.append(ch);
                started = true;
            } else if (started && ch >= 48 && ch <= 57)
                sb.append(ch);
            else if (started && ch == ',' || ch == '}') {
                assert sb.length() <= 3;
                inputRegisters.add(sb.toString());
                sb = new StringBuilder(3);
            } else if (ch == 'L')
                break;
        }
    }

    public int searchTag() {
        return 0;   //must never happen
    }

    public void deleteLine() {
        if (!deleted)
            lines[num] = "#" + lines[num];
        deleted = true;
    }

    public String getArrayRegister() {
        return arrayRegister;
    }

    public int getLine() {
        return num;
    }

    public boolean isJump() {
        return jump;
    }

    public boolean isAbsoluteJump() {
        return absoluteJump;
    }

    public boolean isEnd() {
        return end;
    }

    public ArrayList<String> getInputRegisters() {
        return inputRegisters;
    }

    @Override
    public String toString() {
        return lines[num];
    }

    public boolean isDeleted() {
        return deleted;
    }
}
