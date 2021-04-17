package com.github.cregrant.smaliscissors.structures.opcodes;

public class Put implements Opcode {

    @Override
    public void deleteLine(String[] lines, int i) {
        lines[i] = Opcode.commentLine(lines[i]);
    }
}
