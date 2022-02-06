package com.github.cregrant.smaliscissors.structures.opcodes;

public class Move extends Opcode {

    public Move(String[] bodyLines, int i) {
        super(bodyLines, i);
        scanInputRegisters();
        String line = lines[num];
        int beginIndex = line.indexOf(' ', 8) + 1;
        outputRegister = line.substring(beginIndex, line.indexOf(',', beginIndex + 2));
    }

    @Override
    public void scanInputRegisters() {
        String line = lines[num];
        inputRegisters.add(line.substring(line.lastIndexOf(' ') + 1));
    }
}
