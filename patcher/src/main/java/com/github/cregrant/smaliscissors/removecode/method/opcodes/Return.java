package com.github.cregrant.smaliscissors.removecode.method.opcodes;

public class Return extends Opcode {
    public Return(String line) {
        super(line);
        scanRegisters();
    }
}
