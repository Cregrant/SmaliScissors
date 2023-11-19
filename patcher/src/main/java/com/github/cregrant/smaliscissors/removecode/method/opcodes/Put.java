package com.github.cregrant.smaliscissors.removecode.method.opcodes;

public class Put extends Opcode {

    public Put(String line) {
        super(line);
        scanRegisters();
    }

    public String getFieldReference() {
        return line.substring(line.indexOf('L', 12));
    }
}
