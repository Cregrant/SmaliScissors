package com.github.cregrant.smaliscissors.structures.opcodes;

public class Const extends Opcode {
    public Const(String[] bodyLines, int i) {
        super(bodyLines, i);
        String line = lines[i];
        outputRegister = line.substring(line.indexOf(' ', 6) + 1, line.indexOf(','));
    }

    @Override
    public boolean inputRegisterUsed(String register) {
        return false;
    }
}
