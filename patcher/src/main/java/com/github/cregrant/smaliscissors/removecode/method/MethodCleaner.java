package com.github.cregrant.smaliscissors.removecode.method;

import com.github.cregrant.smaliscissors.removecode.SmaliTarget;
import com.github.cregrant.smaliscissors.removecode.classparts.ClassMethod;
import com.github.cregrant.smaliscissors.removecode.method.opcodes.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;

public class MethodCleaner {

    private static final Logger logger = LoggerFactory.getLogger(MethodCleaner.class);
    private final ClassMethod method;
    private final String removeString;
    private final ArrayList<Opcode> opcodes;
    private final HashSet<SmaliTarget> fieldsCanBeNull = new HashSet<>();
    private boolean returnBroken;

    public MethodCleaner(ClassMethod method, String removeString) {
        this.method = method;
        this.removeString = removeString;
        opcodes = new MethodParser(method, removeString).parse();
    }

    public void clean() {
        ArrayDeque<Line> stack = new ArrayDeque<>();
        cleanMethodArguments(stack);
        if (method.isAbstract()) {
            return;
        }

        MethodOpcodeCleaner cleaner = new MethodOpcodeCleaner(method, opcodes, fieldsCanBeNull, stack);
        scanMethodBody(cleaner);
        cleaner.processBody();
        returnBroken = cleaner.isBroken();
        checkInfinityLoops();
        fixEmptyCatchBlocks();

        if (returnBroken) {
            fillFieldsCanBeNull();
        }
    }

    private void fillFieldsCanBeNull() {
        for (Opcode op : opcodes) {
            if (op instanceof Put) {
                fieldsCanBeNull.add(new SmaliTarget().setRef(((Put) op).getFieldReference()));
            }
        }

    }


    private void cleanMethodArguments(ArrayDeque<Line> stack) {
        ArrayList<String> inputObjects = method.getInputObjects();
        if (inputObjects == null) {
            return;
        }

        int offset = method.isStatic() ? 0 : 1;
        boolean cleaned = false;
        for (int i = 0; i < inputObjects.size(); i++) {
            String obj = inputObjects.get(i);
            if (obj.contains(removeString)) {
                cleaned = true;
                stack.add(new Line("p" + (i + offset), 0));
                inputObjects.set(i, "Z");
            }
        }
        if (cleaned) {
            method.setInputObjects(inputObjects);
        }
    }

    private void fixEmptyCatchBlocks() {   //try-catch block won't compile if empty
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
                    throw new IllegalStateException("Catch block end tag not found?");
                }

                j--;
                for (; j >= 0; j--) {
                    Opcode otherOp = opcodes.get(j);
                    if (current.getStartTag().equals(otherOp)) {
                        otherOp.deleteLine();      //delete start & end tags
                        endTag.deleteLine();
                        do {
                            opcodes.get(i).deleteLine();    //delete .catch statements
                            i++;
                        } while (opcodes.get(i) instanceof Catch);
                        i = lastPos;
                        continue loop;
                    } else if (!otherOp.isDeleted()) {
                        i = j;    //don't touch if something left inside
                        continue loop;
                    }
                }
                throw new IllegalStateException("Catch block start tag not found?");
            }
        }
    }

    private void scanMethodBody(MethodOpcodeCleaner cleaner) {
        for (int i = 0; i < opcodes.size(); i++) {
            Opcode op = opcodes.get(i);
            if (!op.isDeleted() && op.toString().contains(removeString)) {
                String register = op.getOutputRegister();
                op.deleteLine();
                if (op instanceof Put) {
                    fieldsCanBeNull.add(new SmaliTarget().setRef(((Put) op).getFieldReference()));
                }
                cleaner.removeObjectIfIncomplete(op, i);
                if (!register.isEmpty()) {
                    cleaner.getStack().add(new Line(register, i + 1));
                }
            }
        }
        //stack.sort(Collections.reverseOrder());
    }

    private void checkInfinityLoops() {        //deleting conditions can create an infinity loop
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
                returnBroken = true;
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
        return !returnBroken;
    }

    public String getNewBody() {
        return opcodesToString();
    }

    public HashSet<SmaliTarget> getFieldsCanBeNull() {
        return fieldsCanBeNull;
    }

    static class Line {
        final String register;
        final int lineNum;

        public Line(String register, int num) {
            this.register = register;
            this.lineNum = num;
        }
    }
}