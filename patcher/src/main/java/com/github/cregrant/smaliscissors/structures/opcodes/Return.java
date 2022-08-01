package com.github.cregrant.smaliscissors.structures.opcodes;

public class Return extends Opcode {
    public Return(String[] lines, int i) {
        super(lines, i);
        end = true;
        String line = lines[i];
        outputRegister = line.substring(line.indexOf(' ', 10) + 1);
        getInputRegisters().add(outputRegister);
    }
}
