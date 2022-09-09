package com.github.cregrant.smaliscissors.removecode.method.opcodes;

public class MoveResult extends Opcode {

    public MoveResult(String line) {
        super(line);
        scanRegisters();
        outputRegister = inputRegisters.removeFirst();
    }
}
