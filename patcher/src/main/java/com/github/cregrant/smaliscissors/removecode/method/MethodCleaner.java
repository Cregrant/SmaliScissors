package com.github.cregrant.smaliscissors.removecode.method;

import com.github.cregrant.smaliscissors.removecode.classparts.ClassMethod;
import com.github.cregrant.smaliscissors.removecode.method.opcode.*;

import java.util.ArrayDeque;
import java.util.ArrayList;

public class MethodCleaner {
    private final ClassMethod method;
    private final String removeString;
    private ArrayList<Opcode> opcodes;
    private boolean returnBroken;

    public MethodCleaner(ClassMethod method, String removeString) {
        this.method = method;
        this.removeString = removeString;
    }

    public void clean() {
        ArrayDeque<Line> stack = new ArrayDeque<>();
        cleanMethodArguments(stack);
        if (method.isAbstract()) {
            return;
        }
        opcodes = new MethodParser(method, removeString).parse();
        scanMethodBody(stack);
        MethodOpcodeCleaner cleaner = new MethodOpcodeCleaner(method, opcodes, stack);
        cleaner.processBody(new MethodArrayCleaner(cleaner));
        returnBroken = cleaner.isBroken();
        fixEmptyCatchBlocks();
    }

    private void cleanMethodArguments(ArrayDeque<Line> stack) {
        ArrayList<String> inputObjects = method.getInputObjects();
        if (inputObjects == null) {
            return;
        }

        int offset = method.isStatic() ? 0 : 1;
        for (int i = 0; i < inputObjects.size(); i++) {
            String obj = inputObjects.get(i);
            if (obj.contains(removeString)) {
                stack.add(new Line("p" + (i + offset), 0));
                inputObjects.set(i, "Z");
            }
        }
        method.setInputObjects(inputObjects);
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

    private void scanMethodBody(ArrayDeque<Line> stack) {
        for (int i = 0; i < opcodes.size(); i++) {
            Opcode op = opcodes.get(i);
            if (!op.isDeleted() && op.toString().contains(removeString)) {
                String register = op.getOutputRegister();
                op.deleteLine();
                if (!register.isEmpty()) {
                    stack.add(new Line(register, i + 1));
                }
            }
        }
        //stack.sort(Collections.reverseOrder());
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

    static class Line {
        String register;
        int lineNum;

        public Line(String register, int num) {
            this.register = register;
            this.lineNum = num;
        }
    }
}