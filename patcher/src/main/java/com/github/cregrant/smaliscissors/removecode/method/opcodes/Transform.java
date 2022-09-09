package com.github.cregrant.smaliscissors.removecode.method.opcodes;

public class Transform extends Opcode {

    public Transform(String line) {
        super(line);
        scanRegisters();
        outputRegister = inputRegisters.removeFirst();
    }
}
