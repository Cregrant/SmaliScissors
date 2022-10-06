package com.github.cregrant.smaliscissors.removecode.method.opcodes;

import java.util.List;

public class Goto extends Opcode implements Jump {

    protected final Tag tag;

    public Goto(String line) {
        super(line);
        tag = new Tag(line.substring(line.lastIndexOf(':')));
    }

    @Override
    public List<TableTag> getTableTags() {
        return null;
    }

    public Tag getTag() {
        return tag;
    }
}
