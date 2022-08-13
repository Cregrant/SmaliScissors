package com.github.cregrant.smaliscissors.removecode.method.opcode;

public class Cast extends Opcode {

    public Cast(String line) {
        super(line);
        scanInputRegisters();
        outputRegister = getInputRegisters().get(0);
    }

    @Override
    public void scanInputRegisters() {
        getInputRegisters().add(line.substring(15, line.indexOf(',', 17)));
    }
}
