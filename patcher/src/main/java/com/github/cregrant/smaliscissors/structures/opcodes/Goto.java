package com.github.cregrant.smaliscissors.structures.opcodes;

import com.github.cregrant.smaliscissors.Main;
import com.github.cregrant.smaliscissors.structures.smali.parts.SmaliMethod;

public class Goto extends Opcode {
    protected final SmaliMethod method;
    protected final String tag;

    public Goto(SmaliMethod method, String[] bodyLines, int i) {
        super(bodyLines, i);
        this.method = method;
        jump = true;
        absoluteJump = true;
        tag = getTag();
    }

    @Override
    public boolean inputRegisterUsed(String register) {
        return false;
    }

    private String getTag() {
        String line = lines[num];
        int start = line.lastIndexOf(':');
        return line.substring(start);
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
}
