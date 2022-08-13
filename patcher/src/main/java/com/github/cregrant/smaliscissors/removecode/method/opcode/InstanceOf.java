package com.github.cregrant.smaliscissors.removecode.method.opcode;

public class InstanceOf extends Opcode {

    public InstanceOf(String line) {
        super(line);
        scanInputRegisters();
        outputRegister = line.substring(16, line.indexOf(',', 18));
    }

    @Override
    public void scanInputRegisters() {
        getInputRegisters().add(line.substring(line.indexOf(',', 18) + 2, line.indexOf(',', 22)));
    }
}
