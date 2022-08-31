package com.github.cregrant.smaliscissors.removecode.method.opcodes;

public class Blank extends Opcode {

    public Blank() {
        super(null);
        deleted = true;
    }

    @Override
    public String toString() {
        return "\n";
    }
}
