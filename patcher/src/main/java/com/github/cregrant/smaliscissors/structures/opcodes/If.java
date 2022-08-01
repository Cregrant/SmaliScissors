package com.github.cregrant.smaliscissors.structures.opcodes;

import com.github.cregrant.smaliscissors.Main;
import com.github.cregrant.smaliscissors.structures.smali.parts.SmaliMethod;

public class If extends Goto {

    public If(SmaliMethod method, String[] bodyLines, int i) {
        super(method, bodyLines, i);
        absoluteJump = false;
        scanInputRegisters();
    }

    public String getTag() {
        return tag;
    }

    @Override
    public boolean inputRegisterUsed(String register) {
        return !getInputRegisters().isEmpty() && getInputRegisters().contains(register);
    }

    @Override
    public int searchTag() {
        int size = lines.length;
        for (int j=0; j<size; j++) {
            if (lines[j].startsWith(tag, 4)) {
                return j;
            }
        }
        Main.out.println("Critical error: tag " + tag + " not found in method " + method.getRef());
        System.exit(1);
        return 0;
    }

    public void transformToGoto() {
        lines[num] = "    goto " + tag;
        absoluteJump = true;
    }

    @Override
    public void deleteLine() {
        if (!deleted)
            lines[num] = "#" + lines[num];
        deleted = true;
    }
}
