package com.github.cregrant.smaliscissors.structures.opcodes;

import com.github.cregrant.smaliscissors.Main;
import com.github.cregrant.smaliscissors.structures.SmaliMethod;

public class Goto extends Opcode {
    SmaliMethod method;

    public Goto(SmaliMethod method, String[] bodyLines, int i) {
        super(bodyLines, i);
        this.method = method;
        canJump = true;
        absoluteJump = true;
    }

    @Override
    public boolean inputRegisterUsed(String register) {
        return false;
    }

    @Override
    public int searchTag() {
        String line = lines[num];
        int start = line.lastIndexOf(':');
        String tag = line.substring(start);

        int size = lines.length;
        for (int j=0; j<size; j++) {
            if (lines[j].startsWith(tag, 4)) {
                return j;
            }
        }
        Main.out.println("Critical error: tag " + tag + " not found in method " + method.getSmaliPath());
        System.exit(1);
        return 0;
    }
}
