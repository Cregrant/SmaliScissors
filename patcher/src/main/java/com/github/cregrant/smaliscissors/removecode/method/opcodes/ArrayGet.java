package com.github.cregrant.smaliscissors.removecode.method.opcodes;

public class ArrayGet extends Array {

    public ArrayGet(String line) {
        super(line);
        scanRegisters();
        arrayRegister = inputRegisters.get(1);
        outputRegister = inputRegisters.removeFirst();
    }
}
