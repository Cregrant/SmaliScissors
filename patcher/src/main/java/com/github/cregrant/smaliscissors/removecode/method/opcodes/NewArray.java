package com.github.cregrant.smaliscissors.removecode.method.opcodes;

public class NewArray extends Array {

    public NewArray(String line) {
        super(line);
        arrayRegister = line.substring(line.indexOf(' ', 13) + 1, line.indexOf(','));
        outputRegister = arrayRegister;
    }

    @Override
    public boolean inputRegisterUsed(String register) {
        return false;
    }
}
