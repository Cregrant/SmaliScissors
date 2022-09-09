package com.github.cregrant.smaliscissors.removecode.method.opcodes;

public class ArrayPut extends Array {

    public ArrayPut(String line) {
        super(line);
        scanRegisters();
        arrayRegister = inputRegisters.get(1);
    }
}
