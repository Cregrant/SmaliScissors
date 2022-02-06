package com.github.cregrant.smaliscissors.structures.opcodes;

public class Put extends Opcode {
    public Put(String[] bodyLines, int i) {
        super(bodyLines, i);
        scanInputRegisters();
    }

    @Override
    public void scanInputRegisters() {
        String line = lines[num];
        inputRegisters.add(line.substring(16, line.indexOf(',', 17)));
    }
}
