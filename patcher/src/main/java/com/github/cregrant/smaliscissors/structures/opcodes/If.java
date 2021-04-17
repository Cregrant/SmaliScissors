package com.github.cregrant.smaliscissors.structures.opcodes;

public class If implements Opcode {

    @Override
    public void deleteLine(String[] lines, int i) {
        lines[i] = Opcode.commentLine(lines[i]);
        int tagPos = Opcode.searchTag(lines, i);
        lines[tagPos] = Opcode.commentLine(lines[tagPos]);
    }
}
