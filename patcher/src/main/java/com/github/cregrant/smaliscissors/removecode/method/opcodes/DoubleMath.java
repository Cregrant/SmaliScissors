package com.github.cregrant.smaliscissors.removecode.method.opcodes;

public class DoubleMath extends Opcode {

    public DoubleMath(String line) {
        super(line);
        scanRegisters();
        outputRegister = inputRegisters.getFirst();
    }
}
