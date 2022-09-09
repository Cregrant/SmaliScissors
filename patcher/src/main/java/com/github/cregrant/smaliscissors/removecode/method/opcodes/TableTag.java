package com.github.cregrant.smaliscissors.removecode.method.opcodes;

public class TableTag extends Opcode {

    private final Tag tag;

    protected TableTag(String line) {   //invisible in search and equals to a real tag
        super(line);
        tag = new Tag(line);
    }

    public Tag getTag() {
        return tag;
    }
}
