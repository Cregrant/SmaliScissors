package com.github.cregrant.smaliscissors.removecode.method.opcode;

public class Unknown extends Opcode {

    public Unknown(String line) {
        super(line);
        this.line = line;
    }

    @Override
    public void deleteLine() {
        //super.deleteLine();
    }
}
