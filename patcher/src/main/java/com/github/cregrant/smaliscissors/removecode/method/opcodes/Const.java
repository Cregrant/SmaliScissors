package com.github.cregrant.smaliscissors.removecode.method.opcodes;

public class Const extends Opcode {
    public Const(String line) {
        super(line);
        outputRegister = this.line.substring(this.line.indexOf(' ', 6) + 1, this.line.indexOf(','));
    }

    @Override
    public boolean inputRegisterUsed(String register) {
        return false;
    }
}
