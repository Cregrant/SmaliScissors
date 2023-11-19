package com.github.cregrant.smaliscissors.removecode.method;

import com.github.cregrant.smaliscissors.removecode.SmaliTarget;
import com.github.cregrant.smaliscissors.removecode.classparts.ClassMethod;
import com.github.cregrant.smaliscissors.removecode.method.opcodes.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;

public class MethodOpcodeCleaner {

    private static final Logger logger = LoggerFactory.getLogger(MethodOpcodeCleaner.class);
    private final ClassMethod method;
    private final ArrayDeque<MethodCleaner.Line> stack;
    private final ArrayList<Opcode> opcodes;
    private final HashSet<SmaliTarget> fieldsCanBeNull;
    private final HashSet<String> preventLoopsList = new HashSet<>(5);
    private boolean broken;
    private int i;

    public MethodOpcodeCleaner(ClassMethod method, ArrayList<Opcode> opcodes, HashSet<SmaliTarget> fieldsCanBeNull, ArrayDeque<MethodCleaner.Line> stack) {
        this.method = method;
        this.opcodes = opcodes;
        this.fieldsCanBeNull = fieldsCanBeNull;
        this.stack = stack;
    }

    void processBody() {
        try {
            OpcodeArrayCleaner methodArrayCleaner = new OpcodeArrayCleaner(this);

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
                    if (op instanceof Jump) {
                        processCondition(register, op);
                        if (op.getClass() == Goto.class) {      //real Goto, can't use instanceof here
                            break;
                        }
                    }
                    boolean isReturningOpcode = op instanceof Return || op instanceof Throw;
                    boolean registerOverwrote = outputRegister.equals(register);
                    if (op.inputRegisterUsed(register)) {
                        removeObjectIfIncomplete(op, i);
                        if (isReturningOpcode) {
                            if (op instanceof Throw && method.getReturnObject().equals("V")) {
                                ((Throw) op).replaceToReturn();
                                break;
                            } else {
                                broken = true;
                            }
                        }
                        if (op instanceof ArrayPut || op instanceof ArrayGet) {
                            stack.addAll(methodArrayCleaner.delete(op));
                        }
                        if (op instanceof Put) {
                            fieldsCanBeNull.add(new SmaliTarget().setRef(((Put) op).getFieldReference()));
                        }

                        op.deleteLine();

                        registerOverwrote = false;
                        if (op instanceof Invoke && ((Invoke) op).isSoftRemove()) {
                            insertOpcodes(((Invoke) op).getAndClearInsertList());
                        } else if (!outputRegister.isEmpty() && !outputRegister.equals(register)) {
                            stack.add(new MethodCleaner.Line(outputRegister, i));
                        }
                        if (broken) {
                            return;
                        }
                    }
                    if (isReturningOpcode || registerOverwrote) {
                        break;
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            broken = true;
        }
    }

    void removeObjectIfIncomplete(Opcode op, int pos) {    //delete new-instance if constructor has deleted
        if (op instanceof Invoke) {
            Invoke invoke = ((Invoke) op);
            if (invoke.isConstructor() && !invoke.isSoftRemove()) {
                String target = invoke.getInputRegisters().get(0);
                if (!deleteInvokeInstance(target, pos)) {
                    broken = true;    //instance too far away? Let's just delete this method
                }
            }
        }
    }

    public boolean isBroken() {
        return broken;
    }

    private void processCondition(String register, Opcode op) {
        if (op.inputRegisterUsed(register)) {
            op.deleteLine();
        }

        Jump jump = ((Jump) op);
        if (jump.getTag() != null) {
            addToStack(register, jump.getTag());
        } else {
            for (TableTag tableTag : jump.getTableTags()) {
                addToStack(register, tableTag.getTag());
            }
        }
    }

    private void addToStack(String register, Tag tag) {
        int lineNum = searchTag(tag);
        String hash = lineNum + register;
        if (!preventLoopsList.contains(hash)) {
            preventLoopsList.add(hash);
            stack.add(new MethodCleaner.Line(register, lineNum));
        }
    }

    void insertOpcodes(ArrayList<Opcode> ops) {     //the opcodes will be inserted before the current
        opcodes.addAll(i, ops);
        i += ops.size();
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
            if (op instanceof NewInstance && op.getOutputRegister().equals(register)) {
                if (!op.isDeleted()) {
                    op.deleteLine();
                    stack.add(new MethodCleaner.Line(register, j));
                }
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

    public ArrayDeque<MethodCleaner.Line> getStack() {
        return stack;
    }

    public ArrayList<Opcode> getOpcodes() {
        return opcodes;
    }
}
