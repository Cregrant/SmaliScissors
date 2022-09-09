package com.github.cregrant.smaliscissors.removecode.method.opcodes;

public class NewInstance extends Opcode {

    public NewInstance(String line) {
        super(line);
        scanRegisters();
        outputRegister = inputRegisters.removeFirst();
    }
}
