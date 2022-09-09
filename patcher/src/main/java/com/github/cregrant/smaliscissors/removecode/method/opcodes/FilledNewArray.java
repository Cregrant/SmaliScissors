package com.github.cregrant.smaliscissors.removecode.method.opcodes;

public class FilledNewArray extends NewArray {

    private Opcode moveResultLink;

    public FilledNewArray(String line) {
        super(line, false);
    }

    public void setMoveResultLink(Opcode moveResult) {
        moveResultLink = moveResult;
        arrayRegister = moveResult.getOutputRegister();
        outputRegister = arrayRegister;
    }

    @Override
    public void deleteLine() {
         if (!deleted) {
            line = "#" + line;
            if (moveResultLink != null) {
                moveResultLink.deleteLine();
            }
            deleted = true;
        }
    }
}
