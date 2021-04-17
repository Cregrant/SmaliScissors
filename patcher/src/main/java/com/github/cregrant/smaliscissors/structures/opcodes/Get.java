package com.github.cregrant.smaliscissors.structures.opcodes;

public class Get implements Opcode {

    @Override
    public void deleteLine(String[] lines, int i) {
        lines[i] = Opcode.commentLine(lines[i]);
    }
}
