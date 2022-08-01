package com.github.cregrant.smaliscissors.structures.opcodes;

public class ArrayPut extends Opcode {

    public ArrayPut(String[] bodyLines, int i) {
        super(bodyLines, i);
        scanInputRegisters();
    }

    @Override
    public void scanInputRegisters() {
        String line = lines[num];
        int commaPos = line.indexOf(',', 10);
        getInputRegisters().add(line.substring(line.indexOf(' ', 8) + 1, commaPos));
        arrayRegister = line.substring(commaPos + 2, line.indexOf(',', commaPos + 4));
        getInputRegisters().add(arrayRegister);
    }
}
