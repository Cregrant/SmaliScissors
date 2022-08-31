package com.github.cregrant.smaliscissors.removecode.method.opcode;

public class Array extends Opcode {

    protected String arrayRegister;

    public Array(String line) {
        super(line);
    }

    public String getArrayRegister() {
        return arrayRegister;
    }
}
