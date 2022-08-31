package com.github.cregrant.smaliscissors.removecode.method.opcodes;

public class ArrayPut extends Array {

    public ArrayPut(String line) {
        super(line);
        scanInputRegisters();
    }

    @Override
    public void scanInputRegisters() {
        int commaPos = line.indexOf(',', 10);
        String objectRegister = line.substring(line.indexOf(' ', 8) + 1, commaPos);
        getInputRegisters().add(objectRegister);

        arrayRegister = line.substring(commaPos + 2, line.indexOf(',', commaPos + 4));
        getInputRegisters().add(arrayRegister);
    }
}
