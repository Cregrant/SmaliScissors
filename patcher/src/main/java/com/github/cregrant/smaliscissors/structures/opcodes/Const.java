package com.github.cregrant.smaliscissors.structures.opcodes;

public class Const extends Opcode {
    public Const(String[] bodyLines, int i) {
        super(bodyLines, i);
        outputRegister = lines[num].substring(lines[num].indexOf(' ', 6) + 1, lines[num].indexOf(','));     //todo second register for const-wide
    }

    @Override
    public boolean inputRegisterUsed(String register) {
        return false;
    }
}
