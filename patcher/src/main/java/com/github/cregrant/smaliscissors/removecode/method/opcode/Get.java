package com.github.cregrant.smaliscissors.removecode.method.opcode;

public class Get extends Array {

    public Get(String line) {
        super(line);
        scanInputRegisters();
        outputRegister = this.line.substring(this.line.indexOf(' ', 8) + 1, this.line.indexOf(',', 10));
    }

    @Override
    public void scanInputRegisters() {
        if (line.charAt(4) == 'i') {
            int firstEnd = line.indexOf(',', 10);
            getInputRegisters().add(line.substring(firstEnd + 2, line.indexOf(',', firstEnd + 4)));
        }
    }
}
