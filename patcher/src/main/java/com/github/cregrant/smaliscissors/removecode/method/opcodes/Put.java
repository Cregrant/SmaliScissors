package com.github.cregrant.smaliscissors.removecode.method.opcodes;

public class Put extends Opcode {

    public Put(String line) {
        super(line);
        scanRegisters();
    }
}
