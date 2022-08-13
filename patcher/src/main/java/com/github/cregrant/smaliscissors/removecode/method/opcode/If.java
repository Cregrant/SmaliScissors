package com.github.cregrant.smaliscissors.removecode.method.opcode;

public class If extends Goto {

    public If(String line) {
        super(line);
        scanInputRegisters();
    }

//    public void transformToGoto() {
//        lines[num] = "    goto " + tag;
//        absoluteJump = true;
//    }
}
