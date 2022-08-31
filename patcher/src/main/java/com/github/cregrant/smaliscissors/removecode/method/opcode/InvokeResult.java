package com.github.cregrant.smaliscissors.removecode.method.opcode;

public class InvokeResult extends Opcode {

    public InvokeResult(String line) {
        super(line);
        outputRegister = line.substring(line.lastIndexOf(' ') + 1);
    }
}
