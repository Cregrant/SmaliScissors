package com.github.cregrant.smaliscissors.structures.opcodes;

public class Invoke implements Opcode {

    @Override
    public void deleteLine(String[] lines, int i) {
        lines[i] = Opcode.commentLine(lines[i]);
        if (lines[i+2].startsWith("    move-result"))
            lines[i+2] = Opcode.commentLine(lines[i+2]);
    }
}
