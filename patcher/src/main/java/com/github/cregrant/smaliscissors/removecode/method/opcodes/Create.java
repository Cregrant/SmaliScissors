package com.github.cregrant.smaliscissors.removecode.method.opcodes;

public class Create extends Opcode {

    public Create(String line) {
        super(line);
        scanRegisters();
        outputRegister = inputRegisters.removeFirst();
    }
}
