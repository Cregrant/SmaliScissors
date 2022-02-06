package com.github.cregrant.smaliscissors.structures.opcodes;

import com.github.cregrant.smaliscissors.structures.SmaliMethod;

import java.util.ArrayList;

public class Opcode {
    ArrayList<String> inputRegisters = new ArrayList<>(2);
    ArrayList<String> replacedRegisters;
    String outputRegister = "";
    String[] lines;
    final int num;

    public boolean canJump = false;
    public boolean absoluteJump = false;
    public boolean end = false;

    public Opcode(String[] bodyLines, int i) {
        lines = bodyLines;
        num = i;
    }

    public static Opcode parseOpcode(String target, SmaliMethod method, String[] lines, int i) {
        Opcode result = null;

        if (lines[i].startsWith("invoke", 4))
            result = new Invoke(target, lines, i);
        else if (lines[i].startsWith("const", 4) || lines[i].startsWith("new", 4))
            result = new Const(lines, i);
        else if (lines[i].startsWith("sget", 4) || lines[i].startsWith("iget", 4))
            result = new Get(lines, i);
        else if (lines[i].startsWith("sput", 4) || lines[i].startsWith("iput", 4))
            result = new Put(lines, i);
        else if (lines[i].startsWith("move", 4) && lines[i].charAt(9) != 'r' && lines[i].charAt(9) != 'e')
            result = new Move(lines, i);
        else if (lines[i].startsWith("goto", 4))
            result = new Goto(method, lines, i);
        else if (lines[i].startsWith("return", 4))
            result = new Return(lines, i);
        else if (lines[i].startsWith("if", 4))
            result = new If(method, lines, i);
        //fixme add "move"
        return result;
    }

    public String outputRegister() {
        return outputRegister;
    }

    public boolean inputRegisterUsed(String register) {
        return inputRegisters.contains(register);
    }

    public void scanInputRegisters() {
        String line = lines[num];
        int start = line.indexOf(" ", 9) + 1;
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
        lines[num] = "#" + lines[num];
    }

    public int getLine() {
        return num;
    }
}
