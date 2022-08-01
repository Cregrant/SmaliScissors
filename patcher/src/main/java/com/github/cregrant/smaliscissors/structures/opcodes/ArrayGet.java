package com.github.cregrant.smaliscissors.structures.opcodes;

public class ArrayGet extends Opcode {

    public ArrayGet(String[] bodyLines, int i) {
        super(bodyLines, i);
        scanInputRegisters();
        outputRegister = lines[num].substring(lines[num].indexOf(' ', 8) + 1, lines[num].indexOf(',', 8));
    }

    @Override
    public void scanInputRegisters() {
        String line = lines[num];
        int start = line.indexOf(',', 8);
        arrayRegister = line.substring(start + 2, line.indexOf(',', start + 4));
        getInputRegisters().add(arrayRegister);
    }
}
