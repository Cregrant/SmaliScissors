package com.github.cregrant.smaliscissors.removecode.method.opcodes;

public class Tag extends Opcode {

    private final String tag;
    private final boolean catchBlock;

    public Tag(String line) {
        super(line);
        int start = line.indexOf(":");
        if (line.startsWith(":try", start)) {
            tag = line.substring(start + 5);    //:try_end_0 -> end_0
            catchBlock = true;
        } else {
            tag = line.substring(start + 1);    //:goto_0 -> goto_0
            catchBlock = false;
        }
    }

    String getStringTag() {
        return tag;
    }

    public boolean isCatchBlock() {
        return catchBlock;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Tag && ((Tag) other).getStringTag().equals(tag);
    }

    @Override
    public int hashCode() {
        return tag.hashCode();
    }
}
