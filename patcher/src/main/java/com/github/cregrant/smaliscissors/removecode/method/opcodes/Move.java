package com.github.cregrant.smaliscissors.removecode.method.opcodes;

public class Move extends Opcode {

    public Move(String line) {
        super(line);
        scanRegisters();
        outputRegister = inputRegisters.removeFirst();
    }
}
