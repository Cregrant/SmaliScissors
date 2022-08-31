package com.github.cregrant.smaliscissors.removecode.method.opcodes;

public class Put extends Opcode {
    public Put(String line) {
        super(line);
        scanInputRegisters();
    }

    @Override
    public void scanInputRegisters() {
        int firstEnd = line.indexOf(',', 10);
        getInputRegisters().add(line.substring(line.indexOf(' ', 8) + 1, firstEnd));
        if (line.charAt(4) == 'i') {
            getInputRegisters().add(line.substring(firstEnd + 2, line.indexOf(',', firstEnd + 4)));
        }
    }
}
