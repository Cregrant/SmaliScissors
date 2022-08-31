package com.github.cregrant.smaliscissors.removecode.method.opcodes;

public class Return extends Opcode {
    public Return(String line) {
        super(line);
        outputRegister = this.line.substring(this.line.indexOf(' ', 10) + 1);
        getInputRegisters().add(outputRegister);
    }
}
