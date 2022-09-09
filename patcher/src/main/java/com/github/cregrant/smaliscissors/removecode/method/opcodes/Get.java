package com.github.cregrant.smaliscissors.removecode.method.opcodes;

public class Get extends Opcode {

    public Get(String line) {
        super(line);
        scanRegisters();
        outputRegister = inputRegisters.removeFirst();
    }
}
