package com.github.cregrant.smaliscissors.structures.opcodes;

public class Get extends Opcode {

    public Get(String[] bodyLines, int i) {
        super(bodyLines, i);
        scanInputRegisters();
        outputRegister = lines[num].substring(lines[num].indexOf(' ', 8) + 1, lines[num].indexOf(',', 10));
    }

    @Override
    public void scanInputRegisters() {
        String line = lines[num];
        if (line.charAt(4) == 'i') {
            int firstEnd = line.indexOf(',', 10);
            getInputRegisters().add(line.substring(firstEnd + 2, line.indexOf(',', firstEnd + 4)));
        }
    }
}
