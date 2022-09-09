package com.github.cregrant.smaliscissors.removecode.method.opcodes;

public class NewArray extends Array {

    public NewArray(String line) {
        super(line);
        scanRegisters();
        arrayRegister = inputRegisters.removeFirst();
        outputRegister = arrayRegister;
    }

    protected NewArray(String line, boolean b) {
        super(line);
        scanRegisters();
    }
}
