package com.github.cregrant.smaliscissors.removecode.method.opcodes;

public class Cast extends Opcode {

    public Cast(String line) {
        super(line);
        scanRegisters();
        outputRegister = inputRegisters.getFirst();
    }
}
