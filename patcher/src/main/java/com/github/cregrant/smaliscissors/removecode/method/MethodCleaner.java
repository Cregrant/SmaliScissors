package com.github.cregrant.smaliscissors.removecode.method;

import com.github.cregrant.smaliscissors.Flags;
import com.github.cregrant.smaliscissors.removecode.SmaliTarget;
import com.github.cregrant.smaliscissors.removecode.classparts.ClassMethod;
import com.github.cregrant.smaliscissors.removecode.method.opcodes.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.InputMismatchException;

public class MethodCleaner {

    private static final Logger logger = LoggerFactory.getLogger(MethodCleaner.class);
    private final ClassMethod method;
    private final String removeString;
    private final ArrayList<Opcode> opcodes;
    private final HashSet<SmaliTarget> fieldsCanBeNull = new HashSet<>();
    private boolean methodBroken;

    public MethodCleaner(ClassMethod method, String removeString) {
        this.method = method;
        this.removeString = removeString;
        opcodes = new MethodParser(method, removeString).parse();
    }

    public void clean() {
        ArrayDeque<Line> stack = new ArrayDeque<>();
        methodBroken = methodBroken || cleanMethodArguments(stack);
        if (methodBroken || method.isAbstract()) {
            return;
        }

        MethodOpcodeCleaner cleaner = new MethodOpcodeCleaner(method, opcodes, fieldsCanBeNull, stack);
        cleaner.processBody(removeString);
        methodBroken = cleaner.isBroken();

        checkInfinityLoops();
        fixEmptyCatchBlocks();
    }

    public void fillFieldsCanBeNull() {
        for (Opcode op : opcodes) {
            if (op instanceof Put) {    //no need to check if opcode was deleted (whole method body deleted)
                fieldsCanBeNull.add(new SmaliTarget().setRef(((Put) op).getFieldReference()));
            }
        }

    }

    //returns true if we should delete the method
    private boolean cleanMethodArguments(ArrayDeque<Line> stack) {
        ArrayList<String> inputObjects = method.getInputObjects();
        int offset = method.isStatic() ? 0 : 1;
        boolean cleaned = false;

        for (int i = 0; i < inputObjects.size(); i++) {
            String obj = inputObjects.get(i);
            if (obj.contains(removeString)) {
                if (!Flags.SMALI_ALLOW_METHOD_ARGUMENTS_CLEANUP) {
                    return true;
                }
                cleaned = true;
                stack.add(new Line("p" + (i + offset), 0));
                inputObjects.set(i, "Z");
            }
        }
        if (cleaned) {
            method.setInputObjects(inputObjects);
            //delete method if signature collision happened after argument renaming
            if (method.getSmaliClass().containsMethodReference(method.getRef(), method.isStatic())) {
                method.delete();
                return true;
            }
        }
        return false;
    }

    private void fixEmptyCatchBlocks() {   //try-catch block won't compile if empty
        if (methodBroken) {
            return;
        }
        loop:
        for (int i = opcodes.size() - 1; i >= 0; i--) {
            Opcode op = opcodes.get(i);
            if (op instanceof Catch) {
                Catch current = (Catch) op;
                while (current.tagsEqual(opcodes.get(i - 1))) {
                    i--;
                }
                Tag endTag = current.getEndTag();
                int j = i - 1;
                int lastPos = j;
                for (; j >= 0; j--) {
                    if (endTag.equals(opcodes.get(j))) {
                        endTag = ((Tag) opcodes.get(j));
                        break;
                    }
                }
                if (j == -1) {
                    throw new InputMismatchException("Catch block end tag not found?");
                }

                j--;
                for (; j >= 0; j--) {
                    Opcode otherOp = opcodes.get(j);
                    if (current.getStartTag().equals(otherOp)) {
                        otherOp.deleteLine(method);      //delete start & end tags
                        endTag.deleteLine(method);
                        do {
                            opcodes.get(i).deleteLine(method);    //delete .catch statements
                            i++;
                        } while (opcodes.get(i) instanceof Catch);
                        i = lastPos;
                        continue loop;
                    } else if (!(otherOp.isDeleted() || otherOp instanceof Debug)) {
                        i = j;    //don't touch if something left inside
                        continue loop;
                    }
                }
                throw new InputMismatchException("Catch block start tag not found?");
            }
        }
    }

    private void checkInfinityLoops() {        //deleting conditions can create an infinity loop
        if (methodBroken) {
            return;
        }
        for (int i = 0; i < opcodes.size(); i++) {
            Opcode op = opcodes.get(i);
            if (op.isDeleted() || !(op instanceof Tag) || !op.toString().startsWith(":goto", 4)) {
                continue;
            }
            boolean broken = false;
            for (int j = i; j < opcodes.size(); j++) {
                Opcode innerOp = opcodes.get(j);
                boolean deleted = innerOp.isDeleted();
                if (!deleted && (innerOp instanceof Return || innerOp instanceof If)) {
                    break;
                }
                if (deleted || !(innerOp instanceof Goto)) {
                    continue;
                }
                if (((Goto) innerOp).getTag().equals(op)) {
                    broken = true;
                    break;
                }
            }
            if (broken) {
                methodBroken = true;
            }
        }
    }


    private String opcodesToString() {
        StringBuilder sb = new StringBuilder();
        for (Opcode op : opcodes) {
            sb.append(op.toString());
            if (!(op instanceof Blank)) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    public boolean isSuccessful() {
        return !methodBroken;
    }

    public String getNewBody() {
        return opcodesToString();
    }

    public HashSet<SmaliTarget> getFieldsCanBeNull() {
        return fieldsCanBeNull;
    }

    static class Line {
        final String register;
        int lineNum;

        public Line(String register, int num) {
            this.register = register;
            this.lineNum = num;
        }
    }
}