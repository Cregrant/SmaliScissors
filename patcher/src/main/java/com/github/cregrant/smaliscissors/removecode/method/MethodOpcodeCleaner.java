package com.github.cregrant.smaliscissors.removecode.method;

import com.github.cregrant.smaliscissors.removecode.SmaliTarget;
import com.github.cregrant.smaliscissors.removecode.classparts.ClassMethod;
import com.github.cregrant.smaliscissors.removecode.method.opcodes.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MethodOpcodeCleaner {

    private static final Logger logger = LoggerFactory.getLogger(MethodOpcodeCleaner.class);
    private static final String SCAN_REGISTER = "line that never matches";
    private final ClassMethod method;
    private final ArrayList<Opcode> opcodes;
    private final HashSet<SmaliTarget> fieldsCanBeNull;
    private final HashSet<String> preventLoopsList = new HashSet<>(5);
    private ArrayDeque<MethodCleaner.Line> stack;
    private boolean broken;
    private int i;

    public MethodOpcodeCleaner(ClassMethod method, ArrayList<Opcode> opcodes, HashSet<SmaliTarget> fieldsCanBeNull, ArrayDeque<MethodCleaner.Line> stack) {
        this.method = method;
        this.opcodes = opcodes;
        this.fieldsCanBeNull = fieldsCanBeNull;
        this.stack = stack;
    }

    void processBody(String removeString) {
        try {
            OpcodeArrayCleaner methodArrayCleaner = new OpcodeArrayCleaner(this);
            stack.add(new MethodCleaner.Line(SCAN_REGISTER, 0));

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
                    boolean containsTarget = SCAN_REGISTER.equals(register) && op.toString().contains(removeString);
                    if (op.inputRegisterUsed(register) || containsTarget) {
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
                            OpcodeArrayCleaner.ArrayCleanerResult arrayCleanerResult = methodArrayCleaner.delete(op);
                            stack.addAll(arrayCleanerResult.lines);
                            insertOpcodes(arrayCleanerResult.insertList);
                        }
                        if (op instanceof Put && !containsTarget) {
                            fieldsCanBeNull.add(new SmaliTarget().setRef(((Put) op).getFieldReference()));
                        }

                        op.deleteLine(method);

                        registerOverwrote = false;
                        if (!outputRegister.isEmpty() && !outputRegister.equals(register)) {
                            stack.add(new MethodCleaner.Line(outputRegister, i));
                        }
                        if (op instanceof Invoke && ((Invoke) op).isSoftRemove()) {
                            insertOpcodes(((Invoke) op).getAndClearInsertList());
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
            op.deleteLine(method);
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

    void insertOpcodes(List<Opcode> ops) {     //the opcodes will be inserted before the current
        if (ops.isEmpty()) {
            return;
        }
        opcodes.addAll(i, ops);
        ArrayDeque<MethodCleaner.Line> newStack = new ArrayDeque<>(stack.size());
        while (!stack.isEmpty()) {
            MethodCleaner.Line line = stack.poll();
            if (line.lineNum > i) {
                line.lineNum += ops.size();
            }
            newStack.push(line);
        }
        stack = newStack;
        i += ops.size();
    }

    public int searchTag(Tag tag) {
        int index = opcodes.indexOf(tag);
        if (index == -1) {
            throw new InputMismatchException("Tag :" + tag + " not found in method " + method.getRef());
        } else {
            return index;
        }
    }

    private boolean deleteInvokeInstance(String register, int pos) {
        for (int j = pos - 1; j > 0; j--) {
            Opcode op = opcodes.get(j);
            if (op instanceof NewInstance && op.getOutputRegister().equals(register)) {
                if (!op.isDeleted()) {
                    op.deleteLine(method);
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

    public ClassMethod getMethod() {
        return method;
    }
}
