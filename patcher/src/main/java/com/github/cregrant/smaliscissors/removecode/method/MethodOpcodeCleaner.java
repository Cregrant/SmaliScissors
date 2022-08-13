package com.github.cregrant.smaliscissors.removecode.method;

import com.github.cregrant.smaliscissors.removecode.classparts.ClassMethod;
import com.github.cregrant.smaliscissors.removecode.method.opcode.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class MethodOpcodeCleaner {

    private final ClassMethod method;
    private final ArrayDeque<MethodCleaner.Line> stack;
    private final ArrayList<Opcode> opcodes;
    private final HashSet<String> preventLoopsList = new HashSet<>(5);
    private boolean broken;
    private int i;

    public MethodOpcodeCleaner(ClassMethod method, ArrayList<Opcode> opcodes, ArrayDeque<MethodCleaner.Line> stack) {
        this.method = method;
        this.opcodes = opcodes;
        this.stack = stack;
    }

    void processBody(MethodArrayCleaner methodArrayCleaner) {
        try {
            while (!stack.isEmpty()) {
                MethodCleaner.Line line = stack.pop();
                String register = line.register;
                i = line.lineNum;

                for (; i < opcodes.size(); i++) {
                    Opcode op = opcodes.get(i);
                    if (op.isDeleted()) {
                        continue;
                    }

                    String outputRegister = op.getOutputRegister();
                    if (op instanceof Goto) {
                        Goto opcodeJump = (Goto) op;
                        int lineNum = searchTag(opcodeJump.getTag());
                        String hash = lineNum + register;
                        if (!preventLoopsList.contains(hash)) {
                            preventLoopsList.add(hash);
                            if (opcodeJump instanceof If) {    //TODO add as experimental option
//                            boolean nextBroken = checkBranchBroken(preventLoopsList, lines, new Line(register, i + 1));
//                            boolean tagBroken = checkBranchBroken(preventLoopsList, lines, new Line(register, lineNum));
//
//                            if (nextBroken) {
//                                if (tagBroken)
//                                    return true;
//                                else {
//                                    ((If) op).transformToGoto();
//                                    stack.add(new Line(register, lineNum));
//                                    break;
//                                }
//                            }
                                stack.add(new MethodCleaner.Line(register, lineNum));
                            } else {
                                i = lineNum;
                                continue;
                            }
                        }
                    }
                    methodArrayCleaner.track(op);
                    boolean isReturn = op instanceof Return;
                    boolean registerOverwrote = outputRegister.equals(register);
                    if (op.inputRegisterUsed(register)) {
                        if (isReturn) {
                            broken = true;
                            return;
                        }
                        String arrayRegister = methodArrayCleaner.delete(op);
                        if (arrayRegister != null) {
                            stack.add(new MethodCleaner.Line(arrayRegister, i));     //delete opcodes that use the array register
                        }

                        if (op instanceof Invoke) {    //delete instance if constructor has deleted
                            Invoke invoke = ((Invoke) op);
                            if (invoke.isConstructor() && !invoke.isSoftRemove()) {
                                String target = invoke.getInputRegisters().get(0);
                                if (deleteInvokeInstance(target, i)) {
                                    stack.add(new MethodCleaner.Line(target, i));
                                } else {
                                    broken = true;    //instance too far away? Let's just delete this method
                                    return;
                                }
                            }
                        }

                        op.deleteLine();
                        registerOverwrote = false;
                        if (op instanceof Invoke && ((Invoke) op).isSoftRemove()) {
                            insertOpcodes(((Invoke) op).getInsertList());
                        } else if (!outputRegister.isEmpty() && !outputRegister.equals(register)) {
                            stack.add(new MethodCleaner.Line(outputRegister, i));
                        }
                    }
                    if (isReturn || registerOverwrote) {
                        break;
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            broken = true;
        }
    }

    public boolean isBroken() {
        return broken;
    }

    void insertOpcodes(ArrayList<Opcode> ops) {     //the opcodes will be inserted before the current
        opcodes.addAll(i, ops);
        i += ops.size();
    }

    private boolean checkBranchBroken(ArrayList<String> loopsList, String[] lines, MethodCleaner.Line checkLine) {
        String[] linesLocal = Arrays.copyOf(lines, lines.length);   //FIXME broken
        ArrayList<String> preventLoopsList = new ArrayList<>(loopsList);
        ArrayList<MethodCleaner.Line> registersList = new ArrayList<>();
        registersList.add(checkLine);
        boolean returnBrokenLocal = false;
        int k = 0;

        while (k < registersList.size() && !returnBrokenLocal) {
            //FIXME HEAVY WORK HERE
        }
        return returnBrokenLocal;
    }

    public NewArray searchNewArray(String register) {
        for (int j = i; j >= 0; j--) {
            Opcode op = opcodes.get(j);
            if (op instanceof NewArray) {
                NewArray array = ((NewArray) op);
                if (array.getArrayRegister().equals(register)) {
                    return array;
                }
            } else if (op instanceof Move) {
                Move move = ((Move) op);
                if (move.getOutputRegister().equals(register)) {
                    register = move.getInputRegisters().get(0);
                }
            } else if (op instanceof Tag) {
                return new NewArray("    new-array " + register + ",");      //probably array is created by Get opcode, and we shouldn't delete it
            }
        }
        return null;
    }

    public int searchTag(Tag tag) {
        int index = opcodes.indexOf(tag);
        if (index == -1) {
            throw new IllegalArgumentException("Critical error: tag :" + tag + " not found in method " + method.getRef());
        } else {
            return index;
        }
    }

    private boolean deleteInvokeInstance(String register, int pos) {
        for (int j = pos - 1; j > 0; j--) {
            Opcode op = opcodes.get(j);
            if (op instanceof Const && op.getOutputRegister().equals(register)) {
                op.deleteLine();
                return true;
            } else if (op instanceof Move) {
                Move move = ((Move) op);
                if (move.getOutputRegister().equals(register)) {
                    register = move.getInputRegisters().get(0);
                }
            } else if (op instanceof Tag) {
                return false;      //almost never triggered
            }
        }
        return false;
    }
}
