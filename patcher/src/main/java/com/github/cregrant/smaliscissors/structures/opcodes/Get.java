package com.github.cregrant.smaliscissors.structures.opcodes;

public class Get extends Opcode {

    public Get(String[] bodyLines, int i) {
        super(bodyLines, i);
        outputRegister = lines[num].substring(lines[num].indexOf(' ', 6) + 1, lines[num].indexOf(','));
    }

    @Override
    public void scanInputRegisters() {
        String line = lines[num];
        inputRegisters.add(line.substring(16, line.indexOf(',', 17)));
    }
}
