package com.github.cregrant.smaliscissors.structures.opcodes;

import com.github.cregrant.smaliscissors.Main;

public class Return extends Opcode {
    public Return(String[] bodyLines, int i) {
        super(bodyLines, i);
        end = true;
    }

    @Override
    public boolean inputRegisterUsed(String register) {
        if (inputRegisters.contains(register)) {
            addHelpMark();
            //todo breaks return v0
            return true;
        }
        else
            return false;
    }

    void addHelpMark() {
        if (!lines[num].endsWith("    #HELP")) {
            lines[num] = lines[num] + "    #HELP";
            Main.out.println("Problem detected. Search \"#HELP\"");
        }
    }
}
