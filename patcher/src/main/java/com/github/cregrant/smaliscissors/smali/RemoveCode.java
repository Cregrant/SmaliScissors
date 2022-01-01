package com.github.cregrant.smaliscissors.smali;

import com.github.cregrant.smaliscissors.Main;
import com.github.cregrant.smaliscissors.structures.SmaliMethod;
import com.github.cregrant.smaliscissors.structures.opcodes.Opcode;

import java.util.ArrayList;
import java.util.Stack;

public class RemoveCode {
    private final Stack<Line> stack = new Stack<>();
    public static boolean deleteInsteadofComment = false;
    public static boolean doNotDelete = true;

    public void cleanupMethod(SmaliMethod method, String target) {
        String body = method.getBody();
        String[] lines = body.split("\\R");
        if (method.getName().contains("trackTransition"))
            Main.out.println("honk");
        scanMethodSignature(method, target);
        scanMethodBody(method, lines, target);
        deleteRegisters(method, lines);
        StringBuilder sb = new StringBuilder(body.length());
        for (String s : lines) {
            sb.append(s).append(System.lineSeparator());
        }
        String newBody = sb.toString();
        method.setBody(newBody);
    }

    private void deleteRegisters(SmaliMethod method, String[] lines) {
        long time = System.currentTimeMillis();
        //ArrayList<String> preventLoopsIf = new ArrayList<>();
        while (!stack.isEmpty()) {
            Line line = stack.pop();
            String register = line.register;
            int i = line.lineNum;
            ArrayList<String> preventLoopsGoto = new ArrayList<>();

            for (; i<lines.length; i++) {
                if (lines[i].length()==0 || lines[i].startsWith("#")) {
                    if (System.currentTimeMillis() - time > 1000)
                        Main.out.println("Too long operation: " + method.getPath() + " - " + register);
                    continue;
                }
                Opcode op = Opcode.parseOpcode(method, lines, i);
                if (op==null || op.end)
                    continue;   //unknown opcode

                String outputRegister = op.outputRegister();
                boolean inputRegisterUsed = op.inputRegisterUsed(register);
                if (op.canJump) {
                    int jumpLine = op.searchTag();
                    if (op.absoluteJump) {      //goto
                        i = jumpLine;
                        continue;
                    }

                    String hash = register + " " + jumpLine;
                    if (!preventLoopsGoto.contains(hash)) {
                        preventLoopsGoto.add(hash);
                        stack.add(new Line(register, jumpLine));
                    }
                }

                if (inputRegisterUsed) {  //move, sub, mul...
                    op.deleteLine();
                    if (outputRegister!=null && !outputRegister.equals(register))
                        stack.add(new Line(register, i));
                }
                else if (outputRegister!=null && outputRegister.equals(register))
                    break;
            }
        }
    }

    private void scanMethodSignature(SmaliMethod method, String target) {
        ArrayList<String> inputObjects = method.inputObjects;
        int size = inputObjects.size();
        ArrayList<String> inputObjectsCleaned = new ArrayList<>(size);

        for (int i=0; i<size; i++) {
            String obj = inputObjects.get(i);
            if (obj.contains(target)) {
                if (method.isStatic)
                    stack.add(new Line('p' + String.valueOf(i), 0));
                else
                    stack.add(new Line('p' + String.valueOf(i+1), 0));   //skip p0 register
            }
            else
                inputObjectsCleaned.add(obj);
        }
        method.inputObjectsCleaned = inputObjectsCleaned;
    }

    private void scanMethodBody(SmaliMethod method, String[] lines, String target) {
        int size = lines.length;
        for (int i=0; i<size; i++) {
            if (!lines[i].startsWith("#") && lines[i].contains(target)) {
                Opcode op = Opcode.parseOpcode(method, lines, i);
                if (op != null) {
                    String register = op.outputRegister();
                    op.deleteLine();
                    if (register != null)
                        stack.add(new Line(register, i));
                }
            }
        }
        //stack.sort(Collections.reverseOrder());
    }

    class Line {
        String register;
        int lineNum;

        public Line(String register, int num) {
            this.register = register;
            this.lineNum = num;
        }
    }
}

