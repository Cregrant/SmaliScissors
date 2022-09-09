package com.github.cregrant.smaliscissors.removecode.method.opcodes;

public class If extends Goto {

    public If(String line) {
        super(line);
        scanRegisters();
    }

//    public void transformToGoto() {
//        lines[num] = "    goto " + tag;
//        absoluteJump = true;
//    }
}
