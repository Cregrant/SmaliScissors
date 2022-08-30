package com.github.cregrant.smaliscissors.removecode.method.opcode;

public class Goto extends Opcode {

    protected final Tag tag;

    public Goto(String line) {
        super(line);
        tag = new Tag(line.substring(line.lastIndexOf(':')));
    }

    public Tag getTag() {
        return tag;
    }
}
