package com.github.cregrant.smaliscissors.removecode.method.opcodes;

public class Tag extends Opcode {

    private final String tag;

    public Tag(String line) {
        super(line);
        int start = line.indexOf(":");
        if (line.startsWith(":try", start)) {
            tag = line.substring(start + 5);    //:try_end_0 -> end_0
        } else {
            tag = line.substring(start + 1);    //:goto_0 -> goto_0
        }
    }

    String getStringTag() {
        return tag;
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
