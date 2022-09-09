package com.github.cregrant.smaliscissors.removecode.method.opcodes;

public class TripleMath extends Opcode {

    public TripleMath(String line) {
        super(line);
        scanRegisters();
        outputRegister = inputRegisters.removeFirst();
    }
}
