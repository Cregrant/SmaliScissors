package com.github.cregrant.smaliscissors.removecode.method.opcode;

public class ArrayGet extends Array {

    public ArrayGet(String line) {
        super(line);
        scanInputRegisters();
        outputRegister = line.substring(line.indexOf(' ', 8) + 1, line.indexOf(',', 8));
    }

    @Override
    public void scanInputRegisters() {
        int start = line.indexOf(',', 8);
        arrayRegister = line.substring(start + 2, line.indexOf(',', start + 4));
        getInputRegisters().add(arrayRegister);
    }
}
