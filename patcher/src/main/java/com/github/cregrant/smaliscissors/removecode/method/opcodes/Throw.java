package com.github.cregrant.smaliscissors.removecode.method.opcodes;

public class Throw extends Opcode {

    private boolean replaced = false;

    public Throw(String line) {
        super(line);
        scanRegisters();
    }

    public void replaceToReturn() {
        if (!replaced) {
            line = "    return-void #" + line;
            replaced = true;
        }
    }
}
