package com.github.cregrant.smaliscissors.removecode.method.opcodes;

public class If extends Goto implements Jump {

    public If(String line) {
        super(line);
        scanRegisters();
    }
}
