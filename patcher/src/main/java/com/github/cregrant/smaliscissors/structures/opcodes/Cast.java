package com.github.cregrant.smaliscissors.structures.opcodes;

public class Cast extends Opcode {

    public Cast(String[] bodyLines, int i) {
        super(bodyLines, i);
        scanInputRegisters();
        outputRegister = getInputRegisters().get(0);
    }

    @Override
    public void scanInputRegisters() {
        String line = lines[num];
        getInputRegisters().add(line.substring(15, line.indexOf(',', 17)));
    }
}
