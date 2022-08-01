package com.github.cregrant.smaliscissors.structures.opcodes;

public class NewArray extends Opcode {
    public NewArray(String[] bodyLines, int i) {
        super(bodyLines, i);
        String line = lines[i];
        arrayRegister = line.substring(line.indexOf(' ', 13) + 1, line.indexOf(','));
        outputRegister = arrayRegister;
    }

    @Override
    public boolean inputRegisterUsed(String register) {
        return false;
    }
}
