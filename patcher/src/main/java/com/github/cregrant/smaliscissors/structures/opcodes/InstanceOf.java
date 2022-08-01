package com.github.cregrant.smaliscissors.structures.opcodes;

public class InstanceOf extends Opcode {

    public InstanceOf(String[] bodyLines, int i) {
        super(bodyLines, i);
        scanInputRegisters();
        String line = lines[i];
        outputRegister = line.substring(20, line.indexOf(',', 22));
    }

    @Override
    public void scanInputRegisters() {
        String line = lines[num];
        getInputRegisters().add(line.substring(16, line.indexOf(',', 18)));
    }
}
