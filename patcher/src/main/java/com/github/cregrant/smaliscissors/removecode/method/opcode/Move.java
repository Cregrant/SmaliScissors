package com.github.cregrant.smaliscissors.removecode.method.opcode;

public class Move extends Opcode {

    public Move(String line) {
        super(line);
        scanInputRegisters();
        int beginIndex = this.line.indexOf(' ', 8) + 1;
        outputRegister = this.line.substring(beginIndex, this.line.indexOf(',', beginIndex + 2));
    }

    @Override
    public void scanInputRegisters() {
        getInputRegisters().add(line.substring(line.lastIndexOf(' ') + 1));
    }
}
